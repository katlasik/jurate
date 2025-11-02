package jurate

import jurate.printers.{DefaultPrinter, TablePrinter}
import jurate.utils.FieldPath.path
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ErrorPrinterSpec extends AnyFlatSpec with Matchers with EitherValues {

  "DefaultPrinter" should "format errors same as getMessage" in {
    val error = ConfigError.missing(path"port", Seq(env("PORT")))
    error.print(using DefaultPrinter) shouldEqual error.getMessage
  }

  "TablePrinter" should "format single error as table" in {
    val error = ConfigError.missing(path"port", Seq(env("PORT")))
    val output = error.print(using TablePrinter)

    val expected = """┌───────┬────────────┬─────────────────────────────┐
                     >│ Field │ Source     │ Message                     │
                     >├───────┼────────────┼─────────────────────────────┤
                     >│ port  │ PORT (env) │ Missing configuration value │
                     >└───────┴────────────┴─────────────────────────────┘"""
      .stripMargin('>')

    output shouldEqual expected
  }

  it should "format multiple errors with different types" in {
    val error1 = ConfigError.missing(path"port", Seq(env("PORT")))
    val error2 =
      ConfigError.invalid(
        path"port",
        "can't decode",
        "bad",
        Some(env("TIMEOUT"))
      )
    val error3 = ConfigError.other(path"custom", "custom message")
    val combined = error1 ++ error2 ++ error3

    val output = combined.print(using TablePrinter)

    val expected = """┌────────┬───────────────┬──────────────────────────────────────────────┐
                     >│ Field  │ Source        │ Message                                      │
                     >├────────┼───────────────┼──────────────────────────────────────────────┤
                     >│ port   │ PORT (env)    │ Missing configuration value                  │
                     >├────────┼───────────────┼──────────────────────────────────────────────┤
                     >│ port   │ TIMEOUT (env) │ Invalid value: can't decode, received: 'bad' │
                     >├────────┼───────────────┼──────────────────────────────────────────────┤
                     >│ custom │               │ custom message                               │
                     >└────────┴───────────────┴──────────────────────────────────────────────┘"""
      .stripMargin('>')

    output shouldEqual expected
  }

  it should "combine multiple annotations in one row" in {
    val error =
      ConfigError.missing(path"database", Seq(env("DB_NAME"), prop("db.name")))
    val output = error.print(using TablePrinter)

    val expected = """┌──────────┬───────────────────────────────┬─────────────────────────────┐
                     >│ Field    │ Source                        │ Message                     │
                     >├──────────┼───────────────────────────────┼─────────────────────────────┤
                     >│ database │ DB_NAME (env), db.name (prop) │ Missing configuration value │
                     >└──────────┴───────────────────────────────┴─────────────────────────────┘"""
      .stripMargin('>')

    output shouldEqual expected
  }

}
