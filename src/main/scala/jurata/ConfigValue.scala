package jurata

import jurata.utils.*
import magnolia1.{CaseClass, Derivation, SealedTrait}

import scala.reflect.ClassTag


trait ConfigValue[C] {
  def isOption: Boolean = false
}

trait ConfigLoader[C] extends ConfigValue[C]:
  def load(reader: ConfigReader): Either[ConfigError, C]

trait ConfigDecoder[C] extends ConfigValue[C]:
  def decode(raw: String): Either[ConfigError, C]

  def contramap[B](f: C => B): ConfigDecoder[B] = decode(_).map(f)

object ConfigDecoder {
  def apply[C](using value: ConfigDecoder[C]): ConfigDecoder[C] = value
}

object ConfigValue extends Derivation[ConfigValue]:

  def apply[C](using value: ConfigValue[C]): ConfigValue[C] = value

  override def split[T](ctx: SealedTrait[ConfigValue.Typeclass, T]): ConfigValue.Typeclass[T] =
    if ctx.isEnum then
      new ConfigDecoder[T]:
        override def decode(raw: String): Either[ConfigError, T] =
          EnumFinder.find(ctx.typeInfo.full, raw)
    else
      new ConfigLoader[T]:
        def load(reader: ConfigReader): Either[ConfigError, T] =
          Left(ConfigError.other(s"You can't load fields using subtypes - only simple enums without fields are supported"))

  override def join[T](ctx: CaseClass[ConfigValue.Typeclass, T]): ConfigValue.Typeclass[T] =

    def extractAnnotations(param: CaseClass.Param[ConfigValue.Typeclass, T]) = param.annotations.collect{
      case c: ConfigAnnotation => c
    }.reverse

    new ConfigLoader[T]:
      override def load(reader: ConfigReader): Either[ConfigError, T] = {
        traverse(
          ctx.parameters.map {
            param =>
              param.typeclass match {
                case decoder: ConfigDecoder[param.PType] =>
                  val annotations = extractAnnotations(param)

                  if annotations.isEmpty then
                    Left(ConfigError.other(s"Couldn't find annotations on field: '${param.label}', not sure how to load value"))
                  else
                    reader.loadFirst(annotations) match
                      case Some(value) =>
                        decoder.decode(value) match
                          case Right(v) => Right(v)
                          case Left(err) => Left(err)
                      case None =>
                        param.default match
                          case Some(v) => Right(v)
                          case None if decoder.isOption => Right(None)
                          case _ => Left(ConfigError.missing(annotations))

                case loader: ConfigLoader[param.PType] => loader.load(reader) match {
                  case Right(v) => Right(v)
                  case Left(err) if err.onlyContainsMissing && loader.isOption => Right(None)
                  case Left(err) => Left(err)
                }
              }

          }).map(ctx.rawConstruct)
      }
