import sbt.*

object Dependencies {

  val Examples = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.11.33",
    "com.softwaremill.sttp.tapir" %% "tapir-netty-server-sync" % "1.11.33",
    "com.softwaremill.ox" %% "core" % "0.6.0",
  ).map(_ % "examples")

  val Testing = Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % "test"
  )

  val All = Seq(
    Testing,
    Examples
  ).flatten.map(_ withJavadoc () withSources ())

}
