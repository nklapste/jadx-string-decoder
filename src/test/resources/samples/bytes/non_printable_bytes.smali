.class public Lbytes/NonPrintableBytes;
.super Ljava/lang/Object;

# byte[] whose values are all ASCII control characters (0x01-0x08) — valid UTF-8 but 0% printable.
# ByteArrayStringPass must not annotate this because it falls below the default 20% printable threshold.
.field public static final DATA:[B

.method static constructor <clinit>()V
    .registers 2

    const/16 v0, 0x8
    new-array v0, v0, [B
    fill-array-data v0, :array_data
    sput-object v0, Lbytes/NonPrintableBytes;->DATA:[B

    return-void

    :array_data
    .array-data 1
        1 2 3 4 5 6 7 8
    .end array-data

.end method
