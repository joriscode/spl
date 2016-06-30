package center.scala.spl

import rapture.uri._
import rapture.io._
import rapture.codec._
import encodings.`UTF-8`._

object Manager {
  val dir = Spl.splDir / ".manager"
  val repoDir = dir / "repo"
  val file = dir / Spl.toolName
  val binaryDir = "/usr/local/bin"
  val symFile = Helper.pathToFs(binaryDir)/ Spl.toolName

  def bootstrap() = {
    Prompt.info("Start bootstrapping")
    if (! repoDir.exists) throw new Exception(s"To be installed, the tool should be located in $repoDir")

    if (! generateScript()) throw new Exception(s"Could not create the launcher script")

    if (symFile.exists) throw new Exception(s"The command ${Spl.toolName} already exists")

    val content = "#! /bin/sh\n" + "cd " + Helper.fsToPath(repoDir) + "\n" + "CMD=\"$@\"\n" + "java -jar ./target/scala-2.11/spl.jar $CMD"
    content.copyTo(file)


    val cmd = Seq("install", "-S", Helper.fsToPath(file), binaryDir)
    if (!Cli.exec(".", cmd)) throw new Exception(s"Could not create the command ${file.filename}.")
  }

  private def generateScript(): Boolean = {
    Prompt.info("Generate launcher script")
    Cli.exec(Helper.fsToPath(repoDir), Seq("sbt", "assembly"))
  }

  def update() = {
    if (! repoDir.exists) throw new Exception(s"To be installed, the tool should be located in $repoDir")

    Prompt.info(s"Update ${Spl.toolName} source code")
    Cli.exec(Helper.fsToPath(repoDir), Seq("git", "pull"))

    // Delete the symlink that creates the command
    symFile.delete()

    Prompt.warn(s"Run the following command to update the launcher of ${Spl.toolName}")
    Prompt.warn("cd ~/.spl/.manager/repo/ && sbt \"run manager --bootstrap\"")

    // TODO implement. We can assume than master will only contain tagged releases and not in development commits
    //Prompt.warn("Not implemented yet")
    //Prompt.info("Execute the commands:")
    //Prompt.info(s"cd $repoDir")
    //Prompt.info("git pull origin master")
  }
}
