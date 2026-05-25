.class LPascalCaseDecodableB64;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 2
    sget-object p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    # "SiteWith" — PascalCase, valid Base64 charset, decodes to "J+^Z+a" (100% printable, 50% alnum)
    const-string v0, "SiteWith"
    invoke-virtual {p0, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
