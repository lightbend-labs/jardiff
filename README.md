JAR diff for Scala
==================

A tool for generating bytecode diffs
====================================

JarDiff is a tool for generating detailed but comprehensible diffs of sets (JAR or directory) of Java
classfiles.

Class files are rendered with ASM's [`Textifier`](http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/util/Textifier.html)
and with `scalap`. Other files are rendered as-is.

The rendered files are commited into a Git repository, one commit per provided command line argument.

The diffs between these are rendered to standard out (unless `--quiet` is provided). If only a single
argument is provided, the initial commit is rendered.

By default, a temporary git repository is used and deleted on exit. Use `--git` to provide a custom
location for the repository that can be inspected after the tool has run.   

Building and Running
--------------------

After cloning this project. and use `sbt clean core/assembly`.

```
% cd /code/jardiff

% sbt clean core/assembly

% function jardiff() {   java $JAVA_OPTS -jar /code/jardiff/core/target/scala-2.12/jardiff-core-assembly-*.jar "$@"; }

% jardiff -h
usage: jardiff [-c] [-g <dir>] [-h] [-q] [-U <n>] VERSION1 [VERSION2 ...]

Each VERSION may designate a single file, a directory, JAR file or a
`:`-delimited classpath

 -c,--suppress-code   Suppress method bodies
 -g,--git <dir>       Directory to output a git repository containing the diff
 -h,--help            Display this message
 -q,--quiet           Don't output diffs to standard out
 -U,--unified <n>     Number of context lines in diff

% jardiff dir1 dir2

% jardiff v1/A.class v2/A.class

% jardiff --git-repo /tmp/diff-repo --quiet v1.jar v2.jar v3.jar
```

We plan to publish a binary release soon to make this more convenient.

Sample Output
-------------

### Scala 2.11 vs 2.12 trait encoding changes

```scala
// test.scala
trait T { def foo = 4 }
class C extends T
```

```
% ~/scala/2.11/bin/scalac -d /tmp/v1 test.scala && ~/scala/2.12/bin/scalac -d /tmp/v2 test.scala

% jardiff /tmp/v1 /tmp/v2
diff --git a/C.class.asm b/C.class.asm
index f3a33f1..33b9282 100644
--- a/C.class.asm
+++ b/C.class.asm
@@ -1,4 +1,4 @@
-// class version 50.0 (50)
+// class version 52.0 (52)
 // access flags 0x21
 public class C implements T  {
 
@@ -8,7 +8,7 @@
     ALOAD 0
     INVOKESPECIAL java/lang/Object.<init> ()V
     ALOAD 0
-    INVOKESTATIC T$class.$init$ (LT;)V
+    INVOKESTATIC T.$init$ (LT;)V
     RETURN
     MAXSTACK = 1
     MAXLOCALS = 1
@@ -16,7 +16,7 @@
   // access flags 0x1
   public foo()I
     ALOAD 0
-    INVOKESTATIC T$class.foo (LT;)I
+    INVOKESTATIC T.foo$ (LT;)I
     IRETURN
     MAXSTACK = 1
     MAXLOCALS = 1
diff --git a/T.class.asm b/T.class.asm
index 9180093..fcac19f 100644
--- a/T.class.asm
+++ b/T.class.asm
@@ -1,8 +1,28 @@
-// class version 50.0 (50)
+// class version 52.0 (52)
 // access flags 0x601
 public abstract interface T {
 
 
-  // access flags 0x401
-  public abstract foo()I
+  // access flags 0x9
+  public static $init$(LT;)V
+    // parameter final synthetic  $this
+    RETURN
+    MAXSTACK = 0
+    MAXLOCALS = 1
+
+  // access flags 0x1
+  public default foo()I
+    ICONST_4
+    IRETURN
+    MAXSTACK = 1
+    MAXLOCALS = 1
+
+  // access flags 0x1009
+  public static synthetic foo$(LT;)I
+    // parameter final synthetic  $this
+    ALOAD 0
+    INVOKESPECIAL T.foo ()I
+    IRETURN
+    MAXSTACK = 1
+    MAXLOCALS = 1
 }
```

### Scala standard library changes during 2.11 minor releases

```
% jardiff --quiet --git /tmp/scala-library-diff /Users/jz/scala/2.11.*/lib/scala-library.jar

```

Browsable Repo: [scala-library-diff](https://github.com/retronym/scala-library-diff/commits/master)
