.class public Lpem/KeyStore;
.super Ljava/lang/Object;

# CONSTANT_VALUE String fields (PEM-wrapped Base64)
.field public static final GOOGLE_KEY:Ljava/lang/String; = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7l1ex+HA220Dpn7mthvsTWpdamgu\nD/9/SQ59dx9EIm29sa/6FsvHrcV30lacqrewLVQBXT5DKyqO107sSHVBpA=="

# byte[] field initialised by Base64.decode(GOOGLE_KEY, 0)
.field public static final GOOGLE_BYTES:[B

.method static constructor <clinit>()V
    .registers 2

    const-string v0, "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7l1ex+HA220Dpn7mthvsTWpdamgu\nD/9/SQ59dx9EIm29sa/6FsvHrcV30lacqrewLVQBXT5DKyqO107sSHVBpA=="
    const/4 v1, 0x0
    invoke-static {v0, v1}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
    move-result-object v0
    sput-object v0, Lpem/KeyStore;->GOOGLE_BYTES:[B
    return-void
.end method
