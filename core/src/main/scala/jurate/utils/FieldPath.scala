package jurate.utils

/** Represents a hierarchical path to a configuration field.
  *
  * FieldPath is used to track the location of configuration values in nested structures,
  * which helps provide detailed error messages showing exactly which field failed to load.
  *
  * @param values the list of path segments from root to leaf
  */
case class FieldPath(values: List[String]) {
  /** Appends a segment to the path.
    *
    * @param value the path segment to append
    * @return a new FieldPath with the appended segment
    */
  def /(value: String): FieldPath = {
    assert(value.trim.nonEmpty)
    FieldPath(values :+ value)
  }

  /** Converts the path to a dot-separated string.
    *
    * @return the path as a string like "parent.child.field"
    */
  def dottedPath: String = values.mkString(".")
}

/** Companion object for FieldPath. */
object FieldPath {
  /** Creates an empty FieldPath representing the root.
    *
    * @return an empty FieldPath
    */
  def root: FieldPath = FieldPath(Nil)

  /** Creates a FieldPath from a varargs list of segments.
    *
    * @param values the path segments
    * @return a new FieldPath
    */
  def apply(values: String*): FieldPath = new FieldPath(values.toList)

  /** String interpolator extension for creating FieldPaths.
    *
    * Allows creating paths using string interpolation syntax like: path"parent.child.field"
    */
  extension (sc: StringContext) {
    /** Creates a FieldPath from a dot-separated string.
      *
      * @param args interpolation arguments (unused)
      * @return a new FieldPath parsed from the string
      */
    def path(args: Any*): FieldPath = {
      val pathString = sc.parts.mkString
      FieldPath(pathString.split("\\.").toList)
    }
  }
}
