package jurate

import jurate.utils.FieldPath

/** Type class for decoding raw string values into typed configuration values.
  *
  * @tparam C
  *   the type to decode into
  */
trait ConfigDecoder[C] extends ConfigLoader[C]:
  /** Decodes a raw string value into a typed value.
    *
    * @param raw
    *   the raw string value to decode
    * @param ctx
    *   the decoding context containing annotations and field path
    * @return
    *   either a configuration error or the decoded value
    */
  def decode(raw: String, ctx: DecodingContext): Either[ConfigError, C]

  /** Maps the decoded value through a function.
    *
    * @param f
    *   the function to apply to the decoded value
    * @tparam B
    *   the target type
    * @return
    *   a new decoder for type B
    */
  def contramap[B](f: C => B): ConfigDecoder[B] = decode(_, _).map(f)

  /** Maps the decoded value through an error-handling function.
    *
    * @param f
    *   the function to apply that may fail with a ConfigError
    * @tparam B
    *   the target type
    * @return
    *   a new decoder for type B
    */
  def emap[B](
      f: (C, String, DecodingContext) => Either[ConfigError, B]
  ): ConfigDecoder[B] =
    (raw, ctx) => decode(raw, ctx).flatMap(f(_, raw, ctx))

object ConfigDecoder:
  /** Summons an implicit ConfigDecoder instance for type C.
    *
    * @tparam C
    *   the type to decode
    * @return
    *   the decoder instance
    */
  def apply[C](using value: ConfigDecoder[C]): ConfigDecoder[C] = value

/** Context information available during decoding.
  *
  * @param annotations
  *   the configuration source annotations
  * @param evaluatedAnnotation
  *   the annotation that provided the value
  * @param fieldPath
  *   the path to the field being decoded
  */
case class DecodingContext(
    annotations: Seq[ConfigAnnotation],
    evaluatedAnnotation: ConfigAnnotation,
    fieldPath: FieldPath
)
