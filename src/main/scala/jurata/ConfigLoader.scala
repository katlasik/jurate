package jurata

import jurata.utils.*
import jurata.utils.Macros.*

import scala.deriving.Mirror
import scala.compiletime.*

final class ConfigLoader[C](val load: ConfigReader => Either[ConfigError, C])

object ConfigLoader:

  def apply[C](using value: ConfigLoader[C]): ConfigLoader[C] = value

  inline given derived[A](using mirror: Mirror.Of[A]): ConfigLoader[A] =
    inline mirror match
      case sum: Mirror.SumOf[A] => derivedMirrorSum[A](sum)
      case product: Mirror.ProductOf[A] => derivedMirrorProduct[A](product)

  private inline def derivedMirrorProduct[T](
      product: Mirror.ProductOf[T]
  ): ConfigLoader[T] =
    new ConfigLoader[T](reader =>
      aggregate(
        deriveProduct[
          product.MirroredElemLabels,
          product.MirroredElemTypes
        ](
          fieldMetadata[T],
          reader
        )
      ).map(v => product.fromProduct(Tuple.fromArray(v.toArray)))
    )

  inline def deriveProduct[
      Labels <: Tuple,
      Params <: Tuple
  ](
      metadata: Map[String, FieldMetadata],
      reader: ConfigReader
  ): List[Either[ConfigError, Any]] =
    inline erasedValue[(Labels, Params)] match
      case _: (EmptyTuple, EmptyTuple) =>
        Nil
      case _: (l *: ltail, p *: ptail) =>
        val label = constValue[l].asInstanceOf[String]
        val fieldMetadata = metadata(label)

        val value = summonFrom {
          case decoder: ConfigDecoder[`p`] =>
            if fieldMetadata.annotations.isEmpty then
              Left(
                ConfigError.other(s"No annotations found for field: $label")
              )
            else
              reader.read(fieldMetadata.annotations) match {
                case Right(raw) => decoder.decode(raw)
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
          case loader: ConfigLoader[`p`] =>
            loader.load(reader) match {
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
          case _ => decoderError[p]
        }

        value :: deriveProduct[ltail, ptail](metadata, reader)

  inline def deriveSum[
      T,
      Subtypes <: Tuple,
      SuperTypeLabel <: String
  ](
      reader: ConfigReader,
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
              reader
            ).map(_.asInstanceOf[T])
          case loader: ConfigLoader[`subtype`] =>
            loader.load(reader).map(_.asInstanceOf[T]) match {
              case Right(r) => Right(r)
              case Left(e) if e.onlyContainsMissing =>
                deriveSum[T, tail, SuperTypeLabel](
                  reader,
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
                reader
              )
            ).map(v =>
              product.fromProduct(Tuple.fromArray(v.toArray)).asInstanceOf[T]
            )

            agg match {
              case Right(r) => Right(r)
              case Left(e) if e.onlyContainsMissing =>
                deriveSum[T, tail, SuperTypeLabel](
                  reader,
                  nestedReasons ++ e.reasons
                )
              case Left(value) => Left(value)
            }

        }

  private inline def derivedMirrorSum[T](
      sum: Mirror.SumOf[T]
  ): ConfigLoader[T] =
    inline if isSingletonEnum[T] then
      error("You need to derive EnumConfigDecoder for singleton enums")
    else
      new ConfigLoader[T](reader =>
        deriveSum[T, sum.MirroredElemTypes, sum.MirroredLabel](reader)
      )
