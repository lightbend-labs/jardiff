import sbt.Keys._
import sbt.ScriptedPlugin._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin.{AssemblyKeys, MergeStrategy, assemblySettings}


val buildName = "jardiff"
val buildOrganization = "org.scala-lang"

val commonSettings = Defaults.coreDefaultSettings ++ Seq (
    organization := buildOrganization,
    scalaVersion := "2.12.2",
    git.gitTagToVersionNumber := { tag: String =>
      if(tag matches "[0.9]+\\..*") Some(tag)
      else None
    },
    git.useGitDescribe := true,
    licenses := List(("Scala license", "https://github.com/scala/jardiff/blob/master/LICENSE")),
    homepage := Some(url("http://github.com/scala/jardiff")),
    scalacOptions := Seq("-feature", "-deprecation", "-Xlint")
)

def sbtPublishSettings: Seq[Def.Setting[_]] = Seq(
  bintrayOrganization := Some("typesafe"),
  bintrayRepository := "sbt-plugins",
  bintrayReleaseOnPublish := false
)

def sonatypePublishSettings: Seq[Def.Setting[_]] = Seq(
  // If we want on maven central, we need to be in maven style.
  publishMavenStyle := true,
  publishArtifact in Test := false,
  // The Nexus repo we're publishing to.
  publishTo := Some(
    if (isSnapshot.value) "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else                  "releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  ),
  // Maven central cannot allow other repos.  We're ok here because the artifacts we
  // we use externally are *optional* dependencies.
  pomIncludeRepository := { x => false },
  // Maven central wants some extra metadata to keep things 'clean'.
  pomExtra := (
    <scm>
      <url>git@github.com:scala/jardiff.git</url>
      <connection>scm:git:git@github.com:scala/jardiff.git</connection>
    </scm>
    <developers>
      <developer>
        <id>retronym</id>
        <name>Jason Zaugg</name>
      </developer>
    </developers>)
)


lazy val root = (
  project.in(file("."))
  aggregate(core, sbtplugin)
  settings(name := buildName,
           publish := (),
           publishLocal := ()
           )
  enablePlugins(GitVersioning)
)

lazy val core = (
  project.
  settings(commonSettings ++ buildInfoSettings)
  settings(libraryDependencies ++= Seq(
    "commons-cli" % "commons-cli" % "1.4",
    "org.scala-lang.modules" % "scala-asm" % "5.1.0-scala-2",
    "org.scala-lang" % "scalap" % System.getProperty("scalap.version", scalaVersion.value),
    "org.eclipse.jgit" % "org.eclipse.jgit" % "4.6.0.201612231935-r",
    "org.slf4j" % "slf4j-api" % "1.7.24",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.24", // for any java classes looking for this
    "ch.qos.logback" % "logback-classic" % "1.2.1"
  ),
           name := buildName + "-core")
  settings(sonatypePublishSettings:_*)
)

val myAssemblySettings: Seq[Setting[_]] = (assemblySettings: Seq[Setting[_]]) ++ Seq(
  mergeStrategy in assembly ~= (old => {
    case "LICENSE" => MergeStrategy.first
    case x         => old(x)
  }),
  AssemblyKeys.excludedFiles in assembly ~= (old =>
    // Hack to keep LICENSE files.
    { files: Seq[File] => old(files) filterNot (_.getName contains "LICENSE") }
  )
)

lazy val sbtplugin = (
  Project("sbtplugin", file("sbtplugin"), settings = commonSettings)
  settings(scriptedSettings)
  settings(name := "sbt-jardiff-plugin",
           sbtPlugin := true,
           scriptedLaunchOpts := scriptedLaunchOpts.value :+ "-Dplugin.version=" + version.value,
           scriptedBufferLog := false,
           // Scripted locally publishes sbt plugin and then runs test projects with locally published version.
           // Therefore we also need to locally publish dependent projects on scripted test run.
           scripted := (scripted dependsOn (publishLocal in core)).evaluated)
  dependsOn(core)
  settings(sbtPublishSettings:_*)
)
