.class public LStaticFieldB64;
.super Ljava/lang/Object;

.field public static myField:Ljava/lang/String;

.method static constructor <clinit>()V
    .registers 1
    const-string v0, "aGVsbG8="
    sput-object v0, LStaticFieldB64;->myField:Ljava/lang/String;
    return-void
.end method
