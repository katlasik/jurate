package jurate

import jurate.Nested.NestedSeverity
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

enum Protocol:
  case HTTP
  case HTTPS

enum User:
  case Unverified(@env("ANON") anonymous: Boolean)
  case Regular(@env("USER_EMAIL") email: String)
  case Admin(@env("ADMIN_NAME") name: String)
  case Banned(@prop("REASON") reason: String)

enum Severity:
  case Error
  case Warning

object Nested {
  enum NestedSeverity:
    case Error
    case Warning
}

class EnumsSpec extends AnyFlatSpec with Matchers with EitherValues {

  it should "decode simple enum declared in object" in {
    given ConfigReader = ConfigReader.mocked
      .onEnv("SEV", "Warning")

    case class Config(@env("SEV") bugSeverity: Severity)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Severity.Warning))

    case class ConfigWithNested(@env("SEV") bugSeverity: NestedSeverity)

    // when
    val configWithNested = load[ConfigWithNested]

    // then
    configWithNested.value should be(ConfigWithNested(NestedSeverity.Warning))
  }

  it should "decode simple enum" in {
    given ConfigReader = ConfigReader.mocked
      .onProp("protocol", "HTTPS")

    case class Config(@prop("protocol") protocol: Protocol)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Protocol.HTTPS))
  }

  it should "decode simple with custom decoder" in {
    given ConfigReader = ConfigReader.mocked
      .onProp("protocol", "hTtPs")

    given ConfigDecoder[Protocol] = new ConfigDecoder[Protocol]:
      def decode(raw: String, ctx: DecodingContext): Either[ConfigError, Protocol] =
        val rawLowercased = raw.trim().toLowerCase()
        Protocol.values
          .find(_.toString().toLowerCase() == rawLowercased)
          .toRight(
            ConfigError.invalid("protocol", "Couldn't find right value for Protocol", raw, Some(prop("protocol")))
          )

    case class Config(@prop("protocol") protocol: Protocol)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Protocol.HTTPS))
  }

  it should "fail to decode enum if value is missing" in {

    given ConfigReader = ConfigReader.mocked
      .onEnv("SEV", "Bad")

    case class Config(@env("SEV") bugSeverity: Severity)

    // when
    val config = load[Config]

    // then
    config.left.value should be(
      ConfigError.invalid(
        "bugSeverity",
        "couldn't find case for enum Severity (available values: Error, Warning)",
        "Bad",
        Some(env("SEV"))
      )
    )
  }

  it should "use default value if enum is missing" in {

    given ConfigReader = ConfigReader.mocked

    case class Config(@env("SEV") bugSeverity: Severity = Severity.Error)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Severity.Error))
  }

  it should "decode enum with fields" in {

    given ConfigReader = ConfigReader.mocked
      .onEnv("ADMIN_NAME", "Jack")

    case class Config(@env("ENV") user: User)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(User.Admin("Jack")))
  }

  it should "use default value if can't load value" in {

    given ConfigReader = ConfigReader.mocked

    case class Config(@env("ENV") user: User = User.Regular("test@acme.com"))

    // when
    val config = load[Config]

    // then
    config.value should be(Config(User.Regular("test@acme.com")))
  }

  it should "load first availble enum case" in {

    given ConfigReader = ConfigReader.mocked
      .onEnv("ADMIN_NAME", "Jack")
      .onEnv("USER_EMAIL", "jack@acme.com")

    case class Config(@env("ENV") user: User)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(User.Regular("jack@acme.com")))
  }

  it should "fail if enum case is missing annotations" in {

    // given
    enum MedicalJob:
      case MedicalDoctor(specialization: String)
      case Nurse

    given ConfigReader = ConfigReader.mocked

    case class Config(job: MedicalJob)

    // when
    val config = load[Config]

    // then
    config.left.value.getMessage should include(
      "No annotations found for field: specialization"
    )
  }

}
