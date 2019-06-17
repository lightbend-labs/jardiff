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

Installing
----------

### macOS

`macOS` users may install with:

```
brew install retronym/formulas/jardiff
```

### Other Platforms

 - Download `jardiff.jar` from https://github.com/scala/jardiff/releases/latest
 - Wrap in a shell function or script to run `java -jar jardiff.jar "$@"`

Usage
----------

```
% jardiff -h
usage: jardiff [-c] [-g <dir>] [-h] [-i <arg>] [-p] [-q] [-r] [-U <n>] VERSION1 [VERSION2 ...]

Each VERSION may designate a single file, a directory, JAR file or a
`:`-delimited classpath

 -c,--suppress-code       Suppress method bodies
 -g,--git <dir>           Directory to output a git repository containing the
                          diff
 -h,--help                Display this message
 -i,--ignore <arg>        File pattern to ignore rendered files in gitignore
                          format
 -p,--suppress-privates   Display only non-private members
 -q,--quiet               Don't output diffs to standard out
 -r,--raw                 Disable sorting and filtering of classfile contents
 -U,--unified <n>         Number of context lines in diff

% jardiff dir1 dir2

% jardiff v1/A.class v2/A.class

% jardiff --git /tmp/diff-repo --quiet v1.jar v2.jar v3.jar
```

Building
--------

After cloning this project, use `sbt clean core/assembly`.

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
```
```diff
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

### API changes visible in bytecode descriptors / generic signature / scalap output

```scala
// V1.scala
trait C {
  def m1(a: String)
  def m2(a: Option[String])
  def m3(a: String)
}
```
```scala
// V2.scala
trait C {
  def m1(a: AnyRef)
  def m2(a: Option[AnyRef])
  def m3(a: scala.collection.immutable.StringOps) // value class
}
```

```diff
diff --git a/C.class.asm b/C.class.asm
index 52a43d5..bdb0541 100644
--- a/C.class.asm
+++ b/C.class.asm
@@ -4,12 +4,12 @@
 
 
   // access flags 0x401
-  public abstract m1(Ljava/lang/String;)V
+  public abstract m1(Ljava/lang/Object;)V
     // parameter final  a
 
   // access flags 0x401
-  // signature (Lscala/Option<Ljava/lang/String;>;)V
-  // declaration: void m2(scala.Option<java.lang.String>)
+  // signature (Lscala/Option<Ljava/lang/Object;>;)V
+  // declaration: void m2(scala.Option<java.lang.Object>)
   public abstract m2(Lscala/Option;)V
     // parameter final  a
 
diff --git a/C.class.scalap b/C.class.scalap
index 78f7d91..637cd98 100644
--- a/C.class.scalap
+++ b/C.class.scalap
@@ -1,5 +1,5 @@
 trait C extends scala.AnyRef {
-  def m1(a: scala.Predef.String): scala.Unit
-  def m2(a: scala.Option[scala.Predef.String]): scala.Unit
-  def m3(a: scala.Predef.String): scala.Unit
+  def m1(a: scala.AnyRef): scala.Unit
+  def m2(a: scala.Option[scala.AnyRef]): scala.Unit
+  def m3(a: scala.collection.immutable.StringOps): scala.Unit
 }
```

### Validating a Scala compiler nightly

```
% git clone --quiet --depth 1 akka/akka; cd akka

% cat ~/.sbt/0.13/resolver.sbt 
resolvers ++= (
  if (scalaVersion.value.contains("-bin"))
     List("scala-integration" at "https://scala-ci.typesafe.com/artifactory/scala-integration/")
  else Nil
)

% for v in 2.12.2 2.12.3-bin-bd6294d; do \
  echo $v; \
  mkdir -p /tmp/akka-$v; \
  time sbt ++$v clean package &>/dev/null; \
  for f in $(find . -name '*.jar'); do cp $f /tmp/akka-$v/; done; \
done

2.12.2

real	3m19.468s
user	8m56.947s
sys	0m17.343s
2.12.3-bin-bd6294d

real	2m57.631s
user	8m40.168s
sys	0m16.172s

% jardiff -q --git /tmp/akka-diff $(find /tmp/akka-2.12.2 | paste -s -d : -) $(find /tmp/akka-2.12.3-bin-bd6294d | paste -s -d : -)
```


The resulting diff shows that the only change in the generated bytecode is due
to [scala/scala#5857](https://github.com/scala/scala/pull/5857), "Fix lambda deserialization in classes with 252+ lambdas"

```diff
% git --git-dir /tmp/akka-diff/.git log -p -1
commit 83f0e0a5dabbdf1b7677363bf238d81e4c5b032e (HEAD -> master)
Author: Jason Zaugg <jzaugg@gmail.com>
Date:   Mon Jun 5 17:08:17 2017 +1000

    jardiff textified output of: /tmp/akka-2.12.3-bin-bd6294d:/tmp/akka-2.12.3-bin-bd6294d/akka-2.5-SNAPSHOT.jar:...

diff --git a/akka/stream/scaladsl/GraphApply.class.asm b/akka/stream/scaladsl/GraphApply.class.asm
index 5672560..4d1fafb 100644
--- a/akka/stream/scaladsl/GraphApply.class.asm
+++ b/akka/stream/scaladsl/GraphApply.class.asm
@@ -2802,6 +2802,8 @@ public abstract interface akka/stream/scaladsl/GraphApply {
 
   // access flags 0x100A
   private static synthetic $deserializeLambda$(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;
+    TRYCATCHBLOCK L0 L1 L1 java/lang/IllegalArgumentException
+   L0
     ALOAD 0
     INVOKEDYNAMIC lambdaDeserialize(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object; [
       // handle kind 0x6 : INVOKESTATIC
@@ -3308,12 +3310,20 @@ public abstract interface akka/stream/scaladsl/GraphApply {
       // handle kind 0x6 : INVOKESTATIC
       akka/stream/scaladsl/GraphApply.$anonfun$create$250(Lscala/Function1;Ljava/lang/Object;)Ljava/lang/Object;, 
       // handle kind 0x6 : INVOKESTATIC
-      akka/stream/scaladsl/GraphApply.$anonfun$create$251(Lscala/Function1;Ljava/lang/Object;)Ljava/lang/Object;, 
+      akka/stream/scaladsl/GraphApply.$anonfun$create$251(Lscala/Function1;Ljava/lang/Object;)Ljava/lang/Object;
+    ]
+    ARETURN
+   L1
+    ALOAD 0
+    INVOKEDYNAMIC lambdaDeserialize(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object; [
+      // handle kind 0x6 : INVOKESTATIC
+      scala/runtime/LambdaDeserialize.bootstrap(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/CallSite;
+      // arguments:
       // handle kind 0x6 : INVOKESTATIC
       akka/stream/scaladsl/GraphApply.$anonfun$create$252(Lscala/Function1;Ljava/lang/Object;)Ljava/lang/Object;
     ]
     ARETURN
-    MAXSTACK = 1
+    MAXSTACK = 2
     MAXLOCALS = 1
 
   // access flags 0x9
```
