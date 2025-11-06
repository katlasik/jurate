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
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost



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

lazy val core = (project in file("core"))
  .settings(
    name := "jurate",
    libraryDependencies ++= Dependencies.Testing,
    publishTo := sonatypePublishToBundle.value,
    publishMavenStyle := true,
    publish / skip := false
  )

lazy val examples = (project in file("examples"))
  .dependsOn(core)
  .settings(
    name := "examples",
    libraryDependencies ++= Dependencies.Examples,
    publish / skip := true
  )

lazy val integrationTests = (project in file("integration-tests"))
  .dependsOn(core)
  .settings(
    name := "integration-tests",
    libraryDependencies ++= Dependencies.Testing,
    publish / skip := true
  )


lazy val docs = (project in file("docs"))
  .enablePlugins(MdocPlugin)
  .dependsOn(core, examples, integrationTests)
  .settings(
    mdocIn := file("README.template.md"),
    mdocOut := file("README.md"),
    publish / skip := true
  )


lazy val root = (project in file("."))
  .aggregate(core, examples, integrationTests, docs)
  .settings(
    name := "root",
    publish / skip := true
  )