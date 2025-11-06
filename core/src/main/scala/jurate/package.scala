package jurate

import jurate.utils.{FieldPath, aggregate}

import java.io.File
import java.net.{InetAddress, URI, UnknownHostException}
import java.util.UUID
import scala.annotation.implicitNotFound
import java.nio.file.{InvalidPathException, Path, Paths}
import scala.collection.Factory
import scala.concurrent.duration.{Duration, FiniteDuration}

given [T: ConfigLoader] => ConfigLoader[Option[T]] =
  ConfigLoader[T] match {
    case decoder: ConfigDecoder[_] =>
      new ConfigDecoder[Option[T]] {
        override def decode(
            raw: String,
            ctx: DecodingContext
        ): Either[ConfigError, Option[T]] =
          decoder.decode(raw, ctx).map(Some(_))
      }
    case handler: ConfigHandler[_] =>
      new ConfigHandler[Option[T]]((reader, parentFieldPath) =>
        handler.load(reader, parentFieldPath).map(Some(_))
      )
  }

given [T, C[T] <: Seq[T]](using
    decoder: ConfigLoader[T],
    factory: Factory[T, C[T]],
    eitherFactory: Factory[Either[ConfigError, T], C[Either[ConfigError, T]]]
): ConfigDecoder[C[T]] =
  (raw, ctx) =>
    aggregate(
      raw
        .split(",")
        .map(_.trim)
        .map(decoder.asDecoder.decode(_, ctx))
        .to(eitherFactory)
    )

given ConfigDecoder[String] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, String] = Right(raw)
}

given ConfigDecoder[Short] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, Short] = try Right(raw.toShort)
  catch
    case _: NumberFormatException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          "can't decode short value",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[Int] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, Int] = try Right(raw.toInt)
  catch
    case _: NumberFormatException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode integer",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[Long] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, Long] = try Right(raw.toLong)
  catch
    case _: NumberFormatException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode long",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[Float] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, Float] = try Right(raw.toFloat)
  catch
    case _: NumberFormatException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode float value",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[Double] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, Double] = try Right(raw.toDouble)
  catch
    case _: NumberFormatException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode double value",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[BigDecimal] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, BigDecimal] = try Right(BigDecimal(raw))
  catch
    case _: NumberFormatException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode BigDecimal value",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[BigInt] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, BigInt] = try Right(BigInt(raw))
  catch
    case _: NumberFormatException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode BigInt value",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[InetAddress] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, InetAddress] = try Right(InetAddress.getByName(raw))
  catch
    case e: UnknownHostException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode InetAddress value: ${e.getMessage}",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[UUID] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, UUID] = try Right(UUID.fromString(raw))
  catch
    case e: IllegalArgumentException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode UUID value: ${e.getMessage}",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[Path] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, Path] = try Right(Paths.get(raw))
  catch
    case e: InvalidPathException =>
      Left(
        ConfigError.invalid(
          ctx.fieldPath,
          s"can't decode Path value: ${e.getMessage}",
          raw,
          ctx.evaluatedAnnotation
        )
      )
}

given ConfigDecoder[File] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, File] = Right(
    new File(raw)
  )
}

given ConfigDecoder[Boolean] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, Boolean] =
    raw.toLowerCase match
      case "true" | "yes" | "1" => Right(true)
      case "false" | "no" | "0" => Right(false)
      case _ =>
        Left(
          ConfigError.invalid(
            ctx.fieldPath,
            s"can't decode boolean value",
            raw,
            ctx.evaluatedAnnotation
          )
        )
}

given ConfigDecoder[URI] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, URI] =
    try Right(URI.create(raw))
    catch
      case e: IllegalArgumentException =>
        Left(
          ConfigError.invalid(
            ctx.fieldPath,
            s"can't decode URI value: ${e.getMessage}",
            raw,
            ctx.evaluatedAnnotation
          )
        )
}

given ConfigDecoder[Duration] with {
  override def decode(
      raw: String,
      ctx: DecodingContext
  ): Either[ConfigError, Duration] =
    try Right(Duration.create(raw))
    catch
      case e: NumberFormatException =>
        Left(
          ConfigError.invalid(
            ctx.fieldPath,
            s"can't decode Duration value: ${e.getMessage}",
            raw,
            ctx.evaluatedAnnotation
          )
        )
}

given ConfigDecoder[FiniteDuration] = ConfigDecoder[Duration].emap {
  case (fd: FiniteDuration, _, _) => Right(fd)
  case (_, raw, ctx) =>
    Left(
      ConfigError.invalid(
        ctx.fieldPath,
        s"expected finite duration but got infinite",
        raw,
        ctx.evaluatedAnnotation
      )
    )
}

/** Loads configuration into a case class instance.
  *
  * This is the main entry point for loading configuration. It requires a ConfigLoader
  * instance (typically auto-derived) and a ConfigReader (typically the live reader).
  *
  * Example:
  * {{{
  * case class DatabaseConfig(
  *   @env("DB_HOST") host: String,
  *   @env("DB_PORT") port: Int
  * ) derives ConfigLoader
  *
  * val config: Either[ConfigError, DatabaseConfig] = load[DatabaseConfig]
  * }}}
  *
  * @tparam C the configuration type to load
  * @param loader the ConfigLoader instance, typically auto-derived
  * @param reader the ConfigReader to use for reading configuration sources
  * @return Either a ConfigError or the loaded configuration instance
  */
def load[C](using
    @implicitNotFound(
      "Can't find required givens. Did you forget to use derives for your case classes?"
    ) loader: ConfigLoader[C],
    reader: ConfigReader
): Either[ConfigError, C] = loader.load(reader, FieldPath.root)

/** Default ConfigReader that reads from system environment variables and properties.
 * This instance is automatically available when you import jurate.{*, given}.
 *
 * For testing, use ConfigReader.mocked to provide custom values.
 */
given ConfigReader = LiveConfigReader
