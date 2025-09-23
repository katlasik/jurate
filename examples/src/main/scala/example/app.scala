package example

import jurate.{*, given}

enum Environment {
  case DEV, PROD, STAGING
}

case class DbConfig(
    @env("DB_HOST") @prop("db.host") host: String,
    @env("DB_PORT") @prop("db.port") port: Int,
    @env("DB_USER") @prop("db.user") user: String,
    @env("DB_PASSWORD") @prop("db.password") password: Secret[String]
)

case class Config(
    @env("PORT") port: Int,
    @env("HOST") host: String = "localhost",
    dbConfig: DbConfig,
    @env("ENV") env: Environment
)

@main def simpleApp(): Unit = {

  load[Config] match {
    case Right(config) =>
      println(
        s"Starting app on ${config.host}:${config.port} on env ${config.env}"
      )
    case Left(errors) =>
      println(errors)
  }
}
