package jadx.plugins.stringdecoder;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
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

	private final B64DeobfuscateOptions options;

	public B64FieldInitPass(B64DeobfuscateOptions options) {
		this.options = options;
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
			processField(field);
		}
		return false;
	}

	private void processField(FieldNode field) {
		// Case 1: field initialised via <clinit> / constructor and extracted by ExtractFieldInit
		FieldInitInsnAttr initAttr = field.get(AType.FIELD_INIT_INSN);
		if (initAttr != null) {
			InsnNode initInsn = initAttr.getInsn();
			if (initInsn instanceof ConstStringNode) {
				annotateField(field, ((ConstStringNode) initInsn).getString());
			}
			return;
		}

		// Case 2: static final field with a literal value encoded as CONSTANT_VALUE (no <clinit>)
		EncodedValue constVal = field.get(JadxAttrType.CONSTANT_VALUE);
		if (constVal != null && constVal.getType() == EncodedType.ENCODED_STRING) {
			Object val = constVal.getValue();
			if (val instanceof String) {
				annotateField(field, (String) val);
			}
		}
	}

	private void annotateField(FieldNode field, String str) {
		String decoded = B64Detector.detect(str, options);
		if (decoded != null) {
			field.addCodeComment("b64: " + decoded);
		}
	}

	@Override
	public void visit(MethodNode mth) {
	}
}
