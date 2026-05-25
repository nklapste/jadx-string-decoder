.class public Ltest/FilledArrayNoAnchor;
.super Ljava/lang/Object;

# String[] where both elements ("aGVs") decode to valid Base64+UTF-8 ("hel", 3 chars)
# but fail the heuristic pipeline because decoded length 3 < default minDecodedLength=4.
# No element passes full detect(), so hasAnchor=false and no b64: comment is emitted.
.method public static test()[Ljava/lang/String;
    .registers 3
    const-string v0, "aGVs"
    const-string v1, "aGVs"
    filled-new-array {v0, v1}, [Ljava/lang/String;
    move-result-object v2
    return-object v2
.end method
