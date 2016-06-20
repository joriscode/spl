package center.scala.spl

import rapture.fs._
import rapture.uri._
import rapture.io._
import rapture.codec._
import encodings.`UTF-8`._

// TODO create API
// TODO benchmark

object Main extends App {
  try {
    Spl.checkAccessToFiles()
    Scallop.whichCommand(args).exec()

  } catch {
    case e: Throwable =>
      val message = e.getMessage
      val cause = e.getCause

      if (message != null) {
        Prompt.error(message)
      }

      if (cause != null) {
        Prompt.error(cause.toString)
      }

      Prompt.error(s"See ${Spl.splDir}/last.log for full stacktrace")
      val log = Spl.splDir / "last.log"
      val str = e.toString + "\n" + e.getStackTrace.mkString("\n") + "\n" + e.getCause
      str.copyTo(log)
  }
}
