.class LLooksLikeB64;
.super Ljava/lang/Object;

# "AQIDBAUG" matches the Base64 charset but decodes to binary [1,2,3,4,5,6] — not printable UTF-8
.method public static test()Ljava/lang/String;
    .registers 1
    const-string v0, "AQIDBAUG"
    return-object v0
.end method
