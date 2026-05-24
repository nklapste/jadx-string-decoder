.class public Lb64/B64InTryCatch;
.super Ljava/lang/Object;

# Mirrors the pattern from com.igexin.push.g.k.a(String,II):
# new String(Base64.decode("Z2V0UGFja2FnZUluZm8=", 0)) inside a large .catchall block.
# "getPackageInfo" is camelCase — normal detect() rejects it, but decodeForced()
# (triggered by the direct Base64.decode arg) should still produce a b64: comment.
.method public static test()Ljava/lang/String;
    .registers 5

    const/4 v0, 0x0

    :try_start_0
    new-instance v1, Ljava/lang/String;
    const-string v2, "Z2V0UGFja2FnZUluZm8="
    invoke-static {v2, v0}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v2
    invoke-direct {v1, v2}, Ljava/lang/String;-><init>([B)V
    invoke-static {v1}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;
    move-result-object v0
    :try_end_b
    .catchall {:try_start_0 .. :try_end_b} :catchall_c

    return-object v0

    :catchall_c
    move-exception v1
    const/4 v0, 0x0
    return-object v0
.end method
