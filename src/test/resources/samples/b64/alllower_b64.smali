.class LAllLowerB64;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 2
    sget-object p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    # "closed" — all lowercase, valid standard Base64; decodes to "rZ,y" but should be skipped by skipSnakeCase
    const-string v0, "closed"
    invoke-virtual {p0, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
