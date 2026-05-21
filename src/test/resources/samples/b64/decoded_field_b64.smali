.class public Ldecodedfield/HelloWorld;
.super Ljava/lang/Object;

.field public static final ADDRESS:Ljava/lang/String;

.method public static constructor <clinit>()V
    .registers 3

    new-instance v0, Ljava/lang/String;

    const-string v1, "cm9vbTovL2Nsb3VkLnRlbmNlbnQuY29tL3J0Yw=="

    const/4 v2, 0x0

    invoke-static {v1, v2}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B

    move-result-object v1

    invoke-direct {v0, v1}, Ljava/lang/String;-><init>([B)V

    sput-object v0, Ldecodedfield/HelloWorld;->ADDRESS:Ljava/lang/String;

    return-void
.end method

.method public static main([Ljava/lang/String;)V
    .registers 3

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    sget-object v1, Ldecodedfield/HelloWorld;->ADDRESS:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    return-void
.end method
