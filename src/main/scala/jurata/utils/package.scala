package jurata.utils

import jurata.*

import scala.reflect.ClassTag

private[jurata] def traverse[T: ClassTag](
    values: Seq[Either[ConfigError, T]]
): Either[ConfigError, Seq[T]] =
  values.foldLeft(Right(Seq.empty[T]): Either[ConfigError, Seq[T]]) {
    case (acc, Right(value)) => acc.map(_ :+ value)
    case (Left(err), Left(other)) => Left(err ++ other)
    case (_, Left(err)) => Left(err)
  }
