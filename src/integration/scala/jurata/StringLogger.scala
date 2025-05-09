package jurata

import scala.sys.process.ProcessLogger

class StringLogger extends ProcessLogger {

  val outBuffer = new StringBuilder
  val errBuffer = new StringBuilder

  override def out(s: => String): Unit = outBuffer.append(s).append("\n")

  override def err(s: => String): Unit = errBuffer.append(s).append("\n")

  override def buffer[T](f: => T): T = f

  def getOutput: String = outBuffer.toString()
  def getError: String = errBuffer.toString()

}