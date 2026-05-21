package jadx.plugins.stringdecoder;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.api.data.CommentStyle;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.codegen.utils.CodeComment;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class B64DeobfuscatePass implements JadxDecompilePass {

	private final int maxCommentLength;

	public B64DeobfuscatePass(int maxCommentLength) {
		this.maxCommentLength = maxCommentLength;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"B64Deobfuscate",
				"Detect and decode likely Base64-encoded string constants")
				.after("SSATransform")
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
		for (BlockNode block : blocks) {
			for (InsnNode insn : block.getInstructions()) {
				if (!(insn instanceof ConstStringNode)) {
					continue;
				}
				ConstStringNode csn = (ConstStringNode) insn;
				String decoded = B64Detector.detect(csn.getString(), maxCommentLength);
				if (decoded == null) {
					continue;
				}
				if (fieldConstants == null) {
					fieldConstants = collectConstantValueFieldStrings(mth.getParentClass());
				}
				// Skip strings that are static final CONSTANT_VALUE fields — B64FieldInitPass handles those
				if (!fieldConstants.contains(csn.getString())) {
					csn.addAttr(AType.CODE_COMMENTS, new CodeComment("b64: " + decoded, CommentStyle.LINE));
				}
			}
		}
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
