package jurate

import jurate.utils.FieldPath

import scala.annotation.tailrec

/** Abstraction for reading configuration from environment variables and system properties.
  *
  * ConfigReader provides methods to read from different configuration sources.
  * Use [[LiveConfigReader]] for production and [[MapConfigReader]] for testing.
  */
trait ConfigReader {
  /** Reads an environment variable by name.
    *
    * @param name the environment variable name
    * @return Some(value) if found, None otherwise
    */
  def readEnv(name: String): Option[String]

  /** Reads a system property by name.
    *
    * @param name the system property name
    * @return Some(value) if found, None otherwise
    */
  def readProp(name: String): Option[String]

  /** Attempts to read configuration from annotations in left-to-right order,
   * returning the first successful read. This enables fallback patterns like:
   * @env("PRIMARY") @env("FALLBACK") @prop("default.value")
   */
  private[jurate] def read(
      fieldPath: FieldPath,
      annotations: Seq[ConfigAnnotation]
  ): Either[ConfigError, String] = {

    @tailrec
    def iterate(values: Seq[ConfigAnnotation]): Either[ConfigError, String] =

      if values.isEmpty then Left(ConfigError.missing(fieldPath, annotations))
      else
        val maybeValue = values.head match
          case env(name) => readEnv(name)
          case prop(name) => readProp(name)

        maybeValue match
          case Some(value) => Right(value)
          case None => iterate(values.tail)

    iterate(annotations)

  }

}

/** A ConfigReader implementation backed by maps for testing.
  *
  * Provides a fluent builder API for setting up test configuration values.
  *
  * @param envs map of environment variable names to values
  * @param props map of system property names to values
  */
case class MapConfigReader(
    private val envs: Map[String, String],
    private val props: Map[String, String]
) extends ConfigReader:
  /** Reads an environment variable from the internal map.
    *
    * @param name the environment variable name
    * @return Some(value) if found, None otherwise
    */
  def readEnv(name: String): Option[String] = envs.get(name)

  /** Reads a system property from the internal map.
    *
    * @param name the system property name
    * @return Some(value) if found, None otherwise
    */
  def readProp(name: String): Option[String] = props.get(name)

  /** Adds or updates an environment variable in the configuration.
    *
    * @param name the environment variable name
    * @param value the value to set
    * @return a new MapConfigReader with the updated environment variable
    */
  def onEnv(name: String, value: String): MapConfigReader =
    MapConfigReader(envs.updated(name, value), props)

  /** Adds or updates a system property in the configuration.
    *
    * @param name the system property name
    * @param value the value to set
    * @return a new MapConfigReader with the updated system property
    */
  def onProp(name: String, value: String): MapConfigReader =
    MapConfigReader(envs, props.updated(name, value))

private object LiveConfigReader extends ConfigReader {
  override def readEnv(name: String): Option[String] = sys.env.get(name)
  override def readProp(name: String): Option[String] = sys.props.get(name)
}

object ConfigReader {
  /** Creates an empty MapConfigReader for testing.
    *
    * @return a new MapConfigReader with no environment variables or system properties set
    */
  def mocked: MapConfigReader = MapConfigReader(Map.empty, Map.empty)
}
