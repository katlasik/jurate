package jurate

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
    * @return
    *   either a configuration error message as string or the decoded value
    */
  def decode(raw: String): Either[String, C]

  /** Maps the decoded value through a function.
    *
    * @param f
    *   the function to apply to the decoded value
    * @tparam B
    *   the target type
    * @return
    *   a new decoder for type B
    */
  def contramap[B](f: C => B): ConfigDecoder[B] = decode(_).map(f)

  /** Maps the decoded value through an error-handling function.
    *
    * @param f
    *   the function to apply that may fail with an error message string
    * @tparam B
    *   the target type
    * @return
    *   a new decoder for type B
    */
  def emap[B](
      f: (C, String) => Either[String, B]
  ): ConfigDecoder[B] =
    raw => decode(raw).flatMap(f(_, raw))

object ConfigDecoder:
  /** Summons an implicit ConfigDecoder instance for type C.
    *
    * @tparam C
    *   the type to decode
    * @return
    *   the decoder instance
    */
  def apply[C](using value: ConfigDecoder[C]): ConfigDecoder[C] = value
