package jurate.utils

import java.security.MessageDigest

private[jurate] object Hasher {

  def hash[T](value: T): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(value.toString.getBytes("UTF-8"))

    hash.map("%02x".format(_)).mkString
  }

}
