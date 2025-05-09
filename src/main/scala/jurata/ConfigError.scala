package jurata

trait ConfigErrorCause {
  def missing: Boolean
}

case class Missing(annotations: Seq[ConfigAnnotation]) extends ConfigErrorCause {
  override def missing: Boolean = true
}

case class Invalid(receivedValue: String, detail: String, annotation: Option[ConfigAnnotation]) extends ConfigErrorCause {
  override def missing: Boolean = false
}

case class Other(detail: String) extends ConfigErrorCause {
  override def missing: Boolean = false
}

case class ConfigError(causes: List[ConfigErrorCause]) extends Exception(ConfigError.createErrorMessage(causes)) {

  infix def ++(other: ConfigError): ConfigError = {
    ConfigError(causes ++ other.causes)
  }

  def onlyContainsMissing: Boolean = {
    causes.forall(_.missing)
  }
  
}

object ConfigError {

  private def createAnnotationMessage(annotations: Seq[ConfigAnnotation]): String = {
    annotations.map {
      case env(name) => s"missing environment variable $name"
      case prop(path) => s"missing system property $path"
    }.mkString(", ")
  }.capitalize

  private def createErrorMessage(causes: List[ConfigErrorCause]): String = {
    "Configuration loading failed with following issues: " ++ causes.map {
      case Missing(annotations) => createAnnotationMessage(annotations)
      case Invalid(receivedValue, detail, annotation) =>

        annotation match {
          case Some(env(name)) => s"Loaded invalid value while reading environment variable: $name: $detail, received value: '$receivedValue'"
          case Some(prop(path)) => s"Loaded invalid value while reading system property: $path: $detail, received value: '$receivedValue'"
          case _ => s"Loaded invalid value: $detail, received value: '$receivedValue'"
        }
      case Other(detail) => detail
    }.mkString("\n", "\n", "")
  }

  def invalid(detail: String, receivedValue: String): ConfigError =
    ConfigError(
      Invalid(receivedValue = receivedValue, detail = detail, annotation = None) :: Nil
    )

  def missing(annotations: Seq[ConfigAnnotation]): ConfigError =
    ConfigError(
      Missing(annotations = annotations) :: Nil
    )

  def other(detail: String): ConfigError = ConfigError(Other(detail) :: Nil)
}