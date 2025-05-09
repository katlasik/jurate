import sbt.*

object Dependencies {

  val Magnolia = Seq(
    "com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.16"
  )

  val Testing = Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % "test"
  )

  val All = Seq(
    Magnolia,
    Testing
  ).flatten.map(_ withJavadoc () withSources ())

}
