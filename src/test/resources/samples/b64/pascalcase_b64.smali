.class LPascalCaseB64;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 2
    sget-object p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    # "FileUtil" — standard PascalCase Java type name, valid Base64 charset
    const-string v0, "FileUtil"
    invoke-virtual {p0, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
