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

  it should "use default value of case class in case any value is missing" in {

    //given
    case class HttpConfig(@env("PORT") port: Int, @env("HOST") host: String)derives ConfigValue
    case class Config(httpConfig: HttpConfig = HttpConfig(1000, "localhost"))derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("HOST", "localhost")

    //when
    val config = load[Config]

    //then
    config.value should be(Config(HttpConfig(1000, "localhost")))
  }

  it should "load first matching case class in case of sealed trait" in {

    //given
    sealed trait MessagingConfig derives ConfigValue
    case class KafkaConfig(@env("KAFKA_BROKER") broker: String) extends MessagingConfig
    case class Redis(@env("REDIS_CLUSTER" )cluster: String) extends MessagingConfig

    case class Config(messaging: MessagingConfig) derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("REDIS_CLUSTER", "cluster")

    //when
    val config = load[Config]

    //then
    config.value should be(Config(Redis("cluster")))
  }

  it should "fail if there's no matching values for any of subclasses" in {

    //given
    sealed trait MessagingConfig derives ConfigValue
    case class KafkaConfig(@env("KAFKA_BROKER") @prop("broker") broker: String) extends MessagingConfig derives ConfigValue
    case class Redis(@env("REDIS_CLUSTER" )cluster: String) extends MessagingConfig derives ConfigValue

    case class Config(messaging: MessagingConfig) derives ConfigValue

    given ConfigReader = ConfigReader.mocked

    //when
    val config = load[Config]

    //then
    config.left.value.getMessage.lines.toList should contain allOf(
      "Missing environment variable KAFKA_BROKER, missing system property broker",
      "Missing environment variable REDIS_CLUSTER"
    )
  }

  it should "NOT fail if there's no matching values for any of subclasses but field is optional" in {

    //given
    sealed trait MessagingConfig derives ConfigValue
    case class KafkaConfig(@env("KAFKA_BROKER") @prop("broker") broker: String) extends MessagingConfig derives ConfigValue
    case class Redis(@env("REDIS_CLUSTER" )cluster: String) extends MessagingConfig derives ConfigValue

    case class Config(messaging: Option[MessagingConfig]) derives ConfigValue

    given ConfigReader = ConfigReader.mocked
    
    //when
    val config = load[Config]

    //then
    config.value should be(Config(None))
  }

  it should "NOT fail if there's no matching values for any of subclasses but there's default value" in {

    //given
    sealed trait MessagingConfig derives ConfigValue
    case class KafkaConfig(@env("KAFKA_BROKER") @prop("broker") broker: String) extends MessagingConfig derives ConfigValue
    case class Redis(@env("REDIS_CLUSTER" )cluster: String) extends MessagingConfig derives ConfigValue

    case class Config(messaging: MessagingConfig = KafkaConfig("broker")) derives ConfigValue

    given ConfigReader = ConfigReader.mocked
    
    //when
    val config = load[Config]

    //then
    config.value should be(Config(KafkaConfig("broker")))
  }
}