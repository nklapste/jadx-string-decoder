.class public Lbytes/IntArraySentinel;
.super Ljava/lang/Object;

# int[] containing -1 — the standard sentinel in Base64 decode tables for "not a valid char"
# ByteArrayStringPass must reject the entire array when any element is outside [0, 255]
.field public static final TABLE:[I

.method static constructor <clinit>()V
    .registers 2

    const/4 v0, 0x5
    new-array v0, v0, [I
    fill-array-data v0, :array_data
    sput-object v0, Lbytes/IntArraySentinel;->TABLE:[I

    return-void

    :array_data
    .array-data 4
        65 66 67 -1 65
    .end array-data

.end method
