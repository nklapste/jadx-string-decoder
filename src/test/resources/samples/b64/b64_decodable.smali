.class LB64Decodable;
.super Ljava/lang/Object;

# "aGVsbG8=" is not intentionally Base64-encoded but decodes to "hello"
.method public static test()Ljava/lang/String;
    .registers 1
    const-string v0, "aGVsbG8="
    return-object v0
.end method
