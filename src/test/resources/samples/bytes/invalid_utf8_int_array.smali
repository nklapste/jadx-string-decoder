.class public Lbytes/InvalidUtf8IntArray;
.super Ljava/lang/Object;

# int[] where element 128 (0x80) is a lone UTF-8 continuation byte — valid in [0,255]
# so extractByteLiterals accepts it, but decodeUtf8 with CodingErrorAction.REPORT rejects
# the sequence and decodeUtf8OrNull returns null. ByteArrayStringPass must not annotate this.
.field public static final DATA:[I

.method static constructor <clinit>()V
    .registers 2

    const/4 v0, 0x3
    new-array v0, v0, [I
    fill-array-data v0, :array_data
    sput-object v0, Lbytes/InvalidUtf8IntArray;->DATA:[I

    return-void

    :array_data
    .array-data 4
        72 128 111
    .end array-data

.end method
