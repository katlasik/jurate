ThisBuild / scalaVersion := "3.6.2"

libraryDependencies ++= Dependencies.All

lazy val IntegrationTest: Configuration =
  config("integration") extend Test

lazy val Examples: Configuration =
  config("examples") extend Compile

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .configs(Examples)
  .settings(
    inConfig(IntegrationTest)(
      Defaults.testSettings ++ Seq(
        fork := true
      )
    ),
    inConfig(Examples)(Defaults.compileSettings)
  )