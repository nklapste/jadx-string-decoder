## jadx-string-decoder

A JADX plugin that detects likely encoded string constants during decompilation and annotates them with decoded comments inline.

### Features

**Base64 string detection** — scans string constants in method bodies, and field initializers. When a string looks to be Base64 encoded, a `// b64: <decoded>` comment is added next to it.

**Byte array string detection** — scans `byte[]` and `int[]` fields initialised with literal arrays. The values are treated as bytes and decoded as UTF-8; if the result has enough printable characters, a `// bytes: "<string>"` comment is added.

### Installation

In jadx-gui: **Plugins → Install plugin** and enter the location id:

```
github:nklapste:jadx-string-decoder
```

In jadx-cli:

```bash
jadx plugins --install "github:nklapste:jadx-string-decoder"
```

### Example output

```java
// b64: Hello, World!
private static final String ENCODED = "SGVsbG8sIFdvcmxkIQ==";

void example() {
    decode(new String[]{"eHBvc2Vk", "ZnJpZGE=", "Z3Vt"}); //
    // b64[0]: xposed
    // b64[1]: frida
    // b64[2]: gum
}

// bytes: "Hello, World!"
private static final byte[] MSG = {72, 101, 108, 108, 111, 44, 32, 87, 111, 114, 108, 100, 33};

// bytes: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private static final int[] BASE64_ENCODE_TABLE = {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, ...};
```
