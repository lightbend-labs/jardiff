val buildName = "jardiff"

val scala212Version = "2.12.19"
val scala213Version = "2.13.13"

inThisBuild(Seq[Setting[_]](
  organization := "com.lightbend",
  scalaVersion := "2.13.13",
  startYear := Some(2017),
  organizationName := "Lightbend Inc. <https://www.lightbend.com>",
  licenses := List(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))),
  homepage := Some(url("https://github.com/lightbend-labs/jardiff")),
  scmInfo := Some(ScmInfo(url("https://github.com/lightbend-labs/jardiff"), "scm:git:git@github.com:lightbend-labs/jardiff.git")),
  developers := List(
    Developer("retronym", "Jason Zaugg", "@retronym", url("https://github.com/retronym")),
  ),
  scalacOptions := Seq("-feature", "-deprecation", "-Xlint", "-Werror"),
  // since April 2024 or so, macos-latest gives us a machine that doesn't
  // have sbt installed (actions/setup-java#627). if we weren't using
  // sbt-github-actions it would be easy to just edit the workflow directly to
  // use `brew install sbt`, but it's easier to just stay on macos-12 for as
  // long as it remains available. I've reported the issue upstream at
  // sbt/sbt-github-actions#185 . if we do move
  // to macos-latest, note that it doesn't have temurin@8 anymore (as per
  // actions/setup-java#625), so we'd also need to change that entry (a few
  // lines below) from "temurin" to "zulu"
  githubWorkflowOSes := Seq("ubuntu-latest", "macos-12"),
  githubWorkflowJavaVersions := Seq(
    JavaSpec.temurin("8"),
    JavaSpec.temurin("11"),
    JavaSpec.temurin("17"),
    JavaSpec.temurin("21"),
    JavaSpec.temurin("22"),
  ),
  githubWorkflowTargetTags ++= Seq ("v*"),
  githubWorkflowPublishTargetBranches :=  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("main"))
  ),
  githubWorkflowPublish := Seq (
    WorkflowStep.Sbt(
      commands = List("ci-release"),
      name = Some("Publish project"),
      env = Map(
        "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
      )
    )
  ),
  headerLicense := Some(HeaderLicense.Custom("Copyright (C) Lightbend Inc. <https://www.lightbend.com>")),
))

ThisBuild / pomIncludeRepository   := (_ => false)

lazy val root = (
  project.in(file("."))
  aggregate(core, cli)
  settings(
    name := buildName,
    publish / skip := true,
    // See https://github.com/sbt/sbt/issues/4262#issuecomment-405607763
    crossScalaVersions := Seq.empty
  )
)

val AsmVersion = "9.7"

lazy val core = project.
  settings(
    libraryDependencies ++= Seq(
      "org.ow2.asm" % "asm" % AsmVersion,
      "org.ow2.asm" % "asm-util" % AsmVersion,
      "org.scala-lang" % "scalap" % System.getProperty("scalap.version", scalaVersion.value),
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.2.202306221912-r",
      "org.slf4j" % "slf4j-api" % "2.0.13",
      "org.slf4j" % "log4j-over-slf4j" % "2.0.13", // for any java classes looking for this
      "ch.qos.logback" % "logback-classic" % "1.3.11",
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    ),
    name := buildName + "-core",
    crossScalaVersions := Seq(scala212Version, scala213Version),
    scalaVersion := scala212Version,
    scalacOptions ++= {
      // We publish the core library into maven which is done in CI via ci-release
      // so we should only enable the optimizer in CI
      if (insideCI.value) {
        val log = sLog.value
        log.info("Running in CI, enabling Scala2 optimizer <sources> mode for core")
        Seq(
          "-opt-inline-from:<sources>",
          "-opt:l:inline"
        )
      } else Nil
    }
  )

lazy val cli = project.
  settings(
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % "1.8.0",
    ),
    name := buildName + "-cli",
    assembly / assemblyMergeStrategy := {
      case "rootdoc.txt" => MergeStrategy.discard
      case x if x.endsWith("module-info.class") => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value(x)
    },
    // Having Scala 2.13 here in crossScalaVersions is redundant but due to how
    // sbt-github-actions generates the sbt test command (i.e. sbt '++ 2.13.12' test),
    // sbt's update task cannot handle projects with different crossScalaVersions well
    crossScalaVersions := Seq(scala212Version, scala213Version),
    scalaVersion := scala212Version,
    // cli is not meant to be published
    publish / skip := true,
    // We are creating a fatjar here for distribution so we can do global optimization
    // using scala.** while omitting the JDK stdlib since we don't know what JDK version
    // the user will run on. If we implement a way to make the cli release in CI then we
    // can also use insideCI
    scalacOptions ++= Seq(
      "-opt-inline-from:scala.**",
      "-opt:l:inline"
    ),
    assembly := {
      // The Scala 2 optimizer can cause issues if the codebase is not compiled in a clean
      // state so lets make sure that we force clean before assembling the cli jar
      val _ = clean.value
      assembly.value
    }
  ).dependsOn(core)
