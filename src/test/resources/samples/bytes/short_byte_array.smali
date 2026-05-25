.class public Lbytes/ShortByteArray;
.super Ljava/lang/Object;

# byte[] spelling "Hi" — fully printable but only 2 chars, below the default minDecodedLength=4.
# With minDecodedLength=2 the comment must appear; with the default it must not.
.field public static final DATA:[B

.method static constructor <clinit>()V
    .registers 2

    const/4 v0, 0x2
    new-array v0, v0, [B
    fill-array-data v0, :array_data
    sput-object v0, Lbytes/ShortByteArray;->DATA:[B

    return-void

    :array_data
    .array-data 1
        72 105
    .end array-data

.end method
