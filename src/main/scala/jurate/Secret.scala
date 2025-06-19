package jurate

case class Secret[V](value: V) extends AnyVal {
  override def toString: String = "*****"
}

object Secret {
  def apply[V](value: V): Secret[V] = new Secret(value)

  given [V: ConfigDecoder] => ConfigDecoder[Secret[V]] =
    ConfigDecoder[V].contramap(Secret.apply)

}
