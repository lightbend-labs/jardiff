val buildName = "jardiff"

inThisBuild(Seq[Setting[_]](
  version := "1.0-SNAPSHOT",
  organization := "org.scala-lang",
  scalaVersion := "2.12.2",
  licenses := List(("Scala license", url("https://github.com/scala/jardiff/blob/master/LICENSE"))),
  homepage := Some(url("http://github.com/scala/jardiff")),
  scalacOptions := Seq("-feature", "-deprecation", "-Xlint")
))

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
  aggregate(core)
  settings(
    name := buildName,
    publish := (),
    publishLocal := ()
  )
)

lazy val core = (
  project.
  settings(libraryDependencies ++= Seq(
    "commons-cli" % "commons-cli" % "1.4",
    "org.scala-lang.modules" % "scala-asm" % "5.1.0-scala-2",
    "org.scala-lang" % "scalap" % System.getProperty("scalap.version", scalaVersion.value),
    "org.eclipse.jgit" % "org.eclipse.jgit" % "4.6.0.201612231935-r",
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.25", // for any java classes looking for this
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  ),
  name := buildName + "-core")
  settings(sonatypePublishSettings:_*)
)

