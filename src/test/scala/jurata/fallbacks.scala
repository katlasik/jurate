package jurata

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FallbacksSpec extends AnyFlatSpec with Matchers with EitherValues {

  it should "don't use fallback value if first is available" in {

    // given
    case class Config(
        @prop("port") @env("PORT") @env("FALLBACK_PORT") port: Int
    ) derives ConfigLoader

    // when
    val configWithEnv = load[Config](using
      reader = ConfigReader.mocked
        .onEnv("PORT", "1000")
        .onEnv("FALLBACK_PORT", "1001")
    )

    // then
    configWithEnv.value should be(Config(1000))

    // when
    val configWithProp = load[Config](using
      reader = ConfigReader.mocked
        .onEnv("PORT", "1000")
        .onEnv("FALLBACK_PORT", "1001")
        .onProp("port", "9999")
    )

    // then
    configWithProp.value should be(Config(9999))
  }

  it should "use fallback value if first is not available" in {

    // given
    case class Config(@env("PORT") @env("FALLBACK_PORT") port: Int)
        derives ConfigLoader

    given ConfigReader = ConfigReader.mocked
      .onEnv("FALLBACK_PORT", "1001")

    // when
    val config = load[Config]

    // then
    config.value should be(Config(1001))
  }
}
