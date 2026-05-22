## jadx-string-decoder

A JADX plugin that detects likely encoded string constants during decompilation and annotates them with decoded comments inline.

### Features

**Base64 string detection** — scans string constants in method bodies, and field initializers. When a string looks to be Base64 encoded, a `// b64: <decoded>` comment is added next to it.

**Byte array string detection** — scans `byte[]` fields initialised with literal byte arrays. When the bytes decode as valid UTF-8 with sufficient printable characters, a `// bytes: "<string>"` comment is added.

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
```
