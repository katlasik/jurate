ThisBuild / scalaVersion := "3.6.2"

libraryDependencies ++= Dependencies.All

lazy val IntegrationTest: Configuration =
  config("integration") extend Test

lazy val Examples: Configuration =
  config("examples") extend Compile

ThisBuild / scalacOptions ++= Seq(
  "-Wunused:all",      // Enable all unused warnings
  "-Wvalue-discard",   // Warn on unused expression results
  "-deprecation",      // Warn about deprecated APIs
  "-feature"           // Warn about advanced language features
)

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