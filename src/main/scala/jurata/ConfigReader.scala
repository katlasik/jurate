package jurata

trait ConfigReader {
  def readEnv(name: String): Option[String]
  def readProp(name: String): Option[String]

  private[jurata] def read(annotations: Seq[ConfigAnnotation]): Either[ConfigError, String] = {

    def iterate(values: Seq[ConfigAnnotation]): Either[ConfigError, String] = 

      if values.isEmpty then
        Left(ConfigError.missing(annotations))
      else
        val maybeValue = values.head match
          case env(name) => 
            readEnv(name)
          case prop(name) => readProp(name)

        maybeValue match
          case Some(value) => Right(value)
          case None => iterate(values.tail)

    iterate(annotations)

  }
  
}

case class MapConfigReader(private val envs: Map[String, String], private val props: Map[String, String]) extends ConfigReader:
  def readEnv(name: String): Option[String] = envs.get(name)
  def readProp(name: String): Option[String] = props.get(name)
  
  def onEnv(name: String, value: String): MapConfigReader = MapConfigReader(envs.updated(name, value), props)
  def onProp(name: String, value: String): MapConfigReader = MapConfigReader(envs, props.updated(name, value))

private object LiveConfigReader extends ConfigReader {
    override def readEnv(name: String): Option[String] = sys.env.get(name)
    override def readProp(name: String): Option[String] = sys.props.get(name)
}

object ConfigReader {
  def mocked: MapConfigReader = MapConfigReader(Map.empty, Map.empty)
}