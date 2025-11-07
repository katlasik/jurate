package jurate.utils

import jurate.ConfigDecoder

/** ConfigDecoder for singleton enum cases (enums without fields).
  *
  * This decoder matches raw string values against enum case names using exact
  * string matching. It is automatically used for singleton enums when deriving
  * ConfigLoader instances.
  */
private[jurate] final class EnumConfigDecoder[C](
    values: Array[C],
    enumName: String
) extends ConfigDecoder[C]:

  def decode(raw: String): Either[String, C] =
    values
      .find(_.toString() == raw)
      .toRight(
        s"couldn't find case for enum $enumName (available values: ${values.mkString(", ")})"
      )
