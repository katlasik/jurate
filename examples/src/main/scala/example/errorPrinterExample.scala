package example

import jurate.printers.TablePrinter
import jurate.{*, given}

object ErrorPrinterExample extends App {

  case class AppConfig(
      @env("APP_NAME") appName: String,
      dbConfig: DatabaseConfig
  )

  case class DbCredentials(
      @env("DB_USER") username: String,
      @env("DB_PASS") password: Secret[String]
  )

  case class DatabaseConfig(
      @env("DB_HOST") host: String,
      @env("DB_PORT") port: Int,
      @env("DB_NAME") @prop("db.name") database: String,
      credentials: DbCredentials
  )

  val result = load[AppConfig]

  result match {
    case Right(config) =>
      println("Configuration loaded successfully!")
      println(config)

    case Left(error) =>
      println("Configuration loading failed:\n")
      println(error.print(using TablePrinter))
      println()
  }
}
