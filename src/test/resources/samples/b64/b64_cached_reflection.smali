# Tests Base64-obfuscated reflection strings in a real-world caching pattern.
#
# Method getPackages(int):
#   Conditional cached lookup structure with three Base64.decode args:
#
#   First block (if cachedPm == null):
#     "YW5kcm9pZC5hcHAuQWN0aXZpdHlUaHJlYWQ=" -> "android.app.ActivityThread"
#     "Z2V0UGFja2FnZU1hbmFnZXI="              -> "getPackageManager"
#     Both are in a fully-inlined chain (no intervening invokes between each
#     <init> and its post-ctor user) -> grouped as indexed b64[0]/b64[1] comments.
#
#   Second block (if cachedGetPkgs == null):
#     "Z2V0UGFja2FnZXNGb3JVaWQ=" -> "getPackagesForUid"
#     Has a blocking Object.getClass() invoke between <init> and getMethod,
#     but JADX inlines the constructor anyway (getClass is itself inlined first),
#     so the comment must land on the enclosing getMethod statement.

.class public final Lb64/B64CachedReflect;
.super Ljava/lang/Object;


# static fields
.field private static flagA:I = -0x80000000

.field private static cachedPm:Ljava/lang/Object;

.field private static flagC:Ljava/lang/reflect/Method;

.field private static cachedGetPkgs:Ljava/lang/reflect/Method;


.method public static getPackages(I)Landroid/content/pm/PackageInfo;
    .registers 8

    .line 2
    const/4 v0, 0x0

    :try_start_1
    sget-object v1, Lb64/B64CachedReflect;->cachedPm:Ljava/lang/Object;

    const/4 v2, 0x0

    if-nez v1, :cond_2e

    new-instance v1, Ljava/lang/String;

    const-string v3, "YW5kcm9pZC5hcHAuQWN0aXZpdHlUaHJlYWQ="

    invoke-static {v3, v2}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B

    move-result-object v3

    invoke-direct {v1, v3}, Ljava/lang/String;-><init>([B)V

    invoke-static {v1}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v1

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

    sput-object v1, Lb64/B64CachedReflect;->cachedPm:Ljava/lang/Object;

    :cond_2e
    sget-object v1, Lb64/B64CachedReflect;->cachedGetPkgs:Ljava/lang/reflect/Method;

    const/4 v3, 0x1

    if-nez v1, :cond_50

    new-instance v1, Ljava/lang/String;

    const-string v4, "Z2V0UGFja2FnZXNGb3JVaWQ="

    invoke-static {v4, v2}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B

    move-result-object v4

    invoke-direct {v1, v4}, Ljava/lang/String;-><init>([B)V

    sget-object v4, Lb64/B64CachedReflect;->cachedPm:Ljava/lang/Object;

    invoke-virtual {v4}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    new-array v5, v3, [Ljava/lang/Class;

    sget-object v6, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v6, v5, v2

    invoke-virtual {v4, v1, v5}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    sput-object v1, Lb64/B64CachedReflect;->cachedGetPkgs:Ljava/lang/reflect/Method;

    :cond_50
    sget-object v1, Lb64/B64CachedReflect;->cachedGetPkgs:Ljava/lang/reflect/Method;

    sget-object v4, Lb64/B64CachedReflect;->cachedPm:Ljava/lang/Object;

    new-array v5, v3, [Ljava/lang/Object;

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    aput-object p0, v5, v2

    invoke-virtual {v1, v4, v5}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, [Ljava/lang/String;

    if-eqz p0, :cond_73

    array-length v1, p0

    if-ne v1, v3, :cond_73

    aget-object p0, p0, v2

    invoke-static {p0, v2}, Lb64/B64CachedReflect;->getPackageByName(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object p0
    :try_end_6d
    .catchall {:try_start_1 .. :try_end_6d} :catchall_6f

    move-object v0, p0

    goto :goto_73

    :catchall_6f
    move-exception p0

    invoke-static {p0}, Lhelper/ErrUtil;->log(Ljava/lang/Throwable;)V

    :cond_73
    :goto_73
    return-object v0
.end method
