.class public Lbytes/LongByteArray;
.super Ljava/lang/Object;

# byte[] of 120 ASCII 'A' chars (value 65) — default maxCommentLength=100 truncates the
# bytes: comment to 100 chars + "..."; maxCommentLength=10 truncates to 10 chars + "..."
.field public static final DATA:[B

.method static constructor <clinit>()V
    .registers 2

    const/16 v0, 0x78
    new-array v0, v0, [B
    fill-array-data v0, :array_data
    sput-object v0, Lbytes/LongByteArray;->DATA:[B

    return-void

    :array_data
    .array-data 1
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
        65 65 65 65 65 65 65 65 65 65
    .end array-data

.end method
