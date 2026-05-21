package jadx.plugins.stringdecoder;

import java.util.List;

import jadx.api.data.CommentStyle;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.codegen.utils.CodeComment;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class B64DeobfuscatePass implements JadxDecompilePass {

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
		for (BlockNode block : blocks) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn instanceof ConstStringNode) {
					processConstString((ConstStringNode) insn);
				}
			}
		}
	}

	private static void processConstString(ConstStringNode insn) {
		String decoded = B64Detector.detect(insn.getString());
		if (decoded != null) {
			insn.addAttr(AType.CODE_COMMENTS, new CodeComment("b64: " + decoded, CommentStyle.LINE));
		}
	}
}
