package center.scala.sbk

import rapture.uri._
import rapture.io._
import rapture.codec._
import encodings.`UTF-8`._

object Manager {
  val dir = Spl.splDir / ".manager"
  val file = dir / Spl.toolName
  val repoDir = dir / "repo"

  def bootstrap() = {
    if (! repoDir.exists) throw new Exception(s"To be installed, the tool should be located in $repoDir")

    val binaryDir = "/usr/local/bin"
    val symFile = Helper.pathToFs(binaryDir)/ Spl.toolName
    if (symFile.exists) throw new Exception(s"The command ${Spl.toolName} already exists")

    val content = "#! /bin/sh\n" + "cd " + Helper.fsToPath(repoDir) + "\n" + "CMD=\"" + "run" + " $@\"\n" + "sbt $CMD"
    content.copyTo(file)

    val cmd = Seq("install", "-S", Helper.fsToPath(file), binaryDir)
    if (!Cli.exec(".", cmd)) throw new Exception(s"Could not create the command ${file.filename}.")
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
