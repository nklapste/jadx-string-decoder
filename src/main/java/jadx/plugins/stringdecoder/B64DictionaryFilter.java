package jadx.plugins.stringdecoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class B64DictionaryFilter {

	// Split on underscore, hyphen, plus, or camelCase boundaries
	private static final Pattern SPLIT_PATTERN = Pattern.compile(
			"[_+\\-]|(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");

	private static final Set<String> WORDS = loadWords();

	private B64DictionaryFilter() {
	}

	private static Set<String> loadWords() {
		Set<String> words = new HashSet<>();
		try (InputStream is = B64DictionaryFilter.class.getResourceAsStream(
				"/jadx/plugins/stringdecoder/words.txt")) {
			if (is == null) {
				return words;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String w = line.trim();
					if (!w.isEmpty()) {
						words.add(w);
					}
				}
			}
		} catch (IOException ignored) {
		}
		return words;
	}

	/**
	 * Returns true if {@code str} splits entirely into dictionary words, suggesting it is an
	 * identifier rather than an intentionally encoded string (e.g. "getContext", "fill_item").
	 * Segments shorter than 2 chars are ignored so single-letter separators don't block the check.
	 */
	static boolean isAllDictionaryWords(String str) {
		if (WORDS.isEmpty()) {
			return false;
		}
		List<String> segments = Arrays.stream(SPLIT_PATTERN.split(str))
				.map(String::toLowerCase)
				.filter(s -> s.length() >= 2)
				.collect(Collectors.toList());
		if (segments.isEmpty()) {
			return false;
		}
		return segments.stream().allMatch(WORDS::contains);
	}
}
