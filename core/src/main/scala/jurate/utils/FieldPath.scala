package jurate.utils

case class FieldPath(values: List[String]) {
  def /(value: String): FieldPath = FieldPath(values :+ value)

  def dottedPath: String = values.mkString(".")
}

object FieldPath {
  def blank: FieldPath = FieldPath(Nil)

  def apply(values: String*): FieldPath = new FieldPath(values.toList)

  extension (sc: StringContext) {
    def path(args: Any*): FieldPath = {
      val pathString = sc.parts.mkString
      FieldPath(pathString.split("\\.").toList)
    }
  }
}
