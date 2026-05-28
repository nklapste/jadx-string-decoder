package jadx.plugins.stringdecoder;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.core.dex.nodes.FieldNode;

/**
 * Reads compile-time constant values baked into a field by the class file — i.e. a {@code static
 * final} field's {@code CONSTANT_VALUE} attribute, as opposed to a value assigned in {@code <clinit>}
 * or a constructor (which {@code ExtractFieldInit} surfaces as a {@code FIELD_INIT_INSN}).
 */
final class FieldConstants {

	private FieldConstants() {
	}

	/** The field's {@code CONSTANT_VALUE} string literal, or null if it has none / isn't a string. */
	static String readStringValue(FieldNode field) {
		EncodedValue cv = field.get(JadxAttrType.CONSTANT_VALUE);
		if (cv == null || cv.getType() != EncodedType.ENCODED_STRING) {
			return null;
		}
		Object val = cv.getValue();
		return val instanceof String ? (String) val : null;
	}
}
