.class public Lb64/StringArrayFieldB64;
.super Ljava/lang/Object;

# String[] field initialised with new-array + aput-object (large-array pattern).
# After ReplaceNewArray converts the APUT sequence to FilledNewArrayNode,
# B64FieldInitPass.findAndAnnotateFilledArray should produce indexed comments
# for the two Base64 elements at indices 1 and 3.
.field public static strings:[Ljava/lang/String;

.method static constructor <clinit>()V
    .registers 4

    const/4 v3, 0x4
    new-array v0, v3, [Ljava/lang/String;

    const-string v1, "just a plain string"
    const/4 v2, 0x0
    aput-object v1, v0, v2

    const-string v1, "SGVsbG8sIFdvcmxkIQ=="
    const/4 v2, 0x1
    aput-object v1, v0, v2

    const-string v1, "not base64 at all!!!"
    const/4 v2, 0x2
    aput-object v1, v0, v2

    const-string v1, "aGVsbG8="
    const/4 v2, 0x3
    aput-object v1, v0, v2

    sput-object v0, Lb64/StringArrayFieldB64;->strings:[Ljava/lang/String;
    return-void
.end method
