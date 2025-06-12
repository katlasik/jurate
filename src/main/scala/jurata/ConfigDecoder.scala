package jurata

import jurata.utils.Macros.*
import scala.compiletime.*

trait ConfigDecoder[C]:
  def decode(raw: String): Either[ConfigError, C]

  def contramap[B](f: C => B): ConfigDecoder[B] = decode(_).map(f)

object ConfigDecoder:
  def apply[C](using value: ConfigDecoder[C]): ConfigDecoder[C] = value

final class EnumConfigDecoder[C](values: Array[C], enumName: String)
    extends ConfigDecoder[C]:
  def decode(raw: String): Either[ConfigError, C] =
    values
      .find(_.toString() == raw)
      .toRight(
        ConfigError.invalid(
          s"couldn't find case for enum $enumName (available values: ${values.mkString(", ")})",
          raw
        )
      )

object EnumConfigDecoder:

  inline def derived[C]: EnumConfigDecoder[C] = {
    inline if isSingletonEnum[C] then
      val cases = enumCases[C]
      val name = typeName[C]

      new EnumConfigDecoder(cases, name)
    else
      error("You can derive decoder only for singleton enums (without fields!)")

  }
