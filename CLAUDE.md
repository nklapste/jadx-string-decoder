# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

Gradle requires JDK 17+. Set `JAVA_HOME` to your JDK 17 installation before running any Gradle task.

| Task | Command |
|------|---------|
| Build + test | `./gradlew build` |
| Build + produce distributable jar | `./gradlew dist` |
| Run tests only | `./gradlew test` |
| Run a single test | `./gradlew test --tests "jadx.plugins.stringdecoder.JadxStringDecoderPluginTest.integrationTest"` |

The distributable jar lands in `build/dist/`.

## Architecture

A JADX decompiler plugin that detects likely Base64-encoded string constants and byte array fields, adding inline comments with their decoded values. The plugin entry point is declared via Java SPI in `src/main/resources/META-INF/services/jadx.api.plugins.JadxPlugin`.

### Plugin lifecycle

```
JadxStringDecoderPlugin.init()
  → registers B64DeobfuscateOptions
  → registers B64DeobfuscatePass  (method-body Base64 string detection)
  → registers B64FieldInitPass    (field initializer Base64 detection)
  → registers ByteArrayStringPass (byte[]/int[] field string detection)
```

All three passes are skipped when `enable` is false.

### Pass overview

**`B64DeobfuscatePass`** — runs `.after("SSATransform").before("ConstInlineVisitor")`

Iterates every method's basic blocks looking for `ConstStringNode` instructions. On a likely-Base64 string it attaches a `CodeComment` to the instruction. `ConstInlineVisitor` later inlines the const into its consuming instruction and calls `inheritMetadata`, which propagates the comment. Code generation in `InsnGen`/`RegionGen` calls `CodeGenUtils.addCodeComments` to render it on the same line.

Also detects when a string is a direct argument to a `Base64.decode`-like call (via `SSAVar.getUseList()`) and bypasses false-positive heuristics in that case — the call itself is strong evidence of intent.

**`B64FieldInitPass`** — runs `.after("ExtractFieldInit")`

Handles two field patterns:
- *`FIELD_INIT_INSN`* — field set via `<clinit>` or constructor; `ExtractFieldInit` extracts the init expression. If the init is a direct `ConstStringNode`, detects normally. If it's a complex expression (e.g. `new String(Base64.decode("...", 0))`), walks the instruction arg tree recursively via `RegisterArg.getAssignInsn()` / `InsnWrapArg.getWrapInsn()` to find the embedded string. When the string is a direct arg to a `Base64.decode`-like `InvokeNode`, forces decode without heuristic checks.
- *`CONSTANT_VALUE`* — `static final` field with a literal string value encoded in the class file; retrieved via `JadxAttrType.CONSTANT_VALUE` / `EncodedValue`.

Comments are added via `field.addCodeComment()` which renders on a separate line above the field declaration (`FieldNode implements ICodeNode` → `startNewLine = true` in `CodeGenUtils`).

**`ByteArrayStringPass`** — runs `.after("ExtractFieldInit")`

Looks for `byte[]` and `int[]` fields initialised with a `FilledNewArrayNode` of all literal values. For `int[]`, any element outside `[0, 255]` (e.g. a `-1` sentinel in a decode table) causes the whole array to be skipped. The elements are then treated as bytes, decoded as UTF-8, and if the result is sufficiently printable a `bytes: "..."` comment is added on the field. `visit(ClassNode)` returns `true` so `DepthTraversal` recurses into inner classes.

### Detection logic (`B64Detector`)

`B64Detector.detect(String, B64DeobfuscateOptions)` runs an ordered pipeline (all thresholds configurable via options). The pre-decode filters live in the `INPUT_FILTERS` array and short-circuit on the first rejection:

1. Blocklist check (`B64FalsePositives`) — known false positives (e.g. Android class names) rejected first
2. Valid Base64 length (`requireValidLength`, default true) — length *excluding* `\n`/`\r` must be divisible by 4 (line breaks are stripped via `significantLength` so PEM/MIME-wrapped strings aren't rejected for their wrapping)
3. Identifier-shape skips, only applied to strings under 40 chars:
   - `skipCamelCase` (default true) — `^[a-z]+([A-Z][a-z]+)+$`
   - `skipPascalCase` (default true) — `^[A-Z][a-z]+([A-Z][a-z0-9]+)+$`
   - `skipSnakeCase` (default true) — all-caps (`FOO_BAR`) or all-lower (`foo_bar`)
   - `skipDictionaryWords` (default true) — every camelCase/underscore segment is a known word (`B64DictionaryFilter` / `words.txt`)
4. Charset validation — must match the standard or URL-safe Base64 alphabet (`\n`/`\r` permitted)

Then decode and apply post-decode heuristics:

5. Decode with `Base64.getDecoder()`, then `Base64.getUrlDecoder()`; the MIME decoder is also tried when the string contains `\n`/`\r`
6. Strict UTF-8 decode (`CodingErrorAction.REPORT`)
7. Minimum printable-ASCII ratio (`minPrintablePercent`, default 90%)
8. Minimum alphanumeric ratio (`minAlphanumericPercent`, default 35%)
9. Minimum decoded length (`minDecodedLength`, default 4; 0 = disabled)
10. Truncation (`maxCommentLength`, default 100; 0 = unlimited)

Two helpers bypass parts of the pipeline:
- `B64Detector.decodeForced(String, int)` — skips all charset and heuristic filters and uses `CodingErrorAction.REPLACE` instead of `REPORT`. Used when the string is an explicit arg to a `Base64.decode`-like call (recognised by `B64DecodeCalls.isDecodeCall`).
- `B64Detector.decodeIfValid(String, int)` — applies only the charset check + strict UTF-8 decode, no heuristic filters. Used for sibling array elements once one element in the array has passed full detection (the "anchor").

### False-positive prevention

The `<clinit>` / field-init skip: `B64DeobfuscatePass` collects all `CONSTANT_VALUE` field string literals in the class (lazily, on the first B64 hit) and skips any `ConstStringNode` whose value matches — those are handled by `B64FieldInitPass` to avoid double annotation.

### Key JADX API types

| Type | Notes |
|------|-------|
| `JadxDecompilePass` | Interface for decompile-phase passes; `visit(ClassNode)` returns `true` to enable method traversal |
| `ConstStringNode` | `InsnNode` subtype for `CONST_STR`; use `.getString()` |
| `InvokeNode` | `InsnNode` subtype for method calls; use `.getCallMth()` → `MethodInfo` |
| `FilledNewArrayNode` | `InsnNode` subtype for `filled-new-array`; use `.getElemType()` and `.getArg(i)` |
| `AType.CODE_COMMENTS` | `AttrList<CodeComment>` — add with `insn.addAttr(AType.CODE_COMMENTS, new CodeComment(text, CommentStyle.LINE))`. `InsnNode` does **not** extend `NotificationAttrNode`, so use `addAttr` directly |
| `FieldNode.addCodeComment(text)` | Convenience method that renders a comment on a line above the field declaration |
| `OrderedJadxPassInfo` | Supports `.before(name)` and `.after(name)`; names are JADX internal visitor class names, not `@JadxVisitor` name attributes |
| `AType.FIELD_INIT_INSN` / `FieldInitInsnAttr` | Set by `ExtractFieldInit` on fields whose init is extracted from `<clinit>` or a constructor |
| `JadxAttrType.CONSTANT_VALUE` / `EncodedValue` | Literal field value encoded in the class file; check `EncodedType.ENCODED_STRING` before casting |
| `RegisterArg.getAssignInsn()` | Follows a register back to the instruction that assigned it (SSA form) |
| `SSAVar.getUseList()` | All uses of an SSA register — used to find the instruction consuming a `ConstStringNode`'s result |

### jadx-core dependency

`jadx-core` is `compileOnly` — excluded from the output jar. The plugin jar is loaded into an existing JADX installation at runtime.
