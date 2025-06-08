package jurata

import jurata.{*, given}
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
        derives ConfigValue

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
        derives ConfigValue

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
        derives ConfigValue

    // when
    val config = load[Config]

    // then
    config.value should be(Config(7777, "localhost"))
  }

  it should "fail to decode if value is not parseable" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "bad")

    case class Config(@env("PORT") port: Int) derives ConfigValue

    // when
    val config = load[Config]

    // then
    config.left.value should be(
      ConfigError.invalid("can't decode integer", "bad")
    )
  }

  it should "fail to compile if decoder is missing" in {
    //given
    class Foo

    //then
    """case class Config(@env("PORT") port: Foo) derives ConfigValue""" shouldNot compile
  }

  it should "fail to load if value is missing" in {

    // given
    given ConfigReader = ConfigReader.mocked

    case class Config(@env("PORT") port: Int) derives ConfigValue

    // when
    val config = load[Config]

    // then
    config.left.value should be(ConfigError.missing(List(env("PORT"))))
  }

  it should "load config class with secret" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onProp("password", "qwerty")

    case class Config(@prop("password") password: Secret[String])
        derives ConfigValue

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Secret("qwerty")))
    config.value.toString should be("Config(*****)")

  }

  it should "fail to load value if annotation is missing" in {

    // when
    given ConfigReader = ConfigReader.mocked

    case class Config(port: Int) derives ConfigValue

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
    ) derives ConfigValue

    // when
    val config = load[Config]

    // then
    config.left.value.getMessage.lines.toList should contain allOf (
      "Loaded invalid value: can't decode integer, received value: 'bad'",
      "Missing environment variable HOST, missing system property sys.host",
      "Missing environment variable SECRET"
    )
  }
}
