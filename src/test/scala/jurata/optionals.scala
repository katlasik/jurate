package jurata

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OptionalsSpec extends AnyFlatSpec with Matchers with EitherValues {

  it should "load optional value" in {

    // given
    case class Config(@env("PORT") port: Option[Int]) derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT", "2000")

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Some(2000)))
  }

  it should "not fail if optional value is missing" in {

    // given
    case class Config(@env("PORT") port: Option[Int]) derives ConfigValue

    given ConfigReader = ConfigReader.mocked
      .onEnv("PORT2", "200")

    // when
    val config = load[Config]

    // then
    config.value should be(Config(None))
  }

}
