package jurate

import jurate.utils.Hasher

case class Secret[V](value: V) extends AnyVal {

  def hash: String = Hasher.hash(value)
  
  def shortHash: String = hash.take(10)

  override def toString: String = s"Secret($shortHash)"
}

object Secret {
  def apply[V](value: V): Secret[V] = new Secret(value)

  given [V: ConfigDecoder] => ConfigDecoder[Secret[V]] =
    ConfigDecoder[V].contramap(Secret.apply)

}


