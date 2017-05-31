JAR diff for Scala
==================

A tool for generating bytecode diffs
====================================

JarDiff is a tool for generating detailed but comprehensible diffs of sets of Java classfiles.

Usage Examples
--------------

```
% sbt runMain scala.tools.jardiff.Main /tmp/out1.jar /tmp/out2.jar

% sbt runMain scala.tools.jardiff.Main --git-repo /tmp/diff-repo /tmp/out1 /tmp/out2
```

License
-------
Copyright 2017 Lightbend, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
