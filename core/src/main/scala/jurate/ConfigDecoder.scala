package jurate

trait ConfigDecoder[C] extends ConfigLoader[C]:
  def decode(raw: String): Either[ConfigError, C]

  def contramap[B](f: C => B): ConfigDecoder[B] = decode(_).map(f)

object ConfigDecoder:
  def apply[C](using value: ConfigDecoder[C]): ConfigDecoder[C] = value
