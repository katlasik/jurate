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

  inline def fieldMetadata[A] = ${
    fieldMetadataImpl[A]
  }

  inline def typeMetadata[A] = ${
    typeMetadataImpl[A]
  }

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

  private def typeMetadataImpl[A: Type](using
      quotes: Quotes
  ): Expr[TypeMetadata[A]] =
    import quotes.reflect.*

    val typeSymbol = TypeRepr.of[A].typeSymbol

    val annotations = Expr.ofList(
      typeSymbol.annotations
        .filter(filterAnnotation)
        .map(_.asExpr.asExprOf[jurata.ConfigAnnotation])
        .reverse
    )

    val companion = Ref(typeSymbol.companionModule)

    val valuesMethod =
      typeSymbol.companionModule.methodMember("values").headOption

    val enumCases: Expr[Option[Array[A]]] = toExprOpt(
      valuesMethod.map { m =>
        Select.unique(companion, m.name).asExprOf[Array[A]]
      }
    )

    val name = Expr(typeSymbol.name)

    '{
      TypeMetadata[A]($enumCases, $annotations, $name)
    }
}
