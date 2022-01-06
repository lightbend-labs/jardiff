inThisBuild(Seq[Setting[_]](
  scalaVersion := "2.13.1",
  organization := "org.example"
))

lazy val original = (project in file("empty"))
  .settings(
    name := "exampleproject",
    version := "0.1.0"
  )

lazy val changed = (project in file("changed"))
  .settings(
    name := "exampleproject",
    version := "0.2.0",
    jardiffPreviousVersion := "0.1.0",
  )