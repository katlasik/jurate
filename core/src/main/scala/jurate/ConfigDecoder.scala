package jurate

trait ConfigDecoder[C] extends ConfigLoader[C]:
  def decode(raw: String): Either[ConfigError, C]

  def contramap[B](f: C => B): ConfigDecoder[B] = decode(_).map(f)

  def emap[B](f: (C, String) => Either[ConfigError, B]): ConfigDecoder[B] =
    raw => decode(raw).flatMap(f(_, raw))

object ConfigDecoder:
  def apply[C](using value: ConfigDecoder[C]): ConfigDecoder[C] = value
