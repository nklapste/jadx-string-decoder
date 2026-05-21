.class LVarAssignB64;
.super Ljava/lang/Object;

# v0 is used twice so it may be kept as an explicit variable in the decompiled output
.method public static main([Ljava/lang/String;)V
    .registers 2
    const-string v0, "SGVsbG8sIFdvcmxkIQ=="
    sget-object p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {p0, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    invoke-virtual {p0, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
