package jurate

import jurate.utils.FieldPath
import jurate.{*, given}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SimpleSpec extends AnyFlatSpec with Matchers with EitherValues {
  it should "load config class" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "8888")
      .onEnv("HOST", "localhost")

    case class Config(@env("PORT") port: Int, @env("HOST") host: String)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(8888, "localhost"))
  }

  it should "ignore default value if value in environment is available" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "8888")
      .onEnv("HOST", "localhost")

    case class Config(@env("PORT") port: Int = 7777, @env("HOST") host: String)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(8888, "localhost"))
  }

  it should "use default value if value in environment is NOT available" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onEnv("HOST", "localhost")

    case class Config(@env("PORT") port: Int = 7777, @env("HOST") host: String)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(7777, "localhost"))
  }

  it should "fail to decode if value is not parseable" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "bad")

    case class Config(@env("PORT") port: Int)

    // when
    val config = load[Config]

    // then
    config.left.value should be(
      ConfigError.invalid(
        FieldPath("port"),
        "can't decode integer",
        "bad",
        Some(env("PORT"))
      )
    )
  }

  it should "fail to compile if decoder is missing" in {

    """
      class Foo
      case class Config(@env("PORT") port: Foo) derives ConfigLoader
    """ shouldNot compile
  }

  it should "fail to load if value is missing" in {

    // given
    given ConfigReader = ConfigReader.mocked

    case class Config(@env("PORT") port: Int)

    // when
    val config = load[Config]

    // then
    config.left.value should be(
      ConfigError.missing(FieldPath("port"), List(env("PORT")))
    )
  }

  it should "load config class with secret" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onProp("password", "qwerty")

    case class Config(@prop("password") password: Secret[String])

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Secret("qwerty")))
    config.value.toString should be("Config(*****)")

  }

  it should "fail to load value if annotation is missing" in {

    // when
    given ConfigReader = ConfigReader.mocked

    case class Config(port: Int)

    // then
    val config = load[Config]

    config.left.value.getMessage.lines.toList should contain(
      "No annotations found for field: port"
    )

  }

  it should "aggregate errors" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "bad")

    case class Config(
        @env("PORT") port: Int,
        @env("HOST") @prop("sys.host")
        host: String,
        @env("SECRET") secret: Secret[String]
    )

    // when
    val config = load[Config]

    // then
    config.left.value.getMessage.lines.toList should contain allOf (
      "Invalid value received while reading environment variable PORT: can't decode integer, received value: 'bad'",
      "Missing environment variable HOST, missing system property sys.host",
      "Missing environment variable SECRET"
    )
  }

}
