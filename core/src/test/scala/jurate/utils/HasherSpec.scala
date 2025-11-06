package jurate.utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HasherSpec extends AnyFlatSpec with Matchers {

  "Hasher" should "return full SHA-256 hash as hex string" in {
    Hasher.hash("qwerty") should be(
      "65e84be33532fb784c48129675f9eff3a682b27168c0ea744b2cf58ee02337c5"
    )
    Hasher.hash("") should be(
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    )
  }

  it should "return consistent hash for same input" in {
    val hash1 = Hasher.hash("test")
    val hash2 = Hasher.hash("test")
    hash1 should be(hash2)
  }

  it should "return different hashes for different inputs" in {
    val hash1 = Hasher.hash("test1")
    val hash2 = Hasher.hash("test2")
    hash1 should not be hash2
  }

}
