.class LFilledArrayB64;
.super Ljava/lang/Object;

# String[] where five elements are Base64-encoded; the comment should be a multi-line
# comment on the filled-new-array instruction:
#   b64[0]: Hello, World!
#   b64[1]: hello
#   b64[2]: foobar
#   b64[3]: base64
#   b64[4]: testing
.method public static test()[Ljava/lang/String;
    .registers 6
    const-string v0, "SGVsbG8sIFdvcmxkIQ=="
    const-string v1, "aGVsbG8="
    const-string v2, "Zm9vYmFy"
    const-string v3, "YmFzZTY0"
    const-string v4, "dGVzdGluZw=="
    filled-new-array {v0, v1, v2, v3, v4}, [Ljava/lang/String;
    move-result-object v5
    return-object v5
.end method
