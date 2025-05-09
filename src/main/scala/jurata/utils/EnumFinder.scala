package jurata.utils

import jurata.ConfigError


private[jurata] object EnumFinder:

  def find[T](fullType: String, value: String): Either[ConfigError, T] =

    def getCompanionClass: Either[ConfigError, Class[?]] =
        CompanionFinder.getClass(fullType)
          .toRight(ConfigError.other(s"Couldn't find companion class of enum $fullType"))

    def accessCompanion(companionClass: Class[?]): Either[ConfigError, AnyRef] =
      try Right(companionClass.getDeclaredField("MODULE$").get(null))
      catch
        case _: NoSuchFieldException =>
          Left(ConfigError.other(s"Couldn't find companion object of enum $companionClass - make sure enum is declared either as toplevel object or inside another object"))

    def getValues[T](companion: AnyRef): Either[ConfigError, Array[T]] =

      try
        Right(companion.getClass.getMethod("values").invoke(companion).asInstanceOf[Array[T]])
      catch
        case e: NoSuchMethodException =>
          Left(ConfigError.other(s"Couldn't decode enum $fullType - only decoding of enums without fields is supported"))

    def selectValue[T](values: Array[T], value: String): Either[ConfigError, T] =
      val lowercased = value.toLowerCase
      values
        .find(_.toString.toLowerCase == lowercased)
        .toRight(ConfigError.invalid(s"Couldn't find case $value in enum $fullType", value))

    for {
      companionClass <- getCompanionClass
      companion <- accessCompanion(companionClass)
      values <- getValues[T](companion)
      value <- selectValue(values, value)
    } yield value


