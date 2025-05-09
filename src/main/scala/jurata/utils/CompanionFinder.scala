package jurata.utils

private [utils] object CompanionFinder:

  private val PackageSeparator = '.'
  private val InnerClassSeparator = '$'
  private val CompanionSuffix = '$'

  def getClass(fullClassName: String): Option[Class[?]] =

    val classLoader = Option(Thread.currentThread.getContextClassLoader).getOrElse(getClass.getClassLoader)

    def loop(className: String): Option[Class[?]] = try
      Some(Class.forName(className + CompanionSuffix, true, classLoader))
    catch
      case ex: ClassNotFoundException =>
        val lastDotIndex = className.lastIndexOf(PackageSeparator)
        if (lastDotIndex != -1)
          try
            loop(className.substring(0, lastDotIndex) + InnerClassSeparator + className.substring(lastDotIndex + 1))
          catch
            case ignored: ClassNotFoundException => None
        else
          None

    loop(fullClassName)

