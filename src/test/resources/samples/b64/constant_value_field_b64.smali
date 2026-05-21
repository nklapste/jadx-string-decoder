.class public LConstantValueFieldB64;
.super Ljava/lang/Object;

# Literal CONSTANT_VALUE — no <clinit>, value baked into the field declaration
.field public static final PREFIX:Ljava/lang/String; = "SGVsbG8sIFdvcmxkIQ=="

.method public usePrefix()Z
    .registers 3
    const-string v0, "SGVsbG8sIFdvcmxkIQ=="
    const-string v1, "Hello"
    invoke-virtual {v0, v1}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
    move-result v0
    return v0
.end method
