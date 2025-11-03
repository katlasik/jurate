
# Jurate

[![Maven Central](https://maven-badges.sml.io/maven-central/io.github.katlasik/jurate_3/badge.svg)](https://maven-badges.sml.io/maven-central/io.github.katlasik/jurate_3)
## Intro

Jurate is a simple library for instantiating case class instances from environment variables and system properties. You just need to create a case class with the desired fields and annotate them with `@env` or `@prop`. Then you can load your config using `load` method.

```scala
import jurate.{*, given}

case class DbConfig(
  @env("DB_PASSWORD") password: Secret[String],
  @env("DB_USERNAME") username: String
)

case class Config(
  @env("HOST") host: String,
  @env("PORT") port: Int = 8080,
  @env("ADMIN_EMAIL") adminEmail: Option[String],
  @prop("app.debug") debug: Boolean,
  dbConfig: DbConfig
)

load[Config] // Right(Config(localhost, 8080, None, true, DbConfig(*****, user)))
```

You have to import givens using:

```scala
import jurate.{*, given}
```

This provides instance of `ConfigReader` which is required to load values from environment or system properties.

## Usage

To load a value into a field from an environment variable, use the `@env` annotation. To load a value from a system property, use the `@prop` annotation.
You can provide multiple annotations to a field. The library will try to load the value from the first annotation on the left, and if it fails, it will try the next one.
You can also provide a default value for a field, which will be used if the value is not found for any of the annotations.

```scala
case class Config(
  @prop("debug.email") @env("EMAIL") @env("ADMIN_EMAIL") email: String = "foo@bar.com"
)
```

In this example library will first check if system property `debug.email` exists, then it will look for environment variables EMAIL and ADMIN_EMAIL. If none are found default value `foo@bar.com` will be used.

## Optional values
You can make field optional by using `Option` type. If the value is not found, the field will be set to `None`.

```scala
case class Config(
  @env("ADMIN_EMAIL") adminEmail: Option[String],
)
```

## Nested case classes
You can use nested case classes to organize your config.

```scala
case class DbConfig(
  @env("DB_PASSWORD") password: Secret[String],
  @env("DB_USERNAME") username: String
) 

case class Config(
  @env("HOST") host: String,
  @env("PORT") port: Int = 8080,
  dbConfig: DbConfig
)
```

## Enums
You can load values of singleton enums (with no fields) using `@env` or `@prop` annotations. The library will automatically convert the loaded value to the enum case. Searching for the right enum case is case-sensitive.

```scala
enum Environment:
  case DEV, PROD, STAGING

case class Config(
  @env("ENV") env: Environment
)
```

If you want to customize loading of enum you can provide your own instance of `ConfigDecoder`:

```scala
given ConfigDecoder[Environment] = new ConfigDecoder[Environment]:
  def decode(raw: String): Either[ConfigError, Environment] = 
    val rawLowercased = raw.trim().toLowerCase()
    Environment.values
      .find(_.toString().toLowerCase() == rawLowercased)
      .toRight(ConfigError.invalid(s"Couldn't find right value for Environment", raw))
```

## Subclasses
The result of loading sealed trait will be first subclass to load successfully.

```scala
sealed trait MessagingConfig
case class LiveConfig(@env("BROKER_ADDRESS") brokerAddress: String) extends MessagingConfig
case class TestConfig(@prop("BROKER_NAME" ) brokerName: String) extends MessagingConfig

case class Config(messaging: MessagingConfig)
```

The same works for enums with fields
```scala
enum MessagingConfig: 
  case LiveConfig(@env("BROKER_ADDRESS") brokerAddress: String)
  case TestConfig(@prop("BROKER_NAME" ) brokerName: String)

case class Config(messaging: MessagingConfig)
```

## Secret values
If don't want to expose your secret values in logs or error messages, you can use `Secret` type. It will hide the value when printed.

```scala
case class DbConfig(
  @env("DB_PASSWORD") password: Secret[String],
  @env("DB_USERNAME") username: String
)

val config = load[DbConfig]

println(config) // Right(DbConfig(*****, user))
```

## Collection
You can load comma-separated values into collections. Currently it's not possible to change separator.

```scala
case class Numbers(@env("NUMBERS") numbers: List[Int])

val result = load[Numbers]
```

With environment variable containing `"1,2,3"` the `result` will contain `Right(Numbers(List(1,2,3)))`.

# Adding custom decoders
You can add custom decoders for your types by implementing `ConfigDecoder` typeclass.


```scala
class MyClass(val value: String)

given ConfigDecoder[MyClass] with {
  def decode(value: String): Either[ConfigError, MyClass] = {
    if (value.isEmpty) Left(ConfigError.invalid("Value is empty", value))
    else Right(new MyClass(value))
  }
}
```

Library provides default decoders for common types like `UUID`, `Path`, `URI` etc.

# Testing

You can override behavior of `load` function by providing instance of `ConfigReader`.

For test you can use mocked ConfigReader:


```scala
case class DbConfig(@env("DATABASE_HOST") host: String, @prop("dbpass") password: String)

given ConfigReader = ConfigReader
  .mocked
  .onEnv("DATABASE_HOST", "localhost")
  .onProp("dbpass", "mypass")

load[DbConfig] // Right(DbConfig("localhost", "mypass"))

```

# Error Handling

Configuration loading returns an `Either[ConfigError, Config]`. When errors occur, you can format them for display using different printers.

## Default Error Format

By default, errors use `getMessage` which provides a text-based error list:

```scala
case class Config(
  @env("PORT") port: Int,
  @env("HOST") host: String
)

load[Config] match {
  case Left(error) =>
    println(error.getMessage)
    // Configuration loading failed with following issues:
    // Missing environment variable PORT
    // Missing environment variable HOST
  case Right(config) => // ...
}
```

## Table Format

For better readability, use `TablePrinter` to display errors in a formatted table:

```scala
import jurate.printers.TablePrinter

load[Config] match {
  case Left(error) =>
    System.err.println(error.print(using TablePrinter))
    // ┌───────┬────────────┬─────────────────────────────┐
    // │ Field │ Source     │ Message                     │
    // ├───────┼────────────┼─────────────────────────────┤
    // │ port  │ PORT (env) │ Missing configuration value │
    // ├───────┼────────────┼─────────────────────────────┤
    // │ host  │ HOST (env) │ Missing configuration value │
    // └───────┴────────────┴─────────────────────────────┘
  case Right(config) => // ...
}
```

## Custom Error Printers

You can create custom error formatters by implementing the `ErrorPrinter` trait:

```scala
import jurate.printers.ErrorPrinter

object CompactPrinter extends ErrorPrinter {
  def format(error: ConfigError): String =
    error.reasons.map {
      case Missing(field, _) => s"Missing: $field"
      case Invalid(_, detail, _, _) => s"Invalid: $detail"
      case Other(field, detail, _) => s"Error: $detail"
    }.mkString(" | ")
}

// Usage
error.print(using CompactPrinter)
// Output: Missing: port | Missing: host
```

# Examples
You can find more examples under [src/examples](./src/examples/scala). 
You can run them using `sbt "examples/runMain <example-class>"` command (set necessary environment variables first). For instance:

```bash
sbt "examples/runMain jurate.simpleApp"
```