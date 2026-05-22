package jadx.plugins.stringdecoder;

import java.util.Set;

/**
 * Known Base64-charset strings that decode to something plausible but are not intentional
 * Base64 encoding — common identifiers, framework tokens, etc. that slip past the
 * statistical filters. Add entries here when a repeating false positive is confirmed.
 */
final class B64FalsePositives {

	private static final Set<String> ENTRIES = Set.of(
			"SystemJobScheduler" // Android class name; decodes to mostly-printable garbage that passes ratio checks
	);

	static boolean contains(String str) {
		return ENTRIES.contains(str);
	}

	private B64FalsePositives() {
	}
}
