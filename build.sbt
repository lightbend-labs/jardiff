val buildName = "jardiff"

inThisBuild(Seq[Setting[_]](
  version := "1.0-SNAPSHOT",
  organization := "org.scala-lang",
  scalaVersion := "2.13.12",
  startYear := Some(2017),
  organizationName := "Lightbend Inc. <https://www.lightbend.com>",
  licenses := List(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))),
  homepage := Some(url("https://github.com/lightbend-labs/jardiff")),
  scmInfo := Some(ScmInfo(url("https://github.com/lightbend-labs/jardiff"), "scm:git:git@github.com:lightbend-labs/jardiff.git")),
  developers := List(
    Developer("retronym", "Jason Zaugg", "@retronym", url("https://github.com/retronym")),
  ),
  scalacOptions := Seq("-feature", "-deprecation", "-Xlint", "-Werror")
))

lazy val root = (
  project.in(file("."))
  aggregate(core)
  settings(
    name := buildName,
    publish / skip := true,
  )
)

val AsmVersion = "9.6"

lazy val core = (
  project.
  settings(
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % "1.5.0",
      "org.ow2.asm" % "asm" % AsmVersion,
      "org.ow2.asm" % "asm-util" % AsmVersion,
      "org.scala-lang" % "scalap" % System.getProperty("scalap.version", scalaVersion.value),
      "org.eclipse.jgit" % "org.eclipse.jgit" % "6.6.1.202309021850-r",
      "org.slf4j" % "slf4j-api" % "2.0.7",
      "org.slf4j" % "log4j-over-slf4j" % "2.0.7", // for any java classes looking for this
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
    ),
    name := buildName + "-core",
    headerLicense := Some(HeaderLicense.Custom("Copyright (C) Lightbend Inc. <https://www.lightbend.com>")),
    assembly / assemblyMergeStrategy := {
      case "rootdoc.txt" => MergeStrategy.discard
      case x if x.endsWith("module-info.class") => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value(x)
    },
  )
)
