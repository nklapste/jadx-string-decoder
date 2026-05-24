# Tests Base64-obfuscated reflection strings inside try-catch blocks.
#
# Method getUid()I:
#   Two Base64.decode args in sequence inside a simple .catchall block:
#     "YW5kcm9pZC5vcy5Vc2VySGFuZGxl" -> "android.os.UserHandle"
#     "Z2V0VXNlcklk"                  -> "getUserId"
#
# Method getInfo(String,II)Object:
#   Three Base64.decode args inside a conditional try-catch with a goto-merge
#   point (SYNTHETIC RETURN). The last string is inside the else-branch before
#   the merge — exercises findStatementInsn's SYNTHETIC-instruction stop:
#     "YW5kcm9pZC5hcHAuQWN0aXZpdHlUaHJlYWQ=" -> "android.app.ActivityThread"
#     "Z2V0UGFja2FnZU1hbmFnZXI="               -> "getPackageManager"
#     "Z2V0UGFja2FnZUluZm8="                   -> "getPackageInfo"

.class public final Lb64/B64ReflectTryCatch;
.super Ljava/lang/Object;

.field private static cachedUid:I = -0x80000000

.field private static cachedPm:Ljava/lang/Object;

.method static constructor <clinit>()V
    .registers 0
    return-void
.end method

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

# Simple case: two consecutive Base64.decode args inside a .catchall block.
.method private static getUid()I
    .registers 6

    sget v0, Lb64/B64ReflectTryCatch;->cachedUid:I

    const/high16 v1, -0x80000000

    if-eq v0, v1, :cond_compute

    return v0

    :cond_compute
    const/4 v1, 0x0

    :try_start_0
    new-instance v0, Ljava/lang/String;

    const-string v2, "YW5kcm9pZC5vcy5Vc2VySGFuZGxl"

    invoke-static {v2, v1}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B

    move-result-object v2

    invoke-direct {v0, v2}, Ljava/lang/String;-><init>([B)V

    invoke-static {v0}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v0

    new-instance v2, Ljava/lang/String;

    const-string v3, "Z2V0VXNlcklk"

    invoke-static {v3, v1}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/String;-><init>([B)V

    const/4 v3, 0x1

    new-array v4, v3, [Ljava/lang/Class;

    sget-object v5, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v5, v4, v1

    invoke-virtual {v0, v2, v4}, Ljava/lang/Class;->getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    invoke-virtual {v2, v3}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V

    new-array v3, v3, [Ljava/lang/Object;

    invoke-static {}, Landroid/os/Process;->myUid()I

    move-result v4

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    aput-object v4, v3, v1

    invoke-virtual {v2, v0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Integer;

    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result v0

    sput v0, Lb64/B64ReflectTryCatch;->cachedUid:I
    :try_end_0

    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    return v0

    :catchall_0
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    return v1
.end method

# Complex case: conditional branching + goto-merge (SYNTHETIC RETURN) with three
# Base64.decode args. "getPackageInfo" is in the else-branch before the merge point.
.method public static getInfo(Ljava/lang/String;II)Ljava/lang/Object;
    .registers 12

    const/4 v0, 0x0

    :try_start_1

    sget-object v1, Lb64/B64ReflectTryCatch;->cachedPm:Ljava/lang/Object;

    const/4 v2, 0x0

    if-nez v1, :cond_pm_ready

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

    sput-object v1, Lb64/B64ReflectTryCatch;->cachedPm:Ljava/lang/Object;

    :cond_pm_ready

    sget v1, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v3, 0x21

    if-lt v1, v3, :cond_old_sdk

    sget-object p2, Lb64/B64ReflectTryCatch;->cachedPm:Ljava/lang/Object;

    # delegate path (API >= 33) — no Base64 strings
    const/4 v0, 0x0

    goto :goto_done

    :cond_old_sdk

    new-instance v1, Ljava/lang/String;

    const-string v3, "Z2V0UGFja2FnZUluZm8="

    invoke-static {v3, v2}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B

    move-result-object v3

    invoke-direct {v1, v3}, Ljava/lang/String;-><init>([B)V

    sget-object v3, Lb64/B64ReflectTryCatch;->cachedPm:Ljava/lang/Object;

    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v3

    const/4 v4, 0x3

    new-array v5, v4, [Ljava/lang/Class;

    const-class v6, Ljava/lang/String;

    aput-object v6, v5, v2

    sget-object v6, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    const/4 v7, 0x1

    aput-object v6, v5, v7

    const/4 v8, 0x2

    aput-object v6, v5, v8

    invoke-virtual {v3, v1, v5}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    sget-object v3, Lb64/B64ReflectTryCatch;->cachedPm:Ljava/lang/Object;

    new-array v4, v4, [Ljava/lang/Object;

    aput-object p0, v4, v2

    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    aput-object p0, v4, v7

    invoke-static {p2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    aput-object p0, v4, v8

    invoke-virtual {v1, v3, v4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Landroid/content/pm/PackageInfo;
    :try_end_1

    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    move-object v0, p0

    :goto_done
    return-object v0

    :catchall_1
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    goto :goto_done
.end method
