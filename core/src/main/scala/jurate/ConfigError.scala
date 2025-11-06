package jurate

import jurate.utils.FieldPath

/**
 * Base trait for different types of configuration error reasons.
 */
sealed trait ConfigErrorReason {
  /**
   * Returns true if this error is due to a missing configuration value.
   */
  def missing: Boolean
}

/**
 * Represents an error where a required configuration value was not found.
 *
 * @param fieldPath the path to the field that was missing
 * @param annotations the annotations that were tried to find the value
 */
case class Missing(fieldPath: FieldPath, annotations: Seq[ConfigAnnotation])
    extends ConfigErrorReason {
  override def missing: Boolean = true
}

/**
 * Represents an error where a configuration value was found but could not be decoded.
 *
 * @param receivedValue the raw string value that was received
 * @param detail additional details about why decoding failed
 * @param fieldPath the path to the field that failed to decode
 * @param annotation the annotation that provided the value
 */
case class Invalid(
    receivedValue: String,
    detail: String,
    fieldPath: FieldPath,
    annotation: ConfigAnnotation
) extends ConfigErrorReason {
  override def missing: Boolean = false
}

/**
 * Represents other configuration errors.
 *
 * @param fieldPath the path to the field where the error occurred
 * @param detail details about the error
 * @param annotation optionally the annotation related to the error
 */
case class Other(
    fieldPath: FieldPath,
    detail: String,
    annotation: Option[ConfigAnnotation] = None
) extends ConfigErrorReason {
  override def missing: Boolean = false
}

/**
 * Configuration loading error containing one or more error reasons.
 *
 * @param reasons the list of error reasons that caused this configuration error
 */
case class ConfigError(reasons: List[ConfigErrorReason])
    extends Exception(ConfigError.createErrorMessage(reasons)) {

  /**
   * Combines this error with another error, merging their reasons.
   *
   * @param other the other error to combine with
   * @return a new ConfigError containing all reasons from both errors
   */
  infix def ++(other: ConfigError): ConfigError = {
    ConfigError(reasons ++ other.reasons)
  }

  // Does this error only contain missing value errors? - in this case we can succeed if field is optional
  private[jurate] def onlyContainsMissing: Boolean = {
    reasons.forall(_.missing)
  }

  /**
   * Formats this error using the provided error printer.
   *
   * @param printer the error printer to use for formatting
   * @return the formatted error message
   */
  def print(using printer: ErrorPrinter): String = printer.format(this)

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
        case Missing(fieldName, annotations) =>
          createAnnotationMessage(annotations)
        case Invalid(receivedValue, detail, fieldName, annotation) =>
          annotation match {
            case env(name) =>
              s"Invalid value received while reading environment variable $name: $detail, received value: '$receivedValue'"
            case prop(path) =>
              s"Invalid value received while reading system property $path: $detail, received value: '$receivedValue'"
          }
        case Other(fieldName, detail, annotations) => detail
      }
      .mkString("\n", "\n", "")
  }

  /**
   * Creates a configuration error for an invalid value.
   *
   * @param fieldPath the path to the field
   * @param detail details about why the value is invalid
   * @param receivedValue the raw value that was received
   * @param annotation the annotation that provided the value
   * @return a ConfigError with an Invalid reason
   */
  def invalid(
      fieldPath: FieldPath,
      detail: String,
      receivedValue: String,
      annotation: ConfigAnnotation
  ): ConfigError =
    ConfigError(
      Invalid(
        fieldPath = fieldPath,
        receivedValue = receivedValue,
        detail = detail,
        annotation = annotation
      ) :: Nil
    )

  /**
   * Creates a configuration error for a missing value.
   *
   * @param fieldPath the path to the field
   * @param annotations the annotations that were tried
   * @return a ConfigError with a Missing reason
   */
  def missing(
      fieldPath: FieldPath,
      annotations: Seq[ConfigAnnotation]
  ): ConfigError =
    ConfigError(
      Missing(fieldPath, annotations = annotations) :: Nil
    )

  /**
   * Creates a configuration error for other types of errors.
   *
   * @param fieldPath the path to the field
   * @param detail details about the error
   * @return a ConfigError with an Other reason
   */
  def other(fieldPath: FieldPath, detail: String): ConfigError = ConfigError(
    Other(fieldPath, detail) :: Nil
  )
}
