package jurate

import scala.annotation.StaticAnnotation

private[jurate] sealed trait ConfigAnnotation extends StaticAnnotation

case class env(name: String) extends ConfigAnnotation

case class prop(path: String) extends ConfigAnnotation
