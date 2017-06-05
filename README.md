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

Usage
-----

```
usage: jardiff [-c] [-g <dir>] [-h] [-q] [-U <n>] <jar/directory/url> [<jar/directory/url> ...]
 -c,--suppress-code   Suppress method bodies
 -g,--git <dir>       Directory to output a git repository containing the diff
 -h,--help            Display this message
 -q,--quiet           Don't output diffs to standard out
 -U,----unified <n>   Number of context lines in diff
```


Invoking
--------

Clone this project and use `sbt` to execute:

```
% sbt "core/run /tmp/out1.jar /tmp/out2.jar"

% sbt "core/run scala.tools.jardiff.Main --git-repo /tmp/diff-repo /tmp/out1 /tmp/out2"
```

We plan to publish a binary release soon to make this more convenient.

Sample Output
-------------

https://github.com/retronym/scala-library-diff/commits/master
