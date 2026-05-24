.class public Lb64/B64ConditionalTryCatch;
.super Ljava/lang/Object;

# Mirrors the exact structure of com.igexin.push.g.k.a(String,II):
#   - Large .catchall block
#   - if (cache == null) branch: two Base64.decode strings -> reflection -> sput to cache
#   - if (sdk >= 33) branch: delegate call
#   - else branch: third Base64.decode string ("Z2V0UGFja2FnZUluZm8=") -> reflection chain
#   - return PackageInfo (or null)
#
# "Z2V0UGFja2FnZUluZm8=" decodes to "getPackageInfo" — camelCase, filtered by detect(),
# but isUsedAsBase64DecodeArg should trigger decodeForced() to bypass that.

.field private static cache:Ljava/lang/Object;

.method public static test(Ljava/lang/String;II)Ljava/lang/Object;
    .registers 12

    const/4 v0, 0x0

    :try_start_1

    sget-object v1, Lb64/B64ConditionalTryCatch;->cache:Ljava/lang/Object;
    const/4 v2, 0x0
    if-nez v1, :cond_cache_ready

    # cache is null: decode "YW5kcm9pZC5hcHAuQWN0aXZpdHlUaHJlYWQ=" -> "android.app.ActivityThread"
    new-instance v1, Ljava/lang/String;
    const-string v3, "YW5kcm9pZC5hcHAuQWN0aXZpdHlUaHJlYWQ="
    invoke-static {v3, v2}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v3
    invoke-direct {v1, v3}, Ljava/lang/String;-><init>([B)V
    invoke-static {v1}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;
    move-result-object v1

    # decode "Z2V0UGFja2FnZU1hbmFnZXI=" -> "getPackageManager"
    new-instance v3, Ljava/lang/String;
    const-string v4, "Z2V0UGFja2FnZU1hbmFnZXI="
    invoke-static {v4, v2}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v4
    invoke-direct {v3, v4}, Ljava/lang/String;-><init>([B)V

    new-array v4, v2, [Ljava/lang/Class;
    invoke-virtual {v1, v3, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v1

    new-array v3, v2, [Ljava/lang/Object;
    invoke-virtual {v1, v0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v1

    sput-object v1, Lb64/B64ConditionalTryCatch;->cache:Ljava/lang/Object;

    :cond_cache_ready
    # if (sdk >= 33): delegate
    const/16 v1, 0x21
    const/16 v3, 0x21
    if-lt v1, v3, :cond_sdk_old

    sget-object v1, Lb64/B64ConditionalTryCatch;->cache:Ljava/lang/Object;
    invoke-static {v1, p0, p1}, Lb64/B64ConditionalTryCatch;->delegate(Ljava/lang/Object;Ljava/lang/String;I)Ljava/lang/Object;
    move-result-object v0

    goto :goto_done

    :cond_sdk_old
    # sdk < 33: decode "Z2V0UGFja2FnZUluZm8=" -> "getPackageInfo"
    new-instance v1, Ljava/lang/String;
    const-string v3, "Z2V0UGFja2FnZUluZm8="
    invoke-static {v3, v2}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v3
    invoke-direct {v1, v3}, Ljava/lang/String;-><init>([B)V

    sget-object v3, Lb64/B64ConditionalTryCatch;->cache:Ljava/lang/Object;
    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    move-result-object v3

    const/4 v4, 0x3
    new-array v5, v4, [Ljava/lang/Class;
    const-class v6, Ljava/lang/String;
    aput-object v6, v5, v2

    invoke-virtual {v3, v1, v5}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v1

    new-array v4, v4, [Ljava/lang/Object;
    aput-object p0, v4, v2

    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object p0
    aput-object p0, v4, v2

    invoke-virtual {v1, v3, v4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object p0

    :try_end_60
    .catchall {:try_start_1 .. :try_end_60} :catchall_61

    move-object v0, p0

    :goto_done
    return-object v0

    :catchall_61
    move-exception p0
    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    goto :goto_done
.end method

.method private static delegate(Ljava/lang/Object;Ljava/lang/String;I)Ljava/lang/Object;
    .registers 4
    const/4 v0, 0x0
    return-object v0
.end method
