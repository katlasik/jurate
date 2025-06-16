package jurata.utils

import jurata.ConfigDecoder
import jurata.ConfigError

private[jurata] final class EnumConfigDecoder[C](
    values: Array[C],
    enumName: String
) extends ConfigDecoder[C]:
  def decode(raw: String): Either[ConfigError, C] =
    values
      .find(_.toString() == raw)
      .toRight(
        ConfigError.invalid(
          s"couldn't find case for enum $enumName (available values: ${values.mkString(", ")})",
          raw
        )
      )
