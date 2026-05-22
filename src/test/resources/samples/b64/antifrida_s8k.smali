.class public LAntifrida;
.super Ljava/lang/Object;

# Minimal test fixture for antifridas8kTest.
# Models a filled-new-array/range of 9 Base64-encoded strings (common anti-frida/xposed library names).
# Padded strings (frida, libAndHook, liblsposed) anchor contextual decoding of unpadded siblings.

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static a()[Ljava/lang/String;
    .registers 9

    const-string v0, "eHBvc2Vk"

    const-string v1, "ZnJpZGE="

    const-string v2, "Z3Vt"

    const-string v3, "bGluamVjdG9y"

    const-string v4, "bWFnaXNr"

    const-string v5, "c3Vic3RyYXRl"

    const-string v6, "Z2Ric2VydmVy"

    const-string v7, "bGliQW5kSG9vaw=="

    const-string v8, "bGlibHNwb3NlZA=="

    filled-new-array/range {v0 .. v8}, [Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method
