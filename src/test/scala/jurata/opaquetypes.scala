package jurata

import org.scalatest.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

opaque type Name = String

object Name {
  def apply(value: String): Name = value

  extension (name: Name) {
    def value: String = name
  }

  given ConfigDecoder[Name] = ConfigDecoder[String].contramap(Name.apply)
}

class OpaqueTypesSpec extends AnyFlatSpec with Matchers with EitherValues {

  it should "load opaque types class" in {

    // given
    given ConfigReader = ConfigReader.mocked
      .onProp("config.name", "jurata")

    case class Config(@jurata.prop("config.name") name: Name)
        derives ConfigLoader

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Name("jurata")))

  }
}
