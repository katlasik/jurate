package jurata

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues

import java.net.{InetAddress, URI}
import java.nio.file.{Path, Paths}
import java.util.UUID
import org.scalatest.prop.TableDrivenPropertyChecks

class DecodersSpec
    extends AnyFlatSpec
    with Matchers
    with EitherValues
    with TableDrivenPropertyChecks {

  behavior of "ConfigDecoder for String"
  it should "decode String" in {
    given ConfigReader = ConfigReader.mocked.onEnv("STR", "hello")

    case class Config(@env("STR") value: String) derives ConfigValue

    load[Config].value shouldBe Config("hello")
  }

  behavior of "ConfigDecoder for Short"
  it should "decode Short" in {
    given ConfigReader = ConfigReader.mocked.onEnv("SHORT", "123")

    case class Config(@env("SHORT") value: Short) derives ConfigValue

    load[Config].value shouldBe Config(123.toShort)
  }

  it should "fail to decode invalid Short" in {
    given ConfigReader = ConfigReader.mocked.onEnv("SHORT", "abc")

    case class Config(@env("SHORT") value: Short) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Int"
  it should "decode Int" in {
    given ConfigReader = ConfigReader.mocked.onEnv("INT", "456")

    case class Config(@env("INT") value: Int) derives ConfigValue

    load[Config].value shouldBe Config(456)
  }

  it should "fail to decode invalid Int" in {
    given ConfigReader = ConfigReader.mocked.onEnv("INT", "notAnInt")

    case class Config(@env("INT") value: Int) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Long"
  it should "decode Long" in {
    given ConfigReader = ConfigReader.mocked.onEnv("LONG", "12345678900")

    case class Config(@env("LONG") value: Long) derives ConfigValue

    load[Config].value shouldBe Config(12345678900L)
  }

  it should "fail to decode invalid Long" in {
    given ConfigReader = ConfigReader.mocked.onEnv("LONG", "invalidLong")

    case class Config(@env("LONG") value: Long) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Float"
  it should "decode Float" in {
    given ConfigReader = ConfigReader.mocked.onEnv("FLOAT", "123.45")

    case class Config(@env("FLOAT") value: Float) derives ConfigValue

    load[Config].value shouldBe Config(123.45f)
  }

  it should "fail to decode invalid Float" in {
    given ConfigReader = ConfigReader.mocked.onEnv("FLOAT", "abc.def")

    case class Config(@env("FLOAT") value: Float) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Double"
  it should "decode Double" in {
    given ConfigReader = ConfigReader.mocked.onEnv("DOUBLE", "6789.01")

    case class Config(@env("DOUBLE") value: Double) derives ConfigValue

    load[Config].value shouldBe Config(6789.01)
  }

  it should "fail to decode invalid Double" in {
    given ConfigReader = ConfigReader.mocked.onEnv("DOUBLE", "NaNValue")

    case class Config(@env("DOUBLE") value: Double) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for BigDecimal"
  it should "decode BigDecimal" in {
    given ConfigReader = ConfigReader.mocked.onEnv("BIGDEC", "98765.4321")

    case class Config(@env("BIGDEC") value: BigDecimal) derives ConfigValue

    load[Config].value shouldBe Config(BigDecimal("98765.4321"))
  }

  it should "fail to decode invalid BigDecimal" in {
    given ConfigReader = ConfigReader.mocked.onEnv("BIGDEC", "invalidDecimal")

    case class Config(@env("BIGDEC") value: BigDecimal) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for BigInt"
  it should "decode BigInt" in {
    given ConfigReader =
      ConfigReader.mocked.onEnv("BIGINT", "9876543210123456789")

    case class Config(@env("BIGINT") value: BigInt) derives ConfigValue

    load[Config].value shouldBe Config(BigInt("9876543210123456789"))
  }

  it should "fail to decode invalid BigInt" in {
    given ConfigReader = ConfigReader.mocked.onEnv("BIGINT", "notANumber")

    case class Config(@env("BIGINT") value: BigInt) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for InetAddress"
  it should "decode InetAddress" in {
    given ConfigReader = ConfigReader.mocked.onEnv("IP", "192.168.1.1")

    case class Config(@env("IP") value: InetAddress) derives ConfigValue

    load[Config].value shouldBe Config(InetAddress.getByName("192.168.1.1"))
  }

  it should "fail to decode invalid InetAddress" in {
    given ConfigReader = ConfigReader.mocked.onEnv("IP", "invalid_host")

    case class Config(@env("IP") value: InetAddress) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for UUID"
  it should "decode UUID" in {
    val uuidStr = "123e4567-e89b-12d3-a456-426614174000"
    given ConfigReader = ConfigReader.mocked.onEnv("UUID", uuidStr)

    case class Config(@env("UUID") value: UUID) derives ConfigValue

    load[Config].value shouldBe Config(UUID.fromString(uuidStr))
  }

  it should "fail to decode invalid UUID" in {
    given ConfigReader = ConfigReader.mocked.onEnv("UUID", "not-a-valid-uuid")

    case class Config(@env("UUID") value: UUID) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Path"
  it should "decode Path" in {
    given ConfigReader = ConfigReader.mocked.onEnv("PATH", "/tmp/config")

    case class Config(@env("PATH") value: Path) derives ConfigValue

    load[Config].value shouldBe Config(Paths.get("/tmp/config"))
  }

  it should "fail to decode invalid Path" in {
    given ConfigReader =
      ConfigReader.mocked.onEnv("PATH", "\u0000") // Invalid path string

    case class Config(@env("PATH") value: Path) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Boolean"

  it should "correctly decode valid boolean strings" in {

    val validBooleans = Table(
      ("input", "expected"),
      ("true", true),
      ("yes", true),
      ("1", true),
      ("false", false),
      ("no", false),
      ("0", false),
      ("TrUe", true),
      ("FaLsE", false)
    )

    forAll(validBooleans) { (input: String, expected: Boolean) =>
      given ConfigReader = ConfigReader.mocked.onEnv("FLAG", input)

      case class Config(@env("FLAG") value: Boolean) derives ConfigValue

      load[Config].value shouldBe Config(expected)
    }
  }

  it should "fail to decode invalid Boolean" in {
    given ConfigReader = ConfigReader.mocked.onEnv("FLAG", "maybe")

    case class Config(@env("FLAG") value: Boolean) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Option"
  it should "decode Option[Int] when value is present" in {
    given ConfigReader = ConfigReader.mocked.onEnv("OPT", "42")

    case class Config(@env("OPT") value: Option[Int]) derives ConfigValue

    load[Config].value shouldBe Config(Some(42))
  }

  it should "fail to decode Option[Int] when value is invalid" in {
    given ConfigReader = ConfigReader.mocked.onEnv("OPT", "not_a_number")

    case class Config(@env("OPT") value: Option[Int]) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for URI"
  it should "decode URI" in {
    val uriStr = "http://example.com/resource"

    given ConfigReader = ConfigReader.mocked.onEnv("URI", uriStr)

    case class Config(@env("URI") value: URI) derives ConfigValue

    load[Config].value shouldBe Config(URI.create(uriStr))
  }

  it should "fail to decode URI when it's incorrect" in {
    val uriStr = "http:// "

    given ConfigReader = ConfigReader.mocked.onEnv("URI", uriStr)

    case class Config(@env("URI") value: URI) derives ConfigValue

    load[Config].left.value shouldBe a[ConfigError]
  }
}
