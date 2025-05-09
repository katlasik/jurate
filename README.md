# Intro
Jurata is simple library for instantiating case class instances from environment variables or system properties. 
You just need to create a case class with the desired fields and annotate them with `@env` or `@prop`. Then use `derives keyword` to derive typeclass `ConfigValue` for your case class.
Finally, you can load your config using `load` method. The library will take care of the rest.

```scala
case class DbConfig (
  @env("DB_PASSWORD") password: Secret[String],
  @env("DB_USERNAME") username: String
) derives ConfigValue

case class Config(
  @env("HOST") host: String,
  @env("PORT") port: Int = 8080,
  @env("ADMIN_EMAIL") adminEmail: Option[String],          
  @prop("app.debug") debug: Boolean,
  dbConfig: DbConfig
) derives ConfigValue

load[Config] //Right(Config(localhost,8080,None,true, DbConfig(*****, user)))
```

# Loading config values
Currently, library only supports loading values from environment variables or system properties. To load value into field from environment variable, use `@env` annotation. To load value from system property, use `@prop` annotation.

```scala


# Fallbacks
You can provide multiple annotations to a field. The library will try to load the value from the first annotation on the left, and if it fails, it will try the next one.
You can also provide default value for a field which will be used if the value is not found for any of the annotations.

```scala
case class Config(
   @prop("debug.email") @env("EMAIL") @env("ADMIN_EMAIL") email: String = "noreply@mydomain.com"
) derives ConfigValue
```

In this example library will first check if system property `debug.email` exists, then it will look for environment variables EMAIL and ADMIN_EMAIL. If non are found default value 
`noreply@mydomain.com` will be used.

# Optional values
You can make field optional by using `Option` type. If the value is not found, the field will be set to `None`.

```scala
case class Config(
  @env("ADMIN_EMAIL") adminEmail: Option[String],
) derives ConfigValue
```

# Nested case classes
You can use nested case classes to organize your config. Every nested case class has to derive typeclass `ConfigValue`.

```scala
case class DbConfig (
  @env("DB_PASSWORD") password: Secret[String],
  @env("DB_USERNAME") username: String
) derives ConfigValue 

case class Config (
  @env("HOST") host: String,
  @env("PORT") port: Int = 8080,
  dbConfig: DbConfig
) derives ConfigValue
```

# Secret values
If don't want to expose your secret values in logs or error messages, you can use `Secret` type. It will hide the value when printed.

```scala
case class DbConfig (
  @env("DB_PASSWORD") password: Secret[String],
  @env("DB_USERNAME") username: String
) derives ConfigValue


val config = load[DbConfig]

println(config) // Right(DbConfig(*****, user))

```

# Adding custom decoders
You can add custom decoders for your types by implementing `ConfigDecoder` typeclass.


```scala
class MyClass(val value: String)

given ConfigDecoder[MyClass] with {
  def decode(value: String): Either[String, MyClass] = {
    if (value.isEmpty) Left("Value is empty")
    else Right(new MyClass(value))
  }
}
```


# Enums
You can load values of enums using `@env` or `@prop` annotations. The library will automatically convert the string value to the enum value.

```scala
enum Environment {
  case DEV, PROD, STAGING
}

case class Config(
  @env("ENV") env: Environment
) derives ConfigValue
```
