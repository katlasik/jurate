package jurate.printers

import jurate.{ConfigError, ErrorPrinter}

object DefaultPrinter extends ErrorPrinter {
  override def format(error: ConfigError): String = error.getMessage
}