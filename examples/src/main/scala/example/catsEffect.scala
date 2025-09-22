package example

import cats.effect.{IO, IOApp}
import jurate.{*, given}

case class AppConfig(
    @env("APP_NAME") name: String = "CatsEffectApp",
    @env("APP_VERSION") version: String
)

object CatsEffect extends IOApp.Simple {
  def run: IO[Unit] = for {
    config <- IO.fromEither(load[AppConfig])
    _ <- IO.println(
      s"Starting ${config.name} version ${config.version}. Press ENTER to exit."
    )
    _ <- IO.readLine
  } yield ()
}
