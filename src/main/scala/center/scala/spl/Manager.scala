package center.scala.spl

import rapture.uri._

object Manager {
  val dir = Spl.splDir / ".manager"
  val repoDir = dir / "repo"

  def bootstrap() = {
    Prompt.info("Start bootstrapping")
    if (! repoDir.exists) throw new Exception(s"To be installed, the tool should be located in $repoDir")

    if (! generateScript()) throw new Exception(s"Could not create the launcher script")

    val binaryDir = "/usr/local/bin"
    val symFile = Helper.pathToFs(binaryDir)/ Spl.toolName
    if (symFile.exists) throw new Exception(s"The command ${Spl.toolName} already exists")


    val file = repoDir / "target" / "universal" / "stage"/ "bin" / "spl"
    val cmd = Seq("install", "-S", Helper.fsToPath(file), binaryDir)
    if (!Cli.exec(".", cmd)) throw new Exception(s"Could not create the command ${file.filename}.")
  }

  private def generateScript(): Boolean = {
    Prompt.info("Generate launcher script")
    Cli.exec(Helper.fsToPath(repoDir), Seq("sbt", "stage"))
  }

  def update() = {
    if (! repoDir.exists) throw new Exception(s"To be installed, the tool should be located in $repoDir")

    // TODO implement. We can assume than master will only contain tagged releases and not in development commits
    Prompt.warn("Not implemented yet")
    Prompt.info("Execute the commands:")
    Prompt.info(s"cd $repoDir")
    Prompt.info("git pull origin master")
  }
}
