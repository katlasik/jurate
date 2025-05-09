package jurata

import jurata.EnumsSpec.Nested.NestedSeverity
import jurata.EnumsSpec.Severity
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

enum Protocol derives ConfigValue:
  case HTTP
  case HTTPS

enum EnumWithFields derives ConfigValue:
  case A(value: String)

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

  it should "decode simple toplevel enum" in {
    given ConfigReader = ConfigReader.mocked
      .onProp("protocol", "HTTPS")

    case class Config(@prop("protocol") protocol: Protocol) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.value should be(Config(Protocol.HTTPS))
  }

  it should "fail to decode enum if value is missing" in {

    given ConfigReader = ConfigReader.mocked
      .onEnv("SEV", "Bad")

    case class Config(@env("SEV") bugSeverity: Severity) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.left.value should be(ConfigError.invalid("Couldn't find case Bad in enum jurata.EnumsSpec.Severity", "Bad"))
  }

  it should "fail to decode enum with fields" in {

    given ConfigReader = ConfigReader.mocked
      .onEnv("ENV", "x")

    case class Config(@env("ENV") value: EnumWithFields) derives ConfigValue

    //when
    val config = load[Config]

    //then
    config.left.value should be(ConfigError.other("Couldn't decode enum jurata.EnumWithFields - only decoding of enums without fields is supported"))
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