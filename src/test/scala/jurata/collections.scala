package jurata

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

enum Fruit derives EnumConfigDecoder:
  case Apple, Banana, Orange, Pear

class CollectionsSpec extends AnyFlatSpec with Matchers with EitherValues {

  it should "load collection of enums" in {

    case class FruitConfig(@env("FRUITS") fruits: List[Fruit])

    given ConfigReader = ConfigReader.mocked
      .onEnv("FRUITS", "Apple, Banana, Pear")

    // when
    val config = load[FruitConfig]

    // then
    config.value shouldBe FruitConfig(
      List(Fruit.Apple, Fruit.Banana, Fruit.Pear)
    )
  }

  it should "load collection of ints" in {

    case class Numbers(@env("NUMBERS") numbers: Vector[Int])

    given ConfigReader = ConfigReader.mocked
      .onEnv("NUMBERS", "1, 10, 100")

    // when
    val config = load[Numbers]

    // then
    config.value shouldBe Numbers(Vector(1, 10, 100))
  }

  it should "load collection of booleans" in {

    case class Bools(@env("BOOLS") bools: Seq[Boolean])

    given ConfigReader = ConfigReader.mocked
      .onEnv("BOOLS", "1,0,1")

    // when
    val config = load[Bools]

    // then
    config.value shouldBe Bools(Seq(true, false, true))
  }
}
