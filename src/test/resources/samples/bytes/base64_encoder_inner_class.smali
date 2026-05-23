# Synthetic test fixture — NOT taken from any APK.
# Reproduces the inner-class byte[] field pattern:
#   - static class inside an outer class (EnclosingClass + InnerClass annotations)
#   - two byte[] fields (standard and URL-safe Base64 alphabets) initialised in <clinit>
#   - a virtual method whose body is irrelevant to the test
# Tests that ByteArrayStringPass recurses into inner classes (visit() returns true).
.class Ltestdata/b64impl/Base64Impl$Encoder;
.super Ltestdata/b64impl/Base64Impl$Coder;
.source "Base64Impl.java"

# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Ltestdata/b64impl/Base64Impl;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x8
    name = "Encoder"
.end annotation


# static fields
.field private static final k:[B

.field private static final l:[B


# instance fields
.field public final useUrlSafe:Z


# direct methods
.method static constructor <clinit>()V
    .registers 2

    const/16 v0, 0x40

    new-array v1, v0, [B
    fill-array-data v1, :array_standard
    sput-object v1, Ltestdata/b64impl/Base64Impl$Encoder;->k:[B

    new-array v0, v0, [B
    fill-array-data v0, :array_urlsafe
    sput-object v0, Ltestdata/b64impl/Base64Impl$Encoder;->l:[B

    return-void

    nop

    :array_standard
    .array-data 1
        0x41t 0x42t 0x43t 0x44t 0x45t 0x46t 0x47t 0x48t
        0x49t 0x4at 0x4bt 0x4ct 0x4dt 0x4et 0x4ft 0x50t
        0x51t 0x52t 0x53t 0x54t 0x55t 0x56t 0x57t 0x58t
        0x59t 0x5at 0x61t 0x62t 0x63t 0x64t 0x65t 0x66t
        0x67t 0x68t 0x69t 0x6at 0x6bt 0x6ct 0x6dt 0x6et
        0x6ft 0x70t 0x71t 0x72t 0x73t 0x74t 0x75t 0x76t
        0x77t 0x78t 0x79t 0x7at 0x30t 0x31t 0x32t 0x33t
        0x34t 0x35t 0x36t 0x37t 0x38t 0x39t 0x2bt 0x2ft
    .end array-data

    :array_urlsafe
    .array-data 1
        0x41t 0x42t 0x43t 0x44t 0x45t 0x46t 0x47t 0x48t
        0x49t 0x4at 0x4bt 0x4ct 0x4dt 0x4et 0x4ft 0x50t
        0x51t 0x52t 0x53t 0x54t 0x55t 0x56t 0x57t 0x58t
        0x59t 0x5at 0x61t 0x62t 0x63t 0x64t 0x65t 0x66t
        0x67t 0x68t 0x69t 0x6at 0x6bt 0x6ct 0x6dt 0x6et
        0x6ft 0x70t 0x71t 0x72t 0x73t 0x74t 0x75t 0x76t
        0x77t 0x78t 0x79t 0x7at 0x30t 0x31t 0x32t 0x33t
        0x34t 0x35t 0x36t 0x37t 0x38t 0x39t 0x2dt 0x5ft
    .end array-data
.end method

.method public constructor <init>(Z)V
    .registers 2

    invoke-direct {p0}, Ltestdata/b64impl/Base64Impl$Coder;-><init>()V
    iput-boolean p1, p0, Ltestdata/b64impl/Base64Impl$Encoder;->useUrlSafe:Z
    return-void
.end method


# virtual methods
.method public encode([B)[B
    .registers 2

    const/4 v0, 0x0
    return-object v0
.end method
