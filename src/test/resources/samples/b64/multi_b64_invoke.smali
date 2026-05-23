.class public LMultiB64Invoke;
.super Ljava/lang/Object;

# Models the pattern:
#   Class.forName(new String(Base64.decode("YW5k...", 0)))
#       .getMethod(new String(Base64.decode("Z2V0...", 0)), new Class[0])
#       .invoke(null, new Object[0])
# Both const-strings are direct args to Base64.decode — both should get b64: comments.

.method public static a()Ljava/lang/Object;
    .registers 6

    # "YW5kcm9pZC5hcHAuQWN0aXZpdHlUaHJlYWQ=" -> "android.app.ActivityThread"
    const-string v0, "YW5kcm9pZC5hcHAuQWN0aXZpdHlUaHJlYWQ="
    const/4 v1, 0x0
    invoke-static {v0, v1}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v0
    new-instance v2, Ljava/lang/String;
    invoke-direct {v2, v0}, Ljava/lang/String;-><init>([B)V
    invoke-static {v2}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;
    move-result-object v2

    # "Z2V0UGFja2FnZU1hbmFnZXI=" -> "getPackageManager"
    const-string v0, "Z2V0UGFja2FnZU1hbmFnZXI="
    const/4 v1, 0x0
    invoke-static {v0, v1}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v0
    new-instance v3, Ljava/lang/String;
    invoke-direct {v3, v0}, Ljava/lang/String;-><init>([B)V

    const/4 v4, 0x0
    new-array v4, v4, [Ljava/lang/Class;
    invoke-virtual {v2, v3, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v2

    const/4 v3, 0x0
    const/4 v4, 0x0
    new-array v4, v4, [Ljava/lang/Object;
    invoke-virtual {v2, v3, v4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v0

    return-object v0
.end method
