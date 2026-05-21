.class public Lbytearr/HelloWorld;
.super Ljava/lang/Object;

.field public static final GREETING:[B

.method static constructor <clinit>()V
    .registers 2

    const/16 v0, 0xd
    new-array v0, v0, [B
    fill-array-data v0, :array_greeting
    sput-object v0, Lbytearr/HelloWorld;->GREETING:[B
    return-void

    :array_greeting
    .array-data 1
        72 101 108 108 111 44 32 87 111 114 108 100 33
    .end array-data
.end method
