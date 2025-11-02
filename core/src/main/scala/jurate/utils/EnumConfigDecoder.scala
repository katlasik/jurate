package jurate.utils

import jurate.{ConfigDecoder, ConfigError, DecodingContext}

private[jurate] final class EnumConfigDecoder[C](
    values: Array[C],
    enumName: String
) extends ConfigDecoder[C]:
  def decode(raw: String, ctx: DecodingContext): Either[ConfigError, C] =
    values
      .find(_.toString() == raw)
      .toRight(
        ConfigError.invalid(
          s"couldn't find case for enum $enumName (available values: ${values.mkString(", ")})",
          raw,
          ctx.annotations.headOption
        )
      )
