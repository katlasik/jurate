import sbt.url
import xerial.sbt.Sonatype.sonatypeCentralHost


ThisBuild / scalaVersion := "3.6.2"

ThisBuild / scalacOptions ++= Seq(
  "-Wunused:all", // Enable all unused warnings
  "-Wvalue-discard", // Warn on unused expression results
  "-deprecation", // Warn about deprecated APIs
  "-feature" // Warn about advanced language features
)

ThisBuild / versionScheme := Some("early-semver")

lazy val IntegrationTest: Configuration =
  config("integration") extend Test

lazy val Examples: Configuration =
  config("examples") extend Compile

inThisBuild(
  List(
    organization := "io.github.katlasik",
    homepage := Some(url("https://github.com/katlasik/jurate")),
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    developers := List(
      Developer(
        id = "katlasik",
        name = "Krzysztof Atlasik",
        email = "krzysztof.atlasik@pm.me",
        url = url("https://github.com/katlasik")
      )
    )
  )
)

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost


lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .configs(Examples)
  .settings(
    name := "jurate",
    libraryDependencies ++= Dependencies.All,
    publishTo := sonatypePublishToBundle.value,
    publishMavenStyle := true,
    inConfig(IntegrationTest)(
      Defaults.testSettings ++ Seq(
        fork := true
      )
    ),
    inConfig(Examples)(Defaults.compileSettings),
  )
