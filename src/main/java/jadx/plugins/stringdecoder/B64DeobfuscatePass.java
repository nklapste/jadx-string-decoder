package jadx.plugins.stringdecoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jadx.api.data.CommentStyle;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.codegen.utils.CodeComment;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class B64DeobfuscatePass implements JadxDecompilePass {

	// Safety bound for SSA def-use chain walks; real chains are short, this just prevents pathological loops.
	private static final int MAX_STMT_WALK_DEPTH = 16;

	private final B64DeobfuscateOptions options;

	public B64DeobfuscatePass(B64DeobfuscateOptions options) {
		this.options = options;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"B64Deobfuscate",
				"Detect and decode likely Base64-encoded string constants")
				.after("SSATransform")
				.after("MarkFinallyVisitor")
				.before("ConstInlineVisitor");
	}

	@Override
	public void init(RootNode root) {
	}

	@Override
	public boolean visit(ClassNode cls) {
		return true;
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		List<BlockNode> blocks = mth.getBasicBlocks();
		if (blocks == null) {
			return;
		}
		MethodCandidates candidates = collectCandidates(mth, blocks);
		emitCandidates(candidates);
	}

	// ---- candidate collection ----

	/** Mutable per-method aggregation of every comment that needs to be emitted. */
	private static final class MethodCandidates {
		// Decoded strings grouped by their top-level statement (insertion = source order).
		final Map<InsnNode, List<B64Result>> statementCandidates = new LinkedHashMap<>();
		// Per filled-new-array: indexed candidates + anchor flag.
		final Map<InsnNode, ArrayCandidateCollector> arrayCandidates = new LinkedHashMap<>();
	}

	private MethodCandidates collectCandidates(MethodNode mth, List<BlockNode> blocks) {
		MethodCandidates candidates = new MethodCandidates();
		Set<String> fieldConstants = collectConstantValueFieldStrings(mth.getParentClass());
		for (BlockNode block : blocks) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn instanceof ConstStringNode) {
					classifyConstString((ConstStringNode) insn, candidates, fieldConstants);
				}
			}
		}
		return candidates;
	}

	/** Route a single ConstStringNode to either array-element handling or statement-level handling. */
	private void classifyConstString(ConstStringNode csn, MethodCandidates candidates,
			Set<String> fieldConstants) {
		String str = csn.getString();
		B64Result decoded = detectOrForce(csn, str);

		FilledNewArrayNode arrayParent = findFilledNewArrayParent(csn);
		if (arrayParent != null) {
			recordArrayElement(csn, arrayParent, str, decoded, candidates, fieldConstants);
			return;
		}
		if (decoded == null || fieldConstants.contains(str)) {
			return;
		}
		InsnNode statement = findStatementInsn(csn);
		candidates.statementCandidates.computeIfAbsent(statement, k -> new ArrayList<>()).add(decoded);
	}

	/** Pick the right detection mode: forced when the string feeds a Base64.decode call, otherwise heuristic. */
	private B64Result detectOrForce(ConstStringNode csn, String str) {
		boolean forced = !B64FalsePositives.contains(str) && isUsedAsBase64DecodeArg(csn);
		return forced
				? B64Detector.decodeForced(str, options.getMaxCommentLength())
				: B64Detector.detect(str, options);
	}

	private void recordArrayElement(ConstStringNode csn, FilledNewArrayNode arrayParent, String str,
			B64Result decoded, MethodCandidates candidates, Set<String> fieldConstants) {
		int idx = argIndexOf(arrayParent, csn.getResult().getSVar());
		if (idx < 0 || fieldConstants.contains(str)) {
			return;
		}
		B64Result fallback = decoded != null ? null
				: B64Detector.decodeIfValid(str, options.getMaxCommentLength());
		candidates.arrayCandidates
				.computeIfAbsent(arrayParent, k -> new ArrayCandidateCollector())
				.add(idx, decoded, fallback);
	}

	// ---- emission ----

	private static void emitCandidates(MethodCandidates candidates) {
		candidates.statementCandidates.forEach((stmt, results) -> {
			String comment = results.size() == 1 ? results.get(0).commentText() : buildChainComment(results);
			stmt.addAttr(AType.CODE_COMMENTS, new CodeComment(comment, CommentStyle.LINE));
		});
		candidates.arrayCandidates.forEach((arrayInsn, collector) -> {
			if (collector.hasAnchor()) {
				arrayInsn.addAttr(AType.CODE_COMMENTS,
						new CodeComment(collector.buildComment(), CommentStyle.LINE));
			}
		});
	}

	private static String buildChainComment(List<B64Result> results) {
		return IntStream.range(0, results.size())
				.mapToObj(i -> "\n" + results.get(i).indexedCommentText(i))
				.collect(Collectors.joining());
	}

	// ---- SSA / def-use plumbing ----

	/**
	 * Walks the SSA def-use chain forward to find the top-level statement instruction that should
	 * carry the inline comment. The walk pivots through {@code <init>} constructors so the comment
	 * lands on the enclosing statement instead of the result-less init node (which is later merged
	 * with new-instance by ConstructorVisitor).
	 */
	private static InsnNode findStatementInsn(ConstStringNode csn) {
		SSAVar var = ssaVarOf(csn);
		if (var == null) {
			return csn;
		}
		InsnNode current = csn;
		for (int depth = 0; depth < MAX_STMT_WALK_DEPTH; depth++) {
			InsnNode user = soleUserOf(var);
			if (user == null) {
				return current;
			}
			RegisterArg userResult = user.getResult();
			if (userResult == null) {
				// Result-less consumer (void call, ctor, sput, return). Constructors get pivoted
				// past so the comment lands on the post-ctor user rather than the result-less init.
				InsnNode pivoted = pivotPastConstructor(user);
				if (pivoted == user) {
					return user;
				}
				SSAVar pivotVar = ssaVarOf(pivoted);
				if (pivotVar == null || pivotVar.getUseList().isEmpty()) {
					return pivoted;
				}
				current = pivoted;
				var = pivotVar;
				continue;
			}
			SSAVar userVar = userResult.getSVar();
			if (userVar == null || userVar.getUseList().isEmpty()) {
				return user;
			}
			if (nextHopIsSynthetic(userVar)) {
				// SYNTHETIC consumers are control-flow merges JADX doesn't render — stop at the last
				// instruction that produces visible code.
				return current;
			}
			current = user;
			var = userVar;
		}
		return current;
	}

	/** Returns the parent insn of a single user of {@code var}, or null if there isn't exactly one. */
	private static InsnNode soleUserOf(SSAVar var) {
		List<RegisterArg> uses = var.getUseList();
		if (uses.size() != 1) {
			return null;
		}
		return uses.get(0).getParentInsn();
	}

	/** True iff {@code userVar} has exactly one consumer and that consumer is flagged SYNTHETIC. */
	private static boolean nextHopIsSynthetic(SSAVar userVar) {
		if (userVar.getUseList().size() != 1) {
			return false;
		}
		InsnNode sole = userVar.getUseList().get(0).getParentInsn();
		return sole != null && sole.contains(AFlag.SYNTHETIC);
	}

	/** Returns the single post-ctor user of a constructor's receiver, or {@code useInsn} unchanged. */
	private static InsnNode pivotPastConstructor(InsnNode useInsn) {
		if (!(useInsn instanceof InvokeNode) || !((InvokeNode) useInsn).getCallMth().isConstructor()) {
			return useInsn;
		}
		InsnArg arg0 = useInsn.getArg(0);
		if (!(arg0 instanceof RegisterArg) || ((RegisterArg) arg0).getSVar() == null) {
			return useInsn;
		}
		SSAVar recvVar = ((RegisterArg) arg0).getSVar();
		InsnNode found = null;
		for (RegisterArg use : recvVar.getUseList()) {
			InsnNode parent = use.getParentInsn();
			if (parent == null || parent == useInsn) {
				continue;
			}
			if (found != null) {
				return useInsn; // multiple post-ctor uses → don't pivot
			}
			found = parent;
		}
		return found != null ? found : useInsn;
	}

	private static SSAVar ssaVarOf(InsnNode insn) {
		RegisterArg r = insn.getResult();
		return r != null ? r.getSVar() : null;
	}

	private static FilledNewArrayNode findFilledNewArrayParent(ConstStringNode csn) {
		SSAVar v = ssaVarOf(csn);
		if (v == null) {
			return null;
		}
		return v.getUseList().stream()
				.map(RegisterArg::getParentInsn)
				.filter(FilledNewArrayNode.class::isInstance)
				.map(FilledNewArrayNode.class::cast)
				.findFirst().orElse(null);
	}

	private static int argIndexOf(InsnNode insn, SSAVar ssaVar) {
		for (int i = 0; i < insn.getArgsCount(); i++) {
			InsnArg arg = insn.getArg(i);
			if (arg instanceof RegisterArg && ((RegisterArg) arg).getSVar() == ssaVar) {
				return i;
			}
		}
		return -1;
	}

	/** True if any use of {@code csn}'s result feeds a Base64.decode-like call as a direct arg. */
	private static boolean isUsedAsBase64DecodeArg(ConstStringNode csn) {
		SSAVar v = ssaVarOf(csn);
		if (v == null) {
			return false;
		}
		return v.getUseList().stream().anyMatch(use -> B64DecodeCalls.isDecodeCall(use.getParentInsn()));
	}

	private static Set<String> collectConstantValueFieldStrings(ClassNode cls) {
		Set<String> result = new HashSet<>();
		for (FieldNode field : cls.getFields()) {
			EncodedValue cv = field.get(JadxAttrType.CONSTANT_VALUE);
			if (cv == null || cv.getType() != EncodedType.ENCODED_STRING) {
				continue;
			}
			Object val = cv.getValue();
			if (val instanceof String) {
				result.add((String) val);
			}
		}
		return result.isEmpty() ? Collections.emptySet() : result;
	}

	/**
	 * Collects per-index Base64 candidates for a single string-array literal and tracks whether at
	 * least one element passed full heuristic detection (the "anchor"). An array gets a comment only
	 * when at least one element clearly looks like intentional Base64; once that bar is met, every
	 * sibling element that merely decodes to valid UTF-8 is also included with its index.
	 */
	private static final class ArrayCandidateCollector {
		private final TreeMap<Integer, B64Result> candidates = new TreeMap<>();
		private boolean hasAnchor;

		void add(int index, B64Result anchor, B64Result fallback) {
			B64Result chosen = anchor != null ? anchor : fallback;
			if (chosen == null) {
				return;
			}
			candidates.put(index, chosen);
			if (anchor != null) {
				hasAnchor = true;
			}
		}

		boolean hasAnchor() {
			return hasAnchor;
		}

		String buildComment() {
			return B64Result.buildIndexedComment(candidates);
		}
	}
}
