package jurate

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues

import java.net.{InetAddress, URI}
import java.nio.file.{Path, Paths}
import java.util.UUID
import org.scalatest.prop.TableDrivenPropertyChecks
import scala.concurrent.duration.Duration

class DecodersSpec
    extends AnyFlatSpec
    with Matchers
    with EitherValues
    with TableDrivenPropertyChecks {

  behavior of "ConfigDecoder for String"
  it should "decode String" in {
    given ConfigReader = ConfigReader.mocked.onEnv("STR", "hello")

    case class Config(@env("STR") value: String)

    load[Config].value shouldBe Config("hello")
  }

  behavior of "ConfigDecoder for Short"
  it should "decode Short" in {
    given ConfigReader = ConfigReader.mocked.onEnv("SHORT", "123")

    case class Config(@env("SHORT") value: Short)

    load[Config].value shouldBe Config(123.toShort)
  }

  it should "fail to decode invalid Short" in {
    given ConfigReader = ConfigReader.mocked.onEnv("SHORT", "abc")

    case class Config(@env("SHORT") value: Short)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Int"
  it should "decode Int" in {
    given ConfigReader = ConfigReader.mocked.onEnv("INT", "456")

    case class Config(@env("INT") value: Int)

    load[Config].value shouldBe Config(456)
  }

  it should "fail to decode invalid Int" in {
    given ConfigReader = ConfigReader.mocked.onEnv("INT", "notAnInt")

    case class Config(@env("INT") value: Int)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Long"
  it should "decode Long" in {
    given ConfigReader = ConfigReader.mocked.onEnv("LONG", "12345678900")

    case class Config(@env("LONG") value: Long)

    load[Config].value shouldBe Config(12345678900L)
  }

  it should "fail to decode invalid Long" in {
    given ConfigReader = ConfigReader.mocked.onEnv("LONG", "invalidLong")

    case class Config(@env("LONG") value: Long)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Float"
  it should "decode Float" in {
    given ConfigReader = ConfigReader.mocked.onEnv("FLOAT", "123.45")

    case class Config(@env("FLOAT") value: Float)

    load[Config].value shouldBe Config(123.45f)
  }

  it should "fail to decode invalid Float" in {
    given ConfigReader = ConfigReader.mocked.onEnv("FLOAT", "abc.def")

    case class Config(@env("FLOAT") value: Float)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Double"
  it should "decode Double" in {
    given ConfigReader = ConfigReader.mocked.onEnv("DOUBLE", "6789.01")

    case class Config(@env("DOUBLE") value: Double)

    load[Config].value shouldBe Config(6789.01)
  }

  it should "fail to decode invalid Double" in {
    given ConfigReader = ConfigReader.mocked.onEnv("DOUBLE", "NaNValue")

    case class Config(@env("DOUBLE") value: Double)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for BigDecimal"
  it should "decode BigDecimal" in {
    given ConfigReader = ConfigReader.mocked.onEnv("BIGDEC", "98765.4321")

    case class Config(@env("BIGDEC") value: BigDecimal)

    load[Config].value shouldBe Config(BigDecimal("98765.4321"))
  }

  it should "fail to decode invalid BigDecimal" in {
    given ConfigReader = ConfigReader.mocked.onEnv("BIGDEC", "invalidDecimal")

    case class Config(@env("BIGDEC") value: BigDecimal)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for BigInt"
  it should "decode BigInt" in {
    given ConfigReader =
      ConfigReader.mocked.onEnv("BIGINT", "9876543210123456789")

    case class Config(@env("BIGINT") value: BigInt)

    load[Config].value shouldBe Config(BigInt("9876543210123456789"))
  }

  it should "fail to decode invalid BigInt" in {
    given ConfigReader = ConfigReader.mocked.onEnv("BIGINT", "notANumber")

    case class Config(@env("BIGINT") value: BigInt)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for InetAddress"
  it should "decode InetAddress" in {
    given ConfigReader = ConfigReader.mocked.onEnv("IP", "192.168.1.1")

    case class Config(@env("IP") value: InetAddress)

    load[Config].value shouldBe Config(InetAddress.getByName("192.168.1.1"))
  }

  it should "fail to decode invalid InetAddress" in {
    given ConfigReader = ConfigReader.mocked.onEnv("IP", "invalid_host")

    case class Config(@env("IP") value: InetAddress)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for UUID"
  it should "decode UUID" in {
    val uuidStr = "123e4567-e89b-12d3-a456-426614174000"
    given ConfigReader = ConfigReader.mocked.onEnv("UUID", uuidStr)

    case class Config(@env("UUID") value: UUID)

    load[Config].value shouldBe Config(UUID.fromString(uuidStr))
  }

  it should "fail to decode invalid UUID" in {
    given ConfigReader = ConfigReader.mocked.onEnv("UUID", "not-a-valid-uuid")

    case class Config(@env("UUID") value: UUID)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Path"
  it should "decode Path" in {
    given ConfigReader = ConfigReader.mocked.onEnv("PATH", "/tmp/config")

    case class Config(@env("PATH") value: Path)

    load[Config].value shouldBe Config(Paths.get("/tmp/config"))
  }

  it should "fail to decode invalid Path" in {
    given ConfigReader =
      ConfigReader.mocked.onEnv("PATH", "\u0000") // Invalid path string

    case class Config(@env("PATH") value: Path)

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

      case class Config(@env("FLAG") value: Boolean)

      load[Config].value shouldBe Config(expected)
    }
  }

  it should "fail to decode invalid Boolean" in {
    given ConfigReader = ConfigReader.mocked.onEnv("FLAG", "maybe")

    case class Config(@env("FLAG") value: Boolean)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Duration"

  it should "correctly decode valid duration strings" in {

    val validDurations = Table(
      ("input", "expected"),
      ("5 minutes", Duration.apply(5, "minute")),
      ("2m", Duration.apply(2, "minute")),
      ("33s", Duration.apply(33, "second")),
      ("3 days", Duration.apply(3, "day")),
      ("3 ns", Duration.apply(3, "nanoseconds"))
    )

    forAll(validDurations) { (input: String, expected: Duration) =>
      given ConfigReader = ConfigReader.mocked.onEnv("DURATION", input)

      case class Config(@env("DURATION") value: Duration)

      load[Config].value shouldBe Config(expected)
    }
  }

  it should "fail to decode invalid Duration" in {
    given ConfigReader = ConfigReader.mocked.onEnv("DURATION", "1 car")

    case class Config(@env("DURATION") value: Duration)

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for Option"
  it should "decode Option[Int] when value is present" in {
    given ConfigReader = ConfigReader.mocked.onEnv("OPT", "42")

    case class Config(@env("OPT") value: Option[Int])

    load[Config].value shouldBe Config(Some(42))
  }

  it should "fail to decode Option[Int] when value is invalid" in {
    given ConfigReader = ConfigReader.mocked.onEnv("OPT", "not_a_number")

    case class Config(@env("OPT") value: Option[Int])

    load[Config].left.value shouldBe a[ConfigError]
  }

  behavior of "ConfigDecoder for URI"
  it should "decode URI" in {
    val uriStr = "http://example.com/resource"

    given ConfigReader = ConfigReader.mocked.onEnv("URI", uriStr)

    case class Config(@env("URI") value: URI)

    load[Config].value shouldBe Config(URI.create(uriStr))
  }

  it should "fail to decode URI when it's incorrect" in {
    val uriStr = "http:// "

    given ConfigReader = ConfigReader.mocked.onEnv("URI", uriStr)

    case class Config(@env("URI") value: URI)

    load[Config].left.value shouldBe a[ConfigError]
  }
}
