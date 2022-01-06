sys.props.get("plugin.version") match {
  case Some(x) => {
    addSbtPlugin("org.scala-lang" % "sbt-jardiff" % x)
  }
  case _ => {
    addSbtPlugin("org.scala-lang" % "sbt-jardiff" % "1.3-SNAPSHOT")
  }
}