package example

import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer
import jurata.{*, given}
import ox.*
import ox.either.*
import scala.io.StdIn.readLine

case class HttpConfig(
  @env("HTTP_HOST") host: String = "localhost",
  @env("HTTP_PORT") port: Int = 8080
)

@main def helloWorldTapir(): Unit = supervised:

  val config: HttpConfig = load[HttpConfig].orThrow

  val helloEndpoint = endpoint
    .get
    .in("hello")
    .in(query[String]("name"))
    .out(stringBody)
    .handleSuccess(name => s"Hello, $name!")

  println(s"Starting server on ${config.host}:${config.port}")

  val handle = NettySyncServer()
    .port(config.port)
    .host(config.host)
    .addEndpoint(helloEndpoint)
    .start()

  println("Press ENTER to stop the server")
  readLine()
  println("Stopping server...")
  handle.stop()
  println("Server stopped.")

