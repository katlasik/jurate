package jurata.utils

import jurata.*
import scala.collection.Factory

private[jurata] def aggregate[T, C[T] <: Seq[T]](
    values: C[Either[ConfigError, T]]
)(using factory: Factory[T, C[T]]): Either[ConfigError, C[T]] =
  values.foldLeft(Right(factory.newBuilder.result()): Either[ConfigError, C[T]]) {
    case (acc, Right(value)) => acc.map(v => (v :+ value).to(factory))
    case (Left(err), Left(other)) => Left(err ++ other)
    case (_, Left(err)) => Left(err)
  }
