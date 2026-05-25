package jadx.plugins.stringdecoder;

import java.util.TreeMap;
import java.util.stream.Collectors;

final class B64Result {
	private final String decoded;
	private final String tag; // "" = standard, "url", "mime"

	B64Result(String decoded, String tag) {
		this.decoded = decoded;
		this.tag = tag;
	}

	String getDecoded() {
		return decoded;
	}

	/** "b64: hello" or "b64(url): hello" */
	String commentText() {
		return prefix() + ": " + decoded;
	}

	/** "b64[0]: hello" or "b64(url)[0]: hello" */
	String indexedCommentText(int index) {
		return prefix() + "[" + index + "]: " + decoded;
	}

	private String prefix() {
		return tag.isEmpty() ? "b64" : "b64(" + tag + ")";
	}

	/** Multi-line indexed comment from a sorted map of array-index → result. */
	static String buildIndexedComment(TreeMap<Integer, B64Result> entries) {
		return entries.entrySet().stream()
				.map(e -> "\n" + e.getValue().indexedCommentText(e.getKey()))
				.collect(Collectors.joining());
	}
}
