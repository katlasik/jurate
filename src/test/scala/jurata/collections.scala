package jurata

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CollectionsSpec extends AnyFlatSpec with Matchers with EitherValues {

  enum Fruit derives ConfigValue:
    case Apple, Banana, Orange, Pear

  it should "load collection of simple values" in {

    // given
    case class FruitConfig(@env("FRUITS") fruits: List[Fruit]) derives ConfigValue

    ConfigDecoder[List[Fruit]]

    given ConfigReader = ConfigReader.mocked
      .onEnv("FRUITS", "apple, banana, pear")

    // when
    val config = load[FruitConfig]

    // then
    config.value shouldBe FruitConfig(List(Fruit.Apple, Fruit.Banana, Fruit.Pear))
  }
}
