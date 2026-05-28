package jadx.plugins.stringdecoder;

import java.util.Locale;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.nodes.InsnNode;

/**
 * Recognises calls to {@code Base64.decode}-like methods: any invoke whose declaring class name
 * contains "base64" and whose method name contains "decode" (case-insensitive). A direct string
 * argument to such a call is strong evidence of intent, so callers decode it without heuristics.
 */
final class B64DecodeCalls {

	private B64DecodeCalls() {
	}

	/** True if {@code insn} is an invoke of a Base64.decode-like method. */
	static boolean isDecodeCall(InsnNode insn) {
		if (!(insn instanceof InvokeNode)) {
			return false;
		}
		MethodInfo mth = ((InvokeNode) insn).getCallMth();
		return mth.getDeclClass().getFullName().toLowerCase(Locale.ROOT).contains("base64")
				&& mth.getName().toLowerCase(Locale.ROOT).contains("decode");
	}
}
