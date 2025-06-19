package jurate

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
      .onProp("config.name", "jurate")

    case class Config(@jurate.prop("config.name") name: Name)

    // when
    val config = load[Config]

    // then
    config.value should be(Config(Name("jurate")))

  }
}
