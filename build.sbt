import xerial.sbt.Sonatype._

ThisBuild / scalaVersion := "3.6.2"

lazy val IntegrationTest: Configuration =
  config("integration") extend Test

lazy val Examples: Configuration =
  config("examples") extend Compile

ThisBuild / scalacOptions ++= Seq(
  "-Wunused:all", // Enable all unused warnings
  "-Wvalue-discard", // Warn on unused expression results
  "-deprecation", // Warn about deprecated APIs
  "-feature" // Warn about advanced language features
)

ThisBuild / developers := List(
  Developer(
    id = "katlasik",
    name = "Krzysztof Atłasik",
    email = "krzysztof.atlasik@protonmail.com",
    url = url("https://github.com/katlasik")
  )
)

ThisBuild / scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/katlasik/jurate"),
    connection = "scm:git:git@github.com:katlasik/jurate.git"
  )
)

ThisBuild / publishTo := sonatypePublishToBundle.value


lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .configs(Examples)
  .settings(
    organization := "com.github.katlasik",
    homepage := Some(url("https://github.com/katlasik/jurate")),
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    name := "jurate",
    libraryDependencies ++= Dependencies.All,
    inConfig(IntegrationTest)(
      Defaults.testSettings ++ Seq(
        fork := true
      )
    ),
    inConfig(Examples)(Defaults.compileSettings)
  )
