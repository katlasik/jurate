import jurata.{*, given}

enum Environment:
  case DEV, PROD, STAGING

case class DbConfig(
  @env("DB_PASSWORD") password: Secret[String],
  @env("DB_USERNAME") username: String
) 

case class Config(
  @env("HOST") host: String,
  @env("PORT") port: Int = 8080,
  @env("???") dbConfig: DbConfig
)

@main
def main = 
    println(load[Config])
    //println(config) // Right(DbConfig(*****, user))

