package center.scala.sbk

import center.scala.sbk.Git.Repo

sealed trait Command {
  def exec(): Unit
}

case class InstallProject(arg: (String, Option[String]), allReleases: Boolean = false) extends Command {
  override def exec(): Unit = arg._2 match {
    case None => Formula.install(arg._1, allReleases)
    case Some(repo) => Project.install(Repo(arg._1, repo), allReleases, None)
  }
}

/*
case class InstallFormula(name: String) extends Command {
  override def exec(): Unit = Formula.install(name)
}
*/

// TODO add doctor, check

case class ListProject() extends Command {
  override def exec(): Unit = Project.list()
}

case class FetchProject(arg: (String, Option[String]), allReleases: Boolean = false) extends Command {
  override def exec(): Unit = arg._2 match {
    case None => Formula.fetch(arg._1, allReleases)
    case Some(repo) => Project.install(Repo(arg._1, repo), allReleases, None)
  }
}

case class UnpackProject(arg: (String, Option[String])) extends Command {
  override def exec(): Unit = arg._2 match {
    case None => Formula.unpack(arg._1)
    case Some(repo) => Project.Project(arg._1, repo).unpack()
  }
}

case class UpdateProject(arg: (String, Option[String]), allReleases: Boolean = false) extends Command {
  override def exec(): Unit = arg._2 match {
    case None => Formula.update(arg._1, allReleases)
    case Some(repo) => Project.Project(arg._1, repo).update(allReleases, None)
  }
}

case class UninstallProject(arg: (String, Option[String])) extends Command {
  override def exec(): Unit = arg._2 match {
    case None => Formula.uninstall(arg._1)
    case Some(repo) => Project.Project(arg._1, repo).uninstall()
  }
}

case class AddSource(repo: Repo) extends Command {
  override def exec(): Unit = Formula.addSource(repo)
}

case class RemoveSource(repo: Repo) extends Command {
  override def exec(): Unit = Formula.removeSource(repo)
}

case class ListSource() extends Command {
  override def exec(): Unit = Formula.listSources()
}

case class V() extends Command {
  override def exec(): Unit = Prompt.display(Spl.toolVersion)
}

case class Error(msg: String) extends Command {
  override def exec(): Unit = Prompt.error(msg)
}

case class H(command: Option[String] = None) extends Command {
  override def exec(): Unit = {
    val str = command match {
      case None => globalHelp()
      case Some(cmd) => help(cmd)
    }

    Prompt.display(str)
  }

  def globalHelp(): String = {
    //|Usage: sbk [--version] [--help] [--noEnhencement] <command> [<args>]
    s"""v0.2 - (c) Apache Licence v2
        |
        |  -n, --no-enhancement   Disable display enhancement
        |  -h, --help             Show help message
        |  -v, --version          Print version
        |
        |Command: list - Display information about all the projects managed
        |  Usage:
        |    ${Spl.toolName} list
        |
        |Command: fetch - Fetch a project
        |  Usage:
        |    ${Spl.toolName} fetch <formula>
        |      A formula is a Github repository registered on ${Formula.centralRepo}
        |    ${Spl.toolName} fetch <github-organisation> <github-repository>
        |
        |Command: unpack - Install a project that has been fetched
        |  Usage:
        |    ${Spl.toolName} unpack <formula>
        |      A formula is a Github repository registered on ${Formula.centralRepo}
        |    ${Spl.toolName} unpack <github-organisation> <github-repository>
        |
        |Command: install - Install a project
        |  Usage:
        |    ${Spl.toolName} install <formula>
        |      A formula is a Github repository registered on ${Formula.centralRepo}
        |    ${Spl.toolName} install <github-organisation> <github-repository>
        |
        |Command: uninstall - Uninstall a project
        |  Usage:
        |    ${Spl.toolName} uninstall <formula>
        |    ${Spl.toolName} uninstall <github-organisation> <github-repository>
        |
        |
        |Command: pool - Registration of Github pools
        |  Usage:
        |    ${Spl.toolName} pool [option]
        |
        |  Options:
        |    -a, --add  <alias> <org> <repo> <token>*                   Register a pool
        |    -c, --change-token  <poolAlias> <token>                    Change the oAuth2 token of the pool
        |    -r, --remove  <poolAlias>                                  Unregister a pool
        |    -d, --download  <poolAlias> <path-to-file-on-the-repo>     Download a script from a pool
        |    -u, --upload  <scriptAlias> <poolAlias>                    Upload a registered script to the user's pool
       """.stripMargin
  }


  def help(cmd: String): String = cmd match {
    case _ => "use --help"
  }
}
