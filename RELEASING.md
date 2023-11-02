## Core JAR

* [ ] create and tag the release via the GitHub web UI
* [ ] check the GitHub Actions log to see that publishing succeeded
* [ ] wait for the artifact to appear on Maven Central

## CLI jar

* [ ] locally, on JDK 8, run `sbt cli/assembly`
* [ ] rename the resulting JAR to just `jardiff.jar`
* [ ] attach the JAR to the release in the GitHub web UI
* [ ] PR the change to the [homebrew formula](https://github.com/retronym/homebrew-formulas/blob/master/jardiff.rb), using `shasum -a 256 jardiff.jar` to compute the SHA
