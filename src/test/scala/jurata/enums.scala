package jurata

import jurata.EnumsSpec.Nested.NestedSeverity
import jurata.EnumsSpec.Severity
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

enum Protocol derives ConfigValue:
  case HTTP
  case HTTPS

enum User derives ConfigValue:
  case Regular(@env("USER_EMAIL") email: String)
  case Admin(@env("ADMIN_NAME") name: String)


class EnumsSpec extends AnyFlatSpec with Matchers with EitherValues {

  it should "decode simple enum declared in object" in {
    given ConfigReader = ConfigReader.mocked
      .onEnv("SEV", "Error")

    case class Config(@env("SEV") bugSeverity: Severity)derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.value should be(Config(Severity.Error))

    case class ConfigWithNested(@env("SEV") bugSeverity: NestedSeverity)derives ConfigValue

    //when
    val configWithNested = load[ConfigWithNested]

    //then
    configWithNested.value should be(ConfigWithNested(NestedSeverity.Error))
  }

  it should "decode simple enum" in {
    given ConfigReader = ConfigReader.mocked
      .onProp("protocol", "HTTPS")

    case class Config(@prop("protocol") protocol: Protocol) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.value should be(Config(Protocol.HTTPS))
  }

  it should "decode simple enum regardless of case" in {
    given ConfigReader = ConfigReader.mocked
      .onProp("protocol", "hTtP")

    case class Config(@prop("protocol") protocol: Protocol) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.value should be(Config(Protocol.HTTP))
  }

  it should "fail to decode enum if value is missing" in {

    given ConfigReader = ConfigReader.mocked
      .onEnv("SEV", "Bad")

    case class Config(@env("SEV") bugSeverity: Severity) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.left.value should be(ConfigError.invalid("couldn't find case for enum Severity (available values: Error, Warning)", "Bad"))
  }

  it should "decode enum with fields" in {

    given ConfigReader = ConfigReader.mocked
      .onEnv("ADMIN_NAME", "Jack")

    case class Config(@env("ENV") user: User) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.value should be(Config(User.Admin("Jack")))
  }

  it should "use default value if can't load value" in {

    given ConfigReader = ConfigReader.mocked

    case class Config(@env("ENV") user: User = User.Regular("test@acme.com")) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.value should be(Config(User.Regular("test@acme.com")))
  }

    it should "load first availble enum case" in {

    given ConfigReader = ConfigReader.mocked
      .onEnv("ADMIN_NAME", "Jack")
      .onEnv("USER_EMAIL", "jack@acme.com")

    case class Config(@env("ENV") user: User) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.value should be(Config(User.Regular("jack@acme.com")))
  }

}

object EnumsSpec {
  enum Severity derives ConfigValue:
    case Error
    case Warning

  object Nested {
    enum NestedSeverity derives ConfigValue:
      case Error
      case Warning
  }
}