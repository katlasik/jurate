package jurate.printers

import jurate.{ConfigError, ErrorPrinter}

/** A simple error printer that uses the default error message format.
  *
  * This printer returns the basic string representation of configuration errors.
  */
object DefaultPrinter extends ErrorPrinter {
  /** Formats a configuration error using its default message format.
    *
    * @param error the configuration error to format
    * @return the error's default message string
    */
  override def format(error: ConfigError): String = error.getMessage
}
