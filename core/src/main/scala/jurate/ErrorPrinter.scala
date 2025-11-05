package jurate

/** Trait for formatting ConfigError instances into human-readable strings.
  *
  * Implementations can provide different formatting strategies such as plain text,
  * tables, or JSON. See [[jurate.printers.DefaultPrinter]] and [[jurate.printers.TablePrinter]].
  */
trait ErrorPrinter {
  /** Formats a configuration error into a string.
    *
    * @param error the configuration error to format
    * @return a formatted string representation of the error
    */
  def format(error: ConfigError): String
}
