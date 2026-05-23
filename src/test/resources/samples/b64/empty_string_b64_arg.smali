.class public Lb64/EmptyStringB64Arg;
.super Ljava/lang/Object;

# Empty string passed to Base64.decode — real-world pattern where "" is loaded early
# and later used as the input to a decode call (e.g. as a default/fallback value).
.method public static process()V
    .registers 3

    const-string v0, ""
    const/4 v1, 0x0
    invoke-static {v0, v1}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v0
    return-void
.end method
