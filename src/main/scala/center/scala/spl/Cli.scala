package center.scala.spl

/**
  * Command Line Interpreter: allows to execute bash commands
  */
object Cli {
  def exec(path: String, cmd: Seq[String]): Boolean = {
    val p = sys.process.Process(cmd, new java.io.File(path)).run()
    p.exitValue() == 0
  }

  /**
    * Create command on the system.
    * @param path where the script resides
    * @param srcFile the full path to the script
    * @param dstFile the full path to the symlink destination file
    * @return
    */
  def createCommand(path: String, srcFile: String, dstFile: String): Boolean = {
    val link = Seq("ln", "-s", srcFile, dstFile)
    val chmodScript = Seq("chmod", "555", srcFile)
    val chmodLink = Seq("chmod", "755", dstFile)

    exec(path, link) && exec(path, chmodScript) && exec(path, chmodLink)
  }
}