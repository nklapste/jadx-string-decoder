package jadx.plugins.stringdecoder;

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

	/** Returns e.g. "b64: hello", "b64(url): hello", "b64(mime): hello" */
	String commentText() {
		return tag.isEmpty() ? "b64: " + decoded : "b64(" + tag + "): " + decoded;
	}

	/** Returns e.g. "b64[0]: hello", "b64(url)[0]: hello" */
	String indexedCommentText(int index) {
		return tag.isEmpty() ? "b64[" + index + "]: " + decoded : "b64(" + tag + ")[" + index + "]: " + decoded;
	}
}
