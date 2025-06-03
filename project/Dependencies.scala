import sbt.*

object Dependencies {

  val Testing = Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % "test"
  )

  val All = Seq(
    Testing
  ).flatten.map(_ withJavadoc () withSources ())

}
