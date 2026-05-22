.class public Ls8/k;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static a()Ljava/lang/String;
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

    .line 1
    filled-new-array/range {v0 .. v8}, [Ljava/lang/String;

    move-result-object v0

    .line 2
    new-instance v1, Ljava/util/ArrayList;

    invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V

    const/4 v2, 0x0

    const/4 v3, 0x1

    :try_start_1d
    const-string v4, "L3Byb2Mvc2VsZi9tYXBz"

    .line 3
    invoke-static {v4}, Ls8/t;->b(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    .line 4
    new-instance v5, Ljava/io/BufferedReader;

    new-instance v6, Ljava/io/InputStreamReader;

    new-instance v7, Ljava/io/FileInputStream;

    invoke-direct {v7, v4}, Ljava/io/FileInputStream;-><init>(Ljava/lang/String;)V

    invoke-direct {v6, v7}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V

    invoke-direct {v5, v6}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V

    .line 5
    :cond_32
    :goto_32
    invoke-virtual {v5}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v4

    if-eqz v4, :cond_4f

    const/16 v6, 0x20

    .line 6
    invoke-virtual {v4, v6}, Ljava/lang/String;->lastIndexOf(I)I

    move-result v6

    add-int/2addr v6, v3

    invoke-virtual {v4, v6}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v4

    .line 7
    invoke-virtual {v4}, Ljava/lang/String;->length()I

    move-result v6

    if-lez v6, :cond_32

    .line 8
    sget-object v6, Ls8/t;->a:Ljava/util/Random;

    .line 9
    invoke-virtual {v1, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_32

    .line 10
    :cond_4f
    invoke-virtual {v5}, Ljava/io/BufferedReader;->close()V
    :try_end_52
    .catch Ljava/io/IOException; {:try_start_1d .. :try_end_52} :catch_53

    goto :goto_56

    .line 11
    :catch_53
    sget-object v1, Ls8/t;->a:Ljava/util/Random;

    move-object v1, v2

    :goto_56
    if-nez v1, :cond_59

    return-object v2

    .line 12
    :cond_59
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const/4 v4, 0x0

    move v5, v4

    :goto_60
    const/16 v6, 0x9

    if-ge v5, v6, :cond_9d

    .line 13
    aget-object v6, v0, v5

    .line 14
    invoke-static {v6}, Ls8/t;->b(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    move-result-object v6

    .line 15
    invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v7

    :cond_72
    invoke-interface {v7}, Ljava/util/Iterator;->hasNext()Z

    move-result v8

    if-eqz v8, :cond_8c

    invoke-interface {v7}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v8

    check-cast v8, Ljava/lang/String;

    .line 16
    invoke-virtual {v8}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v8, v6}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v8

    if-eqz v8, :cond_72

    .line 17
    sget-object v6, Ls8/t;->a:Ljava/util/Random;

    move v6, v3

    goto :goto_8d

    :cond_8c
    move v6, v4

    :goto_8d
    if-eqz v6, :cond_95

    const-string v6, "1"

    .line 18
    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_9a

    :cond_95
    const-string v6, "0"

    .line 19
    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    :goto_9a
    add-int/lit8 v5, v5, 0x1

    goto :goto_60

    .line 20
    :cond_9d
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method


# virtual methods
.method public final b()V
    .registers 4

    .line 1
    new-instance v0, Ljava/util/Random;

    invoke-direct {v0}, Ljava/util/Random;-><init>()V

    const/4 v1, 0x7

    invoke-virtual {v0, v1}, Ljava/util/Random;->nextInt(I)I

    move-result v0

    .line 2
    sget-object v1, Ls8/t;->a:Ljava/util/Random;

    if-eqz v0, :cond_46

    const/4 v1, 0x1

    if-eq v0, v1, :cond_3d

    const/4 v1, 0x2

    const-string v2, ""

    if-eq v0, v1, :cond_37

    const/4 v1, 0x3

    if-eq v0, v1, :cond_31

    const/4 v1, 0x4

    if-eq v0, v1, :cond_2b

    const/4 v1, 0x5

    if-eq v0, v1, :cond_25

    .line 3
    new-instance v0, Ljava/lang/IllegalArgumentException;

    invoke-direct {v0, v2}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 4
    :cond_25
    new-instance v0, Ljava/lang/ArrayStoreException;

    invoke-direct {v0, v2}, Ljava/lang/ArrayStoreException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 5
    :cond_2b
    new-instance v0, Ljava/lang/ArithmeticException;

    invoke-direct {v0, v2}, Ljava/lang/ArithmeticException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 6
    :cond_31
    new-instance v0, Ljava/lang/IndexOutOfBoundsException;

    invoke-direct {v0, v2}, Ljava/lang/IndexOutOfBoundsException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 7
    :cond_37
    new-instance v0, Ljava/lang/RuntimeException;

    invoke-direct {v0, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 8
    :cond_3d
    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;

    move-result-object v0

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Ljava/lang/Runtime;->halt(I)V

    goto :goto_4d

    .line 9
    :cond_46
    invoke-static {}, Landroid/os/Process;->myPid()I

    move-result v0

    invoke-static {v0}, Landroid/os/Process;->killProcess(I)V

    :goto_4d
    return-void
.end method

.method public run()V
    .registers 6

    .line 1
    new-instance v0, Ljava/util/Random;

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v1

    invoke-direct {v0, v1, v2}, Ljava/util/Random;-><init>(J)V

    .line 2
    :goto_9
    sget-object v1, Ls8/t;->a:Ljava/util/Random;

    .line 3
    invoke-static {}, Landroid/os/Debug;->waitingForDebugger()Z

    move-result v1

    if-nez v1, :cond_17

    invoke-static {}, Landroid/os/Debug;->isDebuggerConnected()Z

    move-result v1

    if-eqz v1, :cond_1a

    .line 4
    :cond_17
    invoke-virtual {p0}, Ls8/k;->b()V

    :cond_1a
    const/4 v1, 0x0

    :try_start_1b
    const-string v2, "L3Byb2Mvc2VsZi9zdGF0dXM="

    .line 5
    invoke-static {v2}, Ls8/t;->b(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    .line 6
    new-instance v3, Ljava/io/BufferedReader;

    new-instance v4, Ljava/io/FileReader;

    invoke-direct {v4, v2}, Ljava/io/FileReader;-><init>(Ljava/lang/String;)V

    invoke-direct {v3, v4}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V

    .line 7
    :cond_2b
    invoke-virtual {v3}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_54

    const-string v4, "VHJhY2VyUGlkOg=="

    .line 8
    invoke-static {v4}, Ls8/t;->b(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    .line 9
    invoke-virtual {v2, v4}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v4

    if-eqz v4, :cond_2b

    const-string v3, ":"

    .line 10
    invoke-virtual {v2, v3}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v2

    const/4 v3, 0x1

    aget-object v2, v2, v3

    invoke-virtual {v2}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v2
    :try_end_4e
    .catch Ljava/io/IOException; {:try_start_1b .. :try_end_4e} :catch_52

    if-eqz v2, :cond_54

    move v1, v3

    goto :goto_54

    .line 11
    :catch_52
    sget-object v2, Ls8/t;->a:Ljava/util/Random;

    :cond_54
    :goto_54
    if-eqz v1, :cond_5b

    .line 12
    sget-object v1, Ls8/t;->a:Ljava/util/Random;

    .line 13
    invoke-virtual {p0}, Ls8/k;->b()V

    :cond_5b
    const/4 v1, 0x5

    .line 14
    :try_start_5c
    invoke-virtual {v0, v1}, Ljava/util/Random;->nextInt(I)I

    move-result v2

    add-int/2addr v2, v1

    mul-int/lit16 v2, v2, 0x3e8

    int-to-long v1, v2

    invoke-static {v1, v2}, Ljava/lang/Thread;->sleep(J)V
    :try_end_67
    .catch Ljava/lang/InterruptedException; {:try_start_5c .. :try_end_67} :catch_68

    goto :goto_9

    .line 15
    :catch_68
    sget-object v1, Ls8/t;->a:Ljava/util/Random;

    goto :goto_9
.end method
