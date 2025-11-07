package jurate.utils

/** Represents a path to a field in a configuration hierarchy.
  *
  * Used for tracking the location of configuration fields in nested structures,
  * which helps provide meaningful error messages.
  */
private[jurate] case class FieldPath(values: List[String]) {

  def /(value: String): FieldPath = {
    assert(value.trim.nonEmpty)
    FieldPath(values :+ value)
  }

  def dottedPath: String = values.mkString(".")
}

object FieldPath {

  def root: FieldPath = FieldPath(Nil)

  def apply(values: String*): FieldPath = new FieldPath(values.toList)

  extension (sc: StringContext) {
    def path(args: Any*): FieldPath = {
      val pathString = sc.parts.mkString
      FieldPath(pathString.split("\\.").toList)
    }
  }
}
