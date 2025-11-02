package jurate

trait ConfigDecoder[C] extends ConfigLoader[C]:
  def decode(raw: String, ctx: DecodingContext): Either[ConfigError, C]

  def contramap[B](f: C => B): ConfigDecoder[B] = decode(_, _).map(f)

  def emap[B](
      f: (C, String, DecodingContext) => Either[ConfigError, B]
  ): ConfigDecoder[B] =
    (raw, ctx) => decode(raw, ctx).flatMap(f(_, raw, ctx))

object ConfigDecoder:
  def apply[C](using value: ConfigDecoder[C]): ConfigDecoder[C] = value

case class DecodingContext(
    annotations: Seq[ConfigAnnotation],
    fieldName: String
)
