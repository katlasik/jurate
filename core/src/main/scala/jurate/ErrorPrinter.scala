package jurate

trait ErrorPrinter {
  def format(error: ConfigError): String
}
