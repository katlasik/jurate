package jurata

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues


class CaseClassSpec extends AnyFlatSpec with Matchers with EitherValues {

  it should "load nested class" in {

    //given
    case class HttpConfig(@env("PORT") port: Int, @env("HOST") host: String) derives ConfigValue
    case class Config(httpConfig: HttpConfig) derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "2000")
      .onEnv("HOST", "localhost")

    //when
    val config = load[Config]

    //then
    config.value should be(Config(HttpConfig(2000, "localhost")))
  }

  it should "fail to load nested class if values are missing" in {

    //given
    case class HttpConfig(@env("PORT") port: Int, @env("HOST") @prop("http.host") host: String)derives ConfigValue
    case class Config(httpConfig: HttpConfig)derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "2000")

    //when
    val config = load[Config]

    //then
    config.left.value should be(ConfigError.missing(IArray(env("HOST"), prop("http.host"))))
  }

  it should "load config if value are missing but field is optional" in {

    //given
    case class HttpConfig(@env("PORT") port: Int, @env("HOST") host: String)derives ConfigValue
    case class Config(httpConfig: Option[HttpConfig])derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "2000")

    //when
    val config = load[Config]

    //then
    config.value should be(Config(None))
  }

  it should "load optional case class if only optional field is missing" in {

    //given
    case class HttpConfig(@env("PORT") port: Int, @env("HOST") host: Option[String])derives ConfigValue
    case class Config(httpConfig: Option[HttpConfig])derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "2000")

    //when
    val config = load[Config]

    //then
    config.value should be(Config(Some(HttpConfig(2000, None))))
  }

  it should "load optional case class with default values" in {

    //given
    case class HttpConfig(@env("PORT") port: Int, @env("HOST") host: String = "localhost")derives ConfigValue
    case class Config(httpConfig: Option[HttpConfig])derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "2000")

    //when
    val config = load[Config]

    //then
    config.value should be(Config(Some(HttpConfig(2000))))
  }

  it should "fail to load nested case class in case invalid value is passed" in {

    //given
    case class HttpConfig(@env("PORT") port: Int, @env("HOST") host: String = "localhost")derives ConfigValue
    case class Config(httpConfig: Option[HttpConfig])derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "Bad")
      .onEnv("HOST", "localhost")

    //when
    val config = load[Config]

    //then
    config.left.value should be(ConfigError.invalid("was expecting integer", "Bad"))
  }
}