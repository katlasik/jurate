package jurate

sealed trait ConfigErrorReason {
  def missing: Boolean
}

case class Missing(fieldName: String, annotations: Seq[ConfigAnnotation])
    extends ConfigErrorReason {
  override def missing: Boolean = true
}

case class Invalid(
    receivedValue: String,
    detail: String,
    fieldName: Option[String],
    annotation: Option[ConfigAnnotation]
) extends ConfigErrorReason {
  override def missing: Boolean = false
}

case class Other(fieldName: String, detail: String, annotation: Option[ConfigAnnotation] = None) extends ConfigErrorReason {
  override def missing: Boolean = false
}

case class ConfigError(reasons: List[ConfigErrorReason])
    extends Exception(ConfigError.createErrorMessage(reasons)) {


  infix def ++(other: ConfigError): ConfigError = {
    ConfigError(reasons ++ other.reasons)
  }

  private[jurate] def onlyContainsMissing: Boolean = {
    reasons.forall(_.missing)
  }

}

object ConfigError {

  private def createAnnotationMessage(
      annotations: Seq[ConfigAnnotation]
  ): String = {
    annotations
      .map {
        case env(name) => s"missing environment variable $name"
        case prop(path) => s"missing system property $path"
      }
      .mkString(", ")
  }.capitalize

  private def createErrorMessage(reasons: List[ConfigErrorReason]): String = {
    "Configuration loading failed with following issues: " ++ reasons
      .map {
        case Missing(fieldName, annotations) => createAnnotationMessage(annotations)
        case Invalid(receivedValue, detail, fieldName, annotation) =>
          annotation match {
            case Some(env(name)) =>
              s"Invalid value received while reading environment variable $name: $detail, received value: '$receivedValue'"
            case Some(prop(path)) =>
              s"Invalid value received while reading system property $path: $detail, received value: '$receivedValue'"
            case _ =>
              s"Invalid value received: $detail, received value: '$receivedValue'"
          }
        case Other(fieldName, detail, annotations) => detail
      }
      .mkString("\n", "\n", "")
  }

  def invalid(detail: String, receivedValue: String, annotation: Option[ConfigAnnotation]): ConfigError =
    ConfigError(
      Invalid(
        fieldName = None,
        receivedValue = receivedValue,
        detail = detail,
        annotation = annotation
      ) :: Nil
    )

  def missing(fieldName: String, annotations: Seq[ConfigAnnotation]): ConfigError =
    ConfigError(
      Missing(fieldName: String, annotations = annotations) :: Nil
    )

  def other(fieldName: String, detail: String): ConfigError = ConfigError(Other(fieldName, detail) :: Nil)
}
