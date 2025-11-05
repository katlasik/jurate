package jurate

import scala.annotation.StaticAnnotation

private[jurate] sealed abstract class ConfigAnnotation extends StaticAnnotation

/**
 * Annotation to load configuration from an environment variable.
 *
 * @param name the name of the environment variable
 */
case class env(name: String) extends ConfigAnnotation

/**
 * Annotation to load configuration from a system property.
 *
 * @param path the path of the system property
 */
case class prop(path: String) extends ConfigAnnotation
