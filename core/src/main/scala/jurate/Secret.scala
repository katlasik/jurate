package jurate

import jurate.utils.Hasher

/** A wrapper for sensitive values that masks them in toString.
  *
  * Secret is useful for wrapping passwords, API keys, and other sensitive configuration
  * values. The actual value is never exposed in toString output, only a short hash.
  *
  * @param value the sensitive value to wrap
  * @tparam V the type of the secret value
  */
case class Secret[V](value: V) extends AnyVal {

  /** Returns the SHA-256 hash of the secret value.
    *
    * @return a hex-encoded SHA-256 hash of the value
    */
  def hash: String = Hasher.hash(value)

  /** Returns the first 10 characters of the hash.
    *
    * @return a truncated hash for display purposes
    */
  def shortHash: String = hash.take(10)

  /** Returns a string representation that masks the actual value.
    *
    * @return a string showing only the short hash, not the actual value
    */
  override def toString: String = s"Secret($shortHash)"
}

/** Companion object for Secret. */
object Secret {
  /** Creates a new Secret wrapping the given value.
    *
    * @param value the value to wrap
    * @tparam V the type of the value
    * @return a new Secret instance
    */
  def apply[V](value: V): Secret[V] = new Secret(value)

  /** Provides a ConfigDecoder for Secret[V] given a ConfigDecoder for V.
    *
    * @tparam V the type of the secret value
    * @param decoder the decoder for the underlying value type
    * @return a ConfigDecoder that wraps decoded values in Secret
    */
  given [V: ConfigDecoder] => ConfigDecoder[Secret[V]] =
    ConfigDecoder[V].contramap(Secret.apply)

}


