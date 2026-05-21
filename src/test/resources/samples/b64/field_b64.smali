.class LFieldB64;
.super Ljava/lang/Object;

.field public secret:Ljava/lang/String;

# Assigned in a regular method so the instruction stays in the method body (not extracted as a field initializer)
.method public setSecret()V
    .registers 2
    const-string v0, "SGVsbG8sIFdvcmxkIQ=="
    iput-object v0, p0, LFieldB64;->secret:Ljava/lang/String;
    return-void
.end method
