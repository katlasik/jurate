package jurate

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues

import scala.sys.process.Process

class IntegrationSpec extends AnyFlatSpec with Matchers with EitherValues {

  it should "load configuration from environment variables" in {

    //given
    val logger = StringLogger()

    //when
    val process = Process(
      """sbt "integration:runMain jurate.app"""",
      None,
      "DB_HOST" -> "localhost",
      "DB_PORT" -> "5432",
      "DB_USER" -> "user",
      "DB_PASSWORD" -> "pass",
      "PORT" -> "5432",
      "HOST" -> "myhost.priv"
    )
    val result = process.run(logger)

    //then
    result.exitValue() should be(0)
    logger.getOutput should include("Config(5432,myhost.priv,DbConfig(localhost,5432,user,*****))")

  }

  it should "load configuration from system properties" in {

    //given
    val logger = StringLogger()

    //when
    val props = Map(
      "db.host" -> "localhost",
      "db.port" -> "5432",
      "db.user" -> "user",
      "db.password" -> "pass",
      "http.port" -> "5432",
      "http.host" -> "myhost.priv"
    ).map {
      case (key, value) => s""""-D$key=$value""""
    }.mkString(",")

    val command = s"""sbt 'set javaOptions ++= Seq($props)' "integration:runMain jurate.app""""
    val process = Process(command, None)

    val result = process.run(logger)

    //then
    result.exitValue() should be(0)
    logger.getOutput should include("Config(5432,myhost.priv,DbConfig(localhost,5432,user,*****))")

  }

  it should "fail if unable to load configuration" in {

    //given
    val logger = StringLogger()

    //when
    val process = Process(
      """sbt "integration:runMain jurate.app"""",
      None
    )
    val result = process.run(logger)

    //then
    result.exitValue() should be(1)
    logger.getOutput should include("Missing environment variable PORT, missing system property http.port")
    logger.getOutput should include("Missing environment variable HOST, missing system property http.host")
    logger.getOutput should include("Missing environment variable DB_HOST, missing system property db.host")
    logger.getOutput should include("Missing environment variable DB_PORT, missing system property db.port")
    logger.getOutput should include("Missing environment variable DB_USER, missing system property db.user")
    logger.getOutput should include("Missing environment variable DB_PASSWORD, missing system property db.password")


  }

}
