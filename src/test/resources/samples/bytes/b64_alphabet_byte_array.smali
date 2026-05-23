.class public Lbytes/B64Alphabets;
.super Ljava/lang/Object;

# Standard Base64 encode alphabet as byte[] — ABCDE...+/
.field public static final alphabet:[B

# URL-safe Base64 encode alphabet as int[] — ABCDE...-_
# int[] is a common real-world pattern to avoid signed-byte confusion with high values.
.field public static final alphabetUrlSafe:[I

.method static constructor <clinit>()V
    .registers 2

    const/16 v0, 0x40
    new-array v0, v0, [B
    fill-array-data v0, :array_standard_bytes
    sput-object v0, Lbytes/B64Alphabets;->alphabet:[B

    const/16 v0, 0x40
    new-array v0, v0, [I
    fill-array-data v0, :array_urlsafe_ints
    sput-object v0, Lbytes/B64Alphabets;->alphabetUrlSafe:[I

    return-void

    :array_standard_bytes
    .array-data 1
        65 66 67 68 69 70 71 72 73 74 75 76 77 78 79 80
        81 82 83 84 85 86 87 88 89 90 97 98 99 100 101 102
        103 104 105 106 107 108 109 110 111 112 113 114 115 116 117 118
        119 120 121 122 48 49 50 51 52 53 54 55 56 57 43 47
    .end array-data

    :array_urlsafe_ints
    .array-data 4
        65 66 67 68 69 70 71 72 73 74 75 76 77 78 79 80
        81 82 83 84 85 86 87 88 89 90 97 98 99 100 101 102
        103 104 105 106 107 108 109 110 111 112 113 114 115 116 117 118
        119 120 121 122 48 49 50 51 52 53 54 55 56 57 45 95
    .end array-data

.end method
