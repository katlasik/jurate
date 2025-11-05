package jurate

import jurate.utils.*
import jurate.utils.Macros.*

import scala.deriving.Mirror
import scala.compiletime.*

/** Base trait for loading configuration values.
  *
  * ConfigLoader is the root of the type class hierarchy for configuration loading.
  * It can be either a [[ConfigDecoder]] (for primitive types) or a [[ConfigHandler]]
  * (for complex types like case classes).
  *
  * @tparam C the type to load configuration into
  */
trait ConfigLoader[C] {
  /** Loads configuration using the provided reader and field path.
    *
    * @param reader the ConfigReader to use for reading configuration sources
    * @param parentFieldPath the path prefix for this configuration field
    * @return Either a ConfigError or the loaded configuration value
    */
  def load(
      reader: ConfigReader,
      parentFieldPath: FieldPath
  ): Either[ConfigError, C] =
    this match {
      case ConfigHandler(handle) => handle(reader, parentFieldPath)
      case _ =>
        throw new UnsupportedOperationException(
          "You should only load config using ConfigHandler!"
        )
    }

  /** Converts this loader to a ConfigDecoder if possible.
    *
    * @return the ConfigDecoder instance
    * @throws UnsupportedOperationException if this is not a ConfigDecoder
    */
  def asDecoder: ConfigDecoder[C] =
    this match {
      case decoder: ConfigDecoder[C] => decoder
      case _ =>
        throw new UnsupportedOperationException(
          s"Unexpected! This is not ConfigDecoder: ${getClass().getName()}"
        )
    }

}

/** A ConfigLoader implementation for complex types like case classes and sealed traits.
  *
  * ConfigHandler wraps a function that knows how to load and compose configuration
  * from multiple fields recursively.
  *
  * @param handle function that loads configuration given a reader and field path
  * @tparam C the type to load configuration into
  */
final case class ConfigHandler[C](
    handle: (ConfigReader, FieldPath) => Either[ConfigError, C]
) extends ConfigLoader[C]

/** Companion object providing automatic derivation of ConfigLoader instances. */
object ConfigLoader:

  /** Summons a ConfigLoader instance for type C from implicit scope.
    *
    * @tparam C the configuration type
    * @param value the implicit ConfigLoader instance
    * @return the ConfigLoader instance
    */
  def apply[C](using value: ConfigLoader[C]): ConfigLoader[C] = value

  /** Automatically derives a ConfigLoader for types with a Mirror.
    * This method uses Scala 3 compile-time reflection to automatically derive
    * ConfigLoader instances for case classes (products) and sealed traits (sums).
    * Example:
    * {{{
    * case class DbConfig(
    *   @env("DB_HOST") host: String,
    *   @env("DB_PORT") port: Int
    * ) derives ConfigLoader
    * }}}
    *
    * @tparam A the type to derive a ConfigLoader for
    * @param mirror the Mirror for type A
    * @return a derived ConfigLoader instance
    *
    */
  inline given derived[A](using mirror: Mirror.Of[A]): ConfigLoader[A] =
    inline mirror match
      case sum: Mirror.SumOf[A] => derivedMirrorSum[A](sum)
      case product: Mirror.ProductOf[A] => derivedMirrorProduct[A](product)

  private inline def derivedMirrorProduct[T](
      product: Mirror.ProductOf[T]
  ): ConfigLoader[T] =
    new ConfigHandler[T]((reader, parentFieldPath) =>
      aggregate(
        deriveProduct[
          product.MirroredElemLabels,
          product.MirroredElemTypes
        ](
          fieldMetadata[T],
          reader,
          parentFieldPath
        )
      ).map(v => product.fromProduct(Tuple.fromArray(v.toArray)))
    )

  private inline def deriveProduct[
      Labels <: Tuple,
      Params <: Tuple
  ](
      metadata: Map[String, FieldMetadata],
      reader: ConfigReader,
      parentFieldPath: FieldPath
  ): List[Either[ConfigError, Any]] =
    inline erasedValue[(Labels, Params)] match
      case _: (EmptyTuple, EmptyTuple) =>
        Nil
      case _: (l *: ltail, p *: ptail) =>
        val label = constValue[l].asInstanceOf[String]
        val fieldMetadata = metadata(label)

        val fieldPath = parentFieldPath / label

        val value = summonFrom {
          case loader: ConfigLoader[`p`] =>
            loader match {
              case decoder: ConfigDecoder[_] =>
                if fieldMetadata.annotations.isEmpty then
                  Left(
                    ConfigError
                      .other(
                        fieldPath,
                        s"No annotations found for field: $label"
                      )
                  )
                else
                  reader.read(fieldPath, fieldMetadata.annotations) match {
                    case Right(raw) =>
                      decoder.decode(
                        raw,
                        DecodingContext(fieldMetadata.annotations, fieldPath)
                      )
                    case Left(e) if e.onlyContainsMissing =>
                      fieldMetadata.default match {
                        case Some(d) => Right(d)
                        case None =>
                          inline erasedValue[p] match {
                            case _: Option[_] =>
                              Right(None)
                            case _ =>
                              Left(e)
                          }
                      }
                    case Left(e) => Left(e)
                  }
              case handler: ConfigHandler[_] =>
                handler.load(reader, fieldPath) match {
                  case Right(v) => Right(v)
                  case Left(e) if e.onlyContainsMissing =>
                    fieldMetadata.default match {
                      case Some(d) => Right(d)
                      case None =>
                        inline erasedValue[p] match {
                          case _: Option[_] =>
                            Right(None)
                          case _ =>
                            Left(e)
                        }
                    }
                  case Left(e) => Left(e)
                }
            }
          case _ => decoderError[p]
        }

        value :: deriveProduct[ltail, ptail](metadata, reader, parentFieldPath)

  private inline def deriveSum[
      T,
      Subtypes <: Tuple,
      SuperTypeLabel <: String
  ](
      reader: ConfigReader,
      fieldPath: FieldPath,
      nestedReasons: List[ConfigErrorReason] = Nil
  ): Either[ConfigError, T] =

    inline erasedValue[Subtypes] match
      case _: EmptyTuple =>
        Left(ConfigError(nestedReasons))
      case _: (subtype *: tail) =>
        summonFrom {
          case nestedSum: Mirror.SumOf[`subtype`] =>
            deriveSum[
              `subtype`,
              nestedSum.MirroredElemTypes,
              nestedSum.MirroredLabel
            ](
              reader,
              fieldPath
            ).map(_.asInstanceOf[T])
          case loader: ConfigLoader[`subtype`] =>
            loader.load(reader, fieldPath).map(_.asInstanceOf[T]) match {
              case Right(r) => Right(r)
              case Left(e) if e.onlyContainsMissing =>
                deriveSum[T, tail, SuperTypeLabel](
                  reader,
                  fieldPath,
                  nestedReasons ++ e.reasons
                )
              case Left(value) => Left(value)
            }
          case product: Mirror.ProductOf[`subtype`] =>
            val agg = aggregate(
              deriveProduct[
                product.MirroredElemLabels,
                product.MirroredElemTypes
              ](
                fieldMetadata[subtype],
                reader,
                fieldPath
              )
            ).map(v =>
              product.fromProduct(Tuple.fromArray(v.toArray)).asInstanceOf[T]
            )

            agg match {
              case Right(r) => Right(r)
              case Left(e) if e.onlyContainsMissing =>
                deriveSum[T, tail, SuperTypeLabel](
                  reader,
                  fieldPath,
                  nestedReasons ++ e.reasons
                )
              case Left(value) => Left(value)
            }

        }

  private inline def derivedMirrorSum[T](
      sum: Mirror.SumOf[T]
  ): ConfigLoader[T] =
    inline if isSingletonEnum[T] then
      val cases = enumCases[T]
      val name = typeName[T]

      new EnumConfigDecoder(cases, name)
    else
      new ConfigHandler[T]((reader, parentFieldPath) =>
        deriveSum[T, sum.MirroredElemTypes, sum.MirroredLabel](
          reader,
          parentFieldPath
        )
      )
