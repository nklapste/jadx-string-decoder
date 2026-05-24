package jadx.plugins.stringdecoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
		// Lazily built on the first B64 hit — avoids field scan for methods with no B64 strings
		Set<String> fieldConstants = null;
		// Decoded strings grouped by their top-level statement instruction (insertion = source order)
		Map<InsnNode, List<B64Result>> stmtCandidates = null;
		// Arrays where ≥1 string passed normal detect() — required anchor for contextual decoding
		Set<InsnNode> arrayAnchors = null;
		// All valid-B64+UTF-8 strings in arrays (superset of anchors; used for the final comment)
		Map<InsnNode, TreeMap<Integer, B64Result>> arrayCandidates = null;

		for (BlockNode block : blocks) {
			for (InsnNode insn : block.getInstructions()) {
				if (!(insn instanceof ConstStringNode)) {
					continue;
				}
				ConstStringNode csn = (ConstStringNode) insn;
				String str = csn.getString();

				// If the string is an arg to an explicit Base64.decode call, decode unconditionally.
				// The call itself is strong evidence of intent — skip false-positive heuristics,
				// but still respect the explicit blocklist.
				boolean forced = isUsedAsBase64DecodeArg(csn);
				if (forced && B64FalsePositives.contains(str)) {
					forced = false;
				}
				B64Result decoded = forced
						? B64Detector.decodeForced(str, options.getMaxCommentLength())
						: B64Detector.detect(str, options);

				// If this string feeds a FilledNewArrayNode, use contextual detection:
				// a string that fails heuristics (e.g. skipIdentifiers) is still included as a
				// candidate if at least one sibling in the array passes normal detection.
				FilledNewArrayNode arrayParent = findFilledNewArrayParent(csn);
				if (arrayParent != null) {
					int idx = argIndexOf(arrayParent, csn.getResult().getSVar());
					if (idx < 0) {
						continue;
					}
					// Determine the best available decoded value for this slot
					B64Result candidate = decoded != null ? decoded
							: B64Detector.decodeIfValid(str, options.getMaxCommentLength());
					if (candidate == null) {
						continue;
					}
					if (fieldConstants == null) {
						fieldConstants = collectConstantValueFieldStrings(mth.getParentClass());
					}
					if (fieldConstants.contains(str)) {
						continue;
					}
					if (arrayCandidates == null) {
						arrayCandidates = new LinkedHashMap<>();
					}
					arrayCandidates.computeIfAbsent(arrayParent, k -> new TreeMap<>()).put(idx, candidate);
					if (decoded != null) {
						if (arrayAnchors == null) {
							arrayAnchors = new LinkedHashSet<>();
						}
						arrayAnchors.add(arrayParent);
					}
					continue;
				}

				if (decoded == null) {
					continue;
				}
				if (fieldConstants == null) {
					fieldConstants = collectConstantValueFieldStrings(mth.getParentClass());
				}
				// Skip strings that are static final CONSTANT_VALUE fields — B64FieldInitPass handles those
				if (fieldConstants.contains(str)) {
					continue;
				}
				if (stmtCandidates == null) {
					stmtCandidates = new LinkedHashMap<>();
				}
				stmtCandidates.computeIfAbsent(findStatementInsn(csn), k -> new ArrayList<>()).add(decoded);
			}
		}

		if (stmtCandidates != null) {
			for (Map.Entry<InsnNode, List<B64Result>> e : stmtCandidates.entrySet()) {
				List<B64Result> results = e.getValue();
				String comment = results.size() == 1
						? results.get(0).commentText()
						: buildChainComment(results);
				e.getKey().addAttr(AType.CODE_COMMENTS, new CodeComment(comment, CommentStyle.LINE));
			}
		}

		// Only emit array comments for arrays with at least one confirmed detection (the anchor).
		// This prevents annotating arrays that happen to contain valid-looking Base64 by coincidence.
		if (arrayAnchors != null) {
			for (InsnNode arrayInsn : arrayAnchors) {
				String comment = buildArrayComment(arrayCandidates.get(arrayInsn));
				arrayInsn.addAttr(AType.CODE_COMMENTS, new CodeComment(comment, CommentStyle.LINE));
			}
		}
	}

	/**
	 * Walks the SSA def-use chain to find the top-level statement instruction.
	 * Pivots through {@code <init>} constructors to avoid landing on the result-less init node,
	 * which would be lost when ConstructorVisitor later merges new-instance + init.
	 */
	private static InsnNode findStatementInsn(ConstStringNode csn) {
		RegisterArg result = csn.getResult();
		if (result == null) {
			return csn;
		}
		SSAVar var = result.getSVar();
		if (var == null) {
			return csn;
		}
		InsnNode current = csn;
		for (int depth = 0; depth < 16; depth++) {
			List<RegisterArg> uses = var.getUseList();
			if (uses.size() != 1) {
				break;
			}
			InsnNode useInsn = uses.get(0).getParentInsn();
			if (useInsn == null) {
				break;
			}
			RegisterArg useResult = useInsn.getResult();
			if (useResult == null) {
				// No result register: terminal statement (void call, constructor, sput, return, etc.).
				// For constructors, pivot through to the post-constructor user so the comment lands
				// on the enclosing statement rather than the result-less <init> node.
				if (useInsn instanceof InvokeNode && ((InvokeNode) useInsn).getCallMth().isConstructor()) {
					InsnArg arg0 = useInsn.getArg(0);
					if (arg0 instanceof RegisterArg) {
						SSAVar ctorVar = ((RegisterArg) arg0).getSVar();
						if (ctorVar != null) {
							InsnNode postCtorUser = singlePostCtorUser(ctorVar, useInsn);
							if (postCtorUser != null) {
								RegisterArg postResult = postCtorUser.getResult();
								if (postResult == null) {
									return postCtorUser;
								}
								SSAVar postVar = postResult.getSVar();
								if (postVar == null || postVar.getUseList().isEmpty()) {
									return postCtorUser;
								}
								current = postCtorUser;
								var = postVar;
								continue;
							}
						}
					}
				}
				return useInsn;
			}
			SSAVar useVar = useResult.getSVar();
			if (useVar == null || useVar.getUseList().isEmpty()) {
				return useInsn;
			}
			// Don't walk into SYNTHETIC instructions (control-flow merge points that JADX generates
			// but doesn't render directly). When useInsn's result is only consumed by a SYNTHETIC
			// instruction, the current instruction is the last one that produces visible code.
			if (useVar.getUseList().size() == 1) {
				InsnNode soleConsumer = useVar.getUseList().get(0).getParentInsn();
				if (soleConsumer != null && soleConsumer.contains(AFlag.SYNTHETIC)) {
					break;
				}
			}
			current = useInsn;
			var = useVar;
		}
		return current;
	}

	/**
	 * Returns the single use of {@code ctorVar} that is not {@code ctor} itself, or null if
	 * there are zero or more than one such uses.
	 */
	private static InsnNode singlePostCtorUser(SSAVar ctorVar, InsnNode ctor) {
		List<RegisterArg> uses = ctorVar.getUseList();
		InsnNode found = null;
		for (RegisterArg use : uses) {
			InsnNode parent = use.getParentInsn();
			if (parent != null && parent != ctor) {
				if (found != null) {
					return null;
				}
				found = parent;
			}
		}
		return found;
	}

	/** Returns the FilledNewArrayNode that directly uses the result of {@code csn}, or null. */
	private static FilledNewArrayNode findFilledNewArrayParent(ConstStringNode csn) {
		RegisterArg result = csn.getResult();
		if (result == null) {
			return null;
		}
		SSAVar ssaVar = result.getSVar();
		if (ssaVar == null) {
			return null;
		}
		for (RegisterArg use : ssaVar.getUseList()) {
			InsnNode parent = use.getParentInsn();
			if (parent instanceof FilledNewArrayNode) {
				return (FilledNewArrayNode) parent;
			}
		}
		return null;
	}

	/** Returns the index of the arg in {@code insn} whose SSAVar matches {@code ssaVar}, or -1. */
	private static int argIndexOf(InsnNode insn, SSAVar ssaVar) {
		for (int i = 0; i < insn.getArgsCount(); i++) {
			InsnArg arg = insn.getArg(i);
			if (arg instanceof RegisterArg && ((RegisterArg) arg).getSVar() == ssaVar) {
				return i;
			}
		}
		return -1;
	}

	private static String buildChainComment(List<B64Result> results) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < results.size(); i++) {
			sb.append('\n');
			sb.append(results.get(i).indexedCommentText(i));
		}
		return sb.toString();
	}

	private static String buildArrayComment(TreeMap<Integer, B64Result> decodings) {
		return B64Result.buildIndexedComment(decodings);
	}

	/**
	 * Returns true if any use of {@code csn}'s result register is a direct argument
	 * to a Base64.decode-like method call.
	 */
	private static boolean isUsedAsBase64DecodeArg(ConstStringNode csn) {
		RegisterArg result = csn.getResult();
		if (result == null) {
			return false;
		}
		SSAVar ssaVar = result.getSVar();
		if (ssaVar == null) {
			return false;
		}
		for (RegisterArg use : ssaVar.getUseList()) {
			InsnNode parent = use.getParentInsn();
			if (B64FieldInitPass.isBase64DecodeCall(parent)) {
				return true;
			}
		}
		return false;
	}

	/** Collects literal string values of all CONSTANT_VALUE fields in the class. */
	private static Set<String> collectConstantValueFieldStrings(ClassNode cls) {
		Set<String> result = null;
		for (FieldNode field : cls.getFields()) {
			EncodedValue constVal = field.get(JadxAttrType.CONSTANT_VALUE);
			if (constVal != null && constVal.getType() == EncodedType.ENCODED_STRING) {
				Object val = constVal.getValue();
				if (val instanceof String) {
					if (result == null) {
						result = new HashSet<>();
					}
					result.add((String) val);
				}
			}
		}
		return result != null ? result : Collections.emptySet();
	}
}
