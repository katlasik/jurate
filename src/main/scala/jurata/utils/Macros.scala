package jurata.utils

import scala.quoted.Quotes
import scala.quoted.Type
import scala.quoted.Expr
import jurata.ConfigAnnotation

private[jurata] case class FieldMetadata(
    annotations: List[ConfigAnnotation],
    default: Option[Any]
)

private[jurata] case class TypeMetadata[T](
    enumCases: Option[Array[T]],
    annotations: List[ConfigAnnotation],
    name: String
)

private[jurata] object Macros {

  private def filterAnnotation(using
      quotes: Quotes
  )(a: quotes.reflect.Term): Boolean =
    import quotes.reflect.*
    scala.util
      .Try(a.tpe <:< TypeRepr.of[jurata.ConfigAnnotation])
      .toOption
      .contains(true)

  private def toExprOpt[T: Type](using
      quotes: Quotes
  )(optExpr: Option[Expr[T]]): Expr[Option[T]] =
    optExpr match
      case Some(e) => '{ Some($e) }
      case None => '{ None }

  inline def isSingletonEnum[A] = ${
    isSingletonEnumImpl[A]
  }

  private def isSingletonEnumImpl[A: Type](using
      quotes: Quotes
  ): Expr[Boolean] = {
    import quotes.reflect.*

    val result =
      TypeRepr.of[A].typeSymbol.companionModule.methodMember("values").nonEmpty

    Expr(result)
  }

  inline def enumCases[C]: Array[C] = ${
    enumCasesImpl[C]
  }

  private def enumCasesImpl[C: Type](using quotes: Quotes): Expr[Array[C]] = {
    import quotes.reflect.*

    val typeSymbol = TypeRepr.of[C].typeSymbol

    val valuesMethod =
      typeSymbol.companionModule
        .methodMember("values")
        .headOption
        .getOrElse(
          report.errorAndAbort("Couldn't find values method, is this Enum?")
        )

    val companion = Ref(typeSymbol.companionModule)

    Select.unique(companion, valuesMethod.name).asExprOf[Array[C]]
  }

  inline def decoderError[T] = ${
    decoderErrorImpl[T]
  }

  private def decoderErrorImpl[T: Type](using
      quotes: Quotes
  ) = {
    import quotes.reflect.*

    val typeName = TypeRepr.of[T].typeSymbol.name

    report.errorAndAbort(s"Couldn't find decoder for type $typeName")
  }

  inline def fieldMetadata[A] = ${
    fieldMetadataImpl[A]
  }

  private def fieldMetadataImpl[A: Type](using
      quotes: Quotes
  ): Expr[Map[String, FieldMetadata]] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[A]

    def default(fieldIdx: Int): Option[Expr[Any]] =
      tpe.typeSymbol.companionClass
        .declaredMethod(s"$$lessinit$$greater$$default$$${fieldIdx + 1}")
        .headOption
        .map { defaultMethod =>
          val callDefault = {
            val base = Ident(tpe.typeSymbol.companionModule.termRef)
              .select(defaultMethod)
            val tParams =
              defaultMethod.paramSymss.headOption.filter(_.forall(_.isType))
            tParams match
              case Some(tParams) => TypeApply(base, tParams.map(TypeTree.ref))
              case _ => base
          }

          defaultMethod.tree match {
            case tree: DefDef => tree.rhs.getOrElse(callDefault)
            case _ => callDefault
          }
        }
        .map(_.asExprOf[Any])

    val fields = tpe.typeSymbol.primaryConstructor.paramSymss.flatten

    val values = Expr.ofList(
      fields.zipWithIndex.map { case (field, fieldIdx) =>
        Expr.ofTuple(
          Expr(field.name) -> {

            val annotations = Expr.ofList(
              field.annotations
                .filter(filterAnnotation)
                .map(_.asExpr.asExprOf[jurata.ConfigAnnotation])
                .reverse
            )

            val defaultValue = toExprOpt(default(fieldIdx))

            '{
              FieldMetadata(
                $annotations,
                $defaultValue
              )
            }
          }
        )
      }
    )

    '{
      Map.from($values)
    }

  inline def typeName[A] = ${
    typeNameImpl[A]
  }

  private def typeNameImpl[A: Type](using
      quotes: Quotes
  ): Expr[String] =
    import quotes.reflect.*
    Expr(TypeRepr.of[A].typeSymbol.name)

}
