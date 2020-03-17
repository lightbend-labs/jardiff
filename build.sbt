val buildName = "jardiff"

inThisBuild(Seq[Setting[_]](
  version := "1.0-SNAPSHOT",
  organization := "org.scala-lang",
  scalaVersion := "2.13.0",
  startYear := Some(2017),
  organizationName := "Lightbend Inc. <https://www.lightbend.com>",
  licenses := List(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))),
  homepage := Some(url("http://github.com/scala/jardiff")),
  scmInfo := Some(ScmInfo(url("https://github.com/scala/jardiff"), "scm:git:git@github.com:scala/jardiff.git")),
  developers := List(
    Developer("retronym", "Jason Zaugg", "@retronym", url("https://github.com/retronym")),
  ),
  scalacOptions := Seq("-feature", "-deprecation", "-Xlint")
))

lazy val root = (
  project.in(file("."))
  aggregate(core)
  settings(
    name := buildName,
    skip in publish := true,
  )
)

lazy val core = (
  project.
  settings(
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % "1.4",
      "org.ow2.asm" % "asm" % AsmVersion,
      "org.ow2.asm" % "asm-util" % AsmVersion,
      "org.scala-lang" % "scalap" % System.getProperty("scalap.version", scalaVersion.value),
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.3.1.201904271842-r",
      "org.slf4j" % "slf4j-api" % "1.7.26",
      "org.slf4j" % "log4j-over-slf4j" % "1.7.26", // for any java classes looking for this
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ),
    name := buildName + "-core",
    headerLicense := Some(HeaderLicense.Custom("Copyright (C) Lightbend Inc. <https://www.lightbend.com>")),
    assemblyMergeStrategy in assembly := {
      case "module-info.class" => MergeStrategy.discard
      case "rootdoc.txt" => MergeStrategy.discard
      case x => (assemblyMergeStrategy in assembly).value(x)
    },
  )
)

val AsmVersion = "7.2"
