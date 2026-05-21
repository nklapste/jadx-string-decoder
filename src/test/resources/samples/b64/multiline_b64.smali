.class LMultilineB64;
.super Ljava/lang/Object;

# "TGluZSAxCkxpbmUgMgpMaW5lIDM=" decodes to "Line 1\nLine 2\nLine 3"
.method public static test()Ljava/lang/String;
    .registers 1
    const-string v0, "TGluZSAxCkxpbmUgMgpMaW5lIDM="
    return-object v0
.end method
