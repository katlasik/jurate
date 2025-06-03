package jurata

import jurata.utils.*
import jurata.utils.Macros.*

import scala.deriving.Mirror
import scala.compiletime.*
import scala.annotation.meta.field


trait ConfigValue[C]

trait ConfigLoader[C] extends ConfigValue[C]:
  def load(reader: ConfigReader): Either[ConfigError, C]

trait ConfigDecoder[C] extends ConfigValue[C]:
  def decode(raw: String): Either[ConfigError, C]

  def contramap[B](f: C => B): ConfigDecoder[B] = decode(_).map(f)

object ConfigDecoder {
  def apply[C](using value: ConfigDecoder[C]): ConfigDecoder[C] = value
}

private class SimpleConfigLoader[T](read: ConfigReader => Either[ConfigError, T]) extends ConfigLoader[T] {
  override def load(reader: ConfigReader): Either[ConfigError, T] = read(reader)
}

object ConfigValue:

  def apply[C](using value: ConfigValue[C]): ConfigValue[C] = value

  inline given derived[A](using mirror: Mirror.Of[A]): ConfigValue[A] =
      inline mirror match
        case sum: Mirror.SumOf[A] => derivedMirrorSum[A](sum)
        case product: Mirror.ProductOf[A] => derivedMirrorProduct[A](product)

  private inline def derivedMirrorProduct[T](
    product: Mirror.ProductOf[T]
  ): ConfigLoader[T] = 

    val reader = summonInline[ConfigReader]

    val read: ConfigReader => Either[ConfigError, T] = reader =>
      traverse(
          deriveProduct[
            product.MirroredElemLabels,
            product.MirroredElemTypes
          ](
            fieldMetadata[T],
            reader
          )
        )
        .map(v => Tuple.fromArray(v.toArray))
        .map(product.fromProduct)


    new SimpleConfigLoader[T](read)

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
        case _: ((l *: ltail), (p *: ptail)) =>
          val label = constValue[l].asInstanceOf[String]
          val fieldMetadata = metadata(label)

          val value = summonInline[ConfigValue[p]] match {
            case decoder: ConfigDecoder[p] => 
                if fieldMetadata.annotations.isEmpty then
                  Left(
                    ConfigError.other(s"No annotations found for field: ${label}")
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

            case loader: ConfigLoader[p] => 
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
          }

          value :: deriveProduct[ltail, ptail](metadata, reader)

  inline def deriveSum[
    T, 
    Subtypes <: Tuple,
    SuperTypeLabel <: String
  ](
    meta: Map[String, FieldMetadata],
    typeMeta: TypeMetadata[T],
    reader: ConfigReader,
    nestedReasons: List[ConfigErrorReason] = Nil
  ): Either[ConfigError, T] = 
    
    val label = constValue[SuperTypeLabel].asInstanceOf[String]

    inline erasedValue[Subtypes] match
      case _: EmptyTuple =>
        Left(ConfigError(nestedReasons))
      case _: (subtype *: tail) => 
        summonFrom {
          case nestedSum: Mirror.SumOf[`subtype`] => 
            deriveSum[`subtype`, nestedSum.MirroredElemTypes, nestedSum.MirroredLabel](
              meta,
              typeMetadata[`subtype`],
              reader
            ).map(_.asInstanceOf[T])
          case configValue: ConfigValue[`subtype`] => 
            configValue match {
              case loader: ConfigLoader[`subtype`] => 
                  loader.load(reader).map(_.asInstanceOf[T]) match {
                    case Right(r) => Right(r)
                    case Left(e) if e.onlyContainsMissing => 
                      deriveSum[T, tail, SuperTypeLabel](meta, typeMeta, reader, nestedReasons ++ e.reasons)
                    case Left(value) => Left(value)
              }
            }
          case product: Mirror.ProductOf[`subtype`] => 
            traverse(
              deriveProduct[product.MirroredElemLabels, product.MirroredElemTypes](
                fieldMetadata[subtype], reader
              )
            )
            .map(v => product.fromProduct(Tuple.fromArray(v.toArray)).asInstanceOf[T]) match {
              case Right(r) => Right(r)
              case Left(e) if e.onlyContainsMissing => 
                deriveSum[T, tail, SuperTypeLabel](meta, typeMeta, reader, nestedReasons ++ e.reasons)
              case Left(value) => Left(value)
            }
          
        }

  private inline def derivedMirrorSum[T](sum: Mirror.SumOf[T]): ConfigValue[T] = 
    val typeMeta = typeMetadata[T]

    typeMeta.enumCases match
      case Some(values) => new EnumConfigDecoder(values)
      case _ => 
        val read: ConfigReader => Either[ConfigError, T] = reader =>
          deriveSum[T, sum.MirroredElemTypes, sum.MirroredLabel](
            fieldMetadata[T],
            typeMetadata[T],
            reader
          )

        new SimpleConfigLoader[T](read)
            

    
    

       
    

      


