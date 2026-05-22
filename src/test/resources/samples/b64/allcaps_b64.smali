.class LAllCapsB64;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 2
    sget-object p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    # "CURSOR" — all uppercase, valid standard Base64; decodes to "\tDR9" but should be skipped by skipSnakeCase
    const-string v0, "CURSOR"
    invoke-virtual {p0, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
