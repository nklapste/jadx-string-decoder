.class public Lbytes/IntArrayUtf8;
.super Ljava/lang/Object;

# int[] storing "café" as unsigned UTF-8 bytes — values 195 and 169 exceed signed byte range
.field public static final MSG:[I

.method static constructor <clinit>()V
    .registers 2

    const/4 v0, 0x5
    new-array v0, v0, [I
    fill-array-data v0, :array_cafe
    sput-object v0, Lbytes/IntArrayUtf8;->MSG:[I
    return-void

    :array_cafe
    .array-data 4
        99 97 102 195 169
    .end array-data

.end method
