package jurate.utils

import jurate.*
import scala.collection.Factory

/** Aggregates a collection of Either values, combining errors when multiple
  * failures occur.
  *
  * This function processes a collection of Either values and either:
  *   - Returns Right with all successful values collected, if all are Right
  *   - Returns Left with combined errors using the provided combine function,
  *     if any are Left
  */
private[jurate] def aggregate[T, E, C[T] <: Seq[T]](
    values: C[Either[E, T]],
    combine: (E, E) => E
)(using factory: Factory[T, C[T]]): Either[E, C[T]] =
  values.foldLeft(
    Right(factory.newBuilder.result()): Either[E, C[T]]
  ) {
    case (acc, Right(value)) => acc.map(v => (v :+ value).to(factory))
    case (Left(err), Left(other)) => Left(combine(err, other))
    case (_, Left(err)) => Left(err)
  }
