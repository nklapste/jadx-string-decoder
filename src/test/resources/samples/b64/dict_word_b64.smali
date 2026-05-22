.class LDictWordB64;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 2
    sget-object p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    # "callback" — all lowercase, single dictionary word, valid standard Base64;
    # decodes to 83% printable (fails 90% default but passes 75% lowered threshold)
    const-string v0, "callback"
    invoke-virtual {p0, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
