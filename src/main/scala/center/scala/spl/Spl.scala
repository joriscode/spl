package center.scala.spl

import rapture.fs._
import rapture.uri._

/**
  * Simple project launcher
  */
object Spl {
  val splDir = File.homeDir / ".spl"
  val toolName = "spl"
  val toolVersion = "1.0.0"

  def checkAccessToFiles(): Unit = {
    if (!splDir.exists) splDir.mkdir()
    if (!splDir.exists) throw new Exception(s"$splDir could not be create")
    if (!splDir.readable) throw new Exception(s"$splDir is not readable")
    if (!splDir.writable) throw new Exception(s"$splDir is not writable")
  }
}
