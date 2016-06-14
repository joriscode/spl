package center.scala.sbk

/**
  * Command Line Interpreter: allows to execute bash commands
  */
object Cli {
  def exec(path: String, cmd: Seq[String]): Boolean = {
    val p = sys.process.Process(cmd, new java.io.File(path)).run()
    p.exitValue() == 0
  }
}