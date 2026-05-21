.class public LIdentifierLikeB64;
.super Ljava/lang/Object;

# "fillItem" is a valid identifier that happens to match the Base64 charset but decodes to garbage
.method public test()Ljava/lang/String;
    .registers 2
    const-string v0, "fillItem"
    return-object v0
.end method
