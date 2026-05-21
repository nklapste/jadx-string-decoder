package jadx.plugins.stringdecoder;

import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.FieldInitInsnAttr;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class B64FieldInitPass implements JadxDecompilePass {

	private final int maxCommentLength;

	public B64FieldInitPass(int maxCommentLength) {
		this.maxCommentLength = maxCommentLength;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"B64FieldInitDeobfuscate",
				"Detect and decode likely Base64-encoded static field initializers")
				.after("ExtractFieldInit");
	}

	@Override
	public void init(RootNode root) {
	}

	@Override
	public boolean visit(ClassNode cls) {
		for (FieldNode field : cls.getFields()) {
			FieldInitInsnAttr initAttr = field.get(AType.FIELD_INIT_INSN);
			if (initAttr == null) {
				continue;
			}
			InsnNode initInsn = initAttr.getInsn();
			if (!(initInsn instanceof ConstStringNode)) {
				continue;
			}
			String decoded = B64Detector.detect(((ConstStringNode) initInsn).getString(), maxCommentLength);
			if (decoded != null) {
				field.addCodeComment("b64: " + decoded);
			}
		}
		return false;
	}

	@Override
	public void visit(MethodNode mth) {
	}
}
