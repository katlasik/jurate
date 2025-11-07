package example

import jurate.{*, given}

import java.time.LocalTime

enum Environment {
  case DEV, PROD, STAGING
}

given ConfigDecoder[LocalTime] with {
  override def decode(
      raw: String
  ): Either[String, LocalTime] = try Right(LocalTime.parse(raw))
  catch
    case _: Exception =>
      Left("can't decode LocalTime value")
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
    @env("ENV") env: Environment,
    @env("MAINTENANCE_WINDOW") maintenanceWindow: Option[LocalTime]
)

@main def simpleApp(): Unit = {

  load[Config] match {
    case Right(config) =>
      println(
        s"Starting app on ${config.host}:${config.port} on env ${config.env}"
      )
    case Left(error) =>
      println(error)
  }
}
