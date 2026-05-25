package jadx.plugins.stringdecoder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for B64DictionaryFilter.isAllDictionaryWords — the split logic and dictionary
 * lookup are non-trivial and untested by the integration suite (the integration test for
 * skipDictionaryWords uses a string that fails UTF-8 decode before this filter is reached).
 */
class B64DictionaryFilterTest {

	@Test
	public void singleDictionaryWordTest() {
		// "callback" is a single segment that is in words.txt
		assertThat(B64DictionaryFilter.isAllDictionaryWords("callback")).isTrue();
	}

	@Test
	public void camelCaseSplitTest() {
		// "getContext" splits on the lowercase→uppercase boundary into ["get", "context"]
		assertThat(B64DictionaryFilter.isAllDictionaryWords("getContext")).isTrue();
	}

	@Test
	public void underscoreSplitTest() {
		// Underscores are split delimiters: "get_item" → ["get", "item"]
		assertThat(B64DictionaryFilter.isAllDictionaryWords("get_item")).isTrue();
	}

	@Test
	public void acronymBoundaryTest() {
		// The pattern (?<=[A-Z])(?=[A-Z][a-z]) splits between consecutive uppercase letters
		// when the second starts a new word: "APICallback" → ["api", "callback"]
		assertThat(B64DictionaryFilter.isAllDictionaryWords("APICallback")).isTrue();
	}

	@Test
	public void nonDictionaryWordTest() {
		// "aGVs" is not a dictionary word
		assertThat(B64DictionaryFilter.isAllDictionaryWords("aGVs")).isFalse();
	}

	@Test
	public void partialDictionaryWordTest() {
		// "callbackXYZ" splits to ["callback", "xyz"]; "xyz" is not in the dictionary
		assertThat(B64DictionaryFilter.isAllDictionaryWords("callbackXYZ")).isFalse();
	}

	@Test
	public void shortSegmentIgnoredTest() {
		// "x+y" splits on '+' into ["x", "y"]; both are < 2 chars and filtered out,
		// leaving no valid segments → treated as not all-dictionary-words
		assertThat(B64DictionaryFilter.isAllDictionaryWords("x+y")).isFalse();
	}

	@Test
	public void emptyStringTest() {
		assertThat(B64DictionaryFilter.isAllDictionaryWords("")).isFalse();
	}
}
