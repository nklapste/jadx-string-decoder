.class public Lmultiline/HelloWorld;
.super Ljava/lang/Object;

# Field whose value is a PEM-style line-wrapped Base64 string (64-char lines)
# The value is "SGVsbG8s\nIFdvcmxk\nIQ==" which decodes to "Hello, World!"
.field public static final ENCODED:Ljava/lang/String; = "SGVsbG8s\nIFdvcmxk\nIQ=="

# byte[] field initialised by Base64.decode(ENCODED, 0)
.field public static final DECODED:[B

.method static constructor <clinit>()V
    .registers 3

    const-string v0, "SGVsbG8s\nIFdvcmxk\nIQ=="
    const/4 v1, 0x0
    invoke-static {v0, v1}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v0
    sput-object v0, Lmultiline/HelloWorld;->DECODED:[B
    return-void
.end method
