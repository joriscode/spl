package center.scala.sbk

import center.scala.sbk.Git.Repo
import org.rogach.scallop._
import org.rogach.scallop.exceptions._

import scala.language.reflectiveCalls

object Scallop {

  class Conf(args: Seq[String]) extends ScallopConf(args) {
    //version(s"\nv${Sbk.toolVersion} - (c) Apache Licence v2")
    banner(s"""
              |Usage: ${Spl.toolName} [--version] [--help] [--noEnhancement] <command> [<args>]
              |""".stripMargin)

    // global options
    val version = opt[Boolean](hidden=true, descr = "Print version")
    val noEnhancement = opt[Boolean](hidden=false, descr = "Disable display enhancement")

    val list = new Subcommand("list") {}

    val fetch = new Subcommand("fetch") {
      val project = trailArg[(String, Option[String])](descr = "fetch a project")
      val allReleases = opt[Boolean](descr = "Accept also pre-releases and draft releases")

      val i = for {
        p <- project
        a <- allReleases
      } yield FetchProject(p, a)
    }

    val unpack = new Subcommand("unpack") {
      val project = trailArg[(String, Option[String])](descr = "unpack a project")
    }

    val install = new Subcommand("install") {
      val project = trailArg[(String, Option[String])](descr = "install a package")
      val allReleases = opt[Boolean](descr = "Accept also pre-releases and draft releases")

      val i = for {
        p <- project
        a <- allReleases
      } yield InstallProject(p, a)
    }

    val update = new Subcommand("update") {
      val project = trailArg[(String, Option[String])](descr = "update a package")
      val allReleases = opt[Boolean](descr = "Accept also pre-releases and draft releases")

      val u = for {
        p <- project
        a <- allReleases
      } yield UpdateProject(p, a)
    }

    val uninstall = new Subcommand("uninstall") {
      val project = trailArg[(String, Option[String])](descr = "update a package")

      val u = for {
        p <- project
      } yield UninstallProject(p)
    }

    val source = new Subcommand("source") {
      val add = opt[AddSource](descr = "add a source")
      val remove = opt[RemoveSource](descr = "remove a source")
      val list = opt[ListSource](descr = "list the sources")
      requireOne(add, remove, list)
      mutuallyExclusive(add, remove, list)
    }

    /* overwrite help */
    val help = opt[Boolean](descr = "Help")

    addSubcommand(list)
    addSubcommand(fetch)
    addSubcommand(unpack)
    addSubcommand(install)
    addSubcommand(update)
    addSubcommand(uninstall)
    addSubcommand(source)


    override def onError(e: Throwable): Unit = e match {
      case Help("") => H()
      case Help(subcommandName) => H(Some(subcommandName))
      case Version =>
      case Exit() => // catches both Help and Error
      case ScallopException(message) => // catches all exceptions
      case RequiredOptionNotFound(optionName) =>
      // you can also conveniently match on exceptions
      case other => throw other
    }

    verify()
  }

  implicit val installProjectConverter = new ValueConverter[(String, Option[String])] {
    def parse(s: List[(String, List[String])]): Either[String, Option[(String, Option[String])]] = {
      s match {
        case (_, formula :: Nil) :: Nil => Right(Some((formula, None)))
        case (_, org :: repo :: Nil) :: Nil => Right(Some((org, Some(repo))))
        case Nil => Right(None)
        case _ => Left("install project parser error")
      }
    }

    val tag = scala.reflect.runtime.universe.typeTag[(String, Option[String])]
    val argType = org.rogach.scallop.ArgType.LIST
  }

  implicit val addSourceConverter = new ValueConverter[AddSource] {
    def parse(s: List[(String, List[String])]): Either[String, Option[AddSource]] = {
      s match {
        case (_, org :: repo :: Nil) :: Nil => Right(Some(AddSource(Repo(org, repo))))
        case Nil => Right(None)
        case _ => Left("add source parser error")
      }
    }

    val tag = scala.reflect.runtime.universe.typeTag[AddSource]
    val argType = org.rogach.scallop.ArgType.LIST
  }

  implicit val removeSourceConverter = new ValueConverter[RemoveSource] {
    def parse(s: List[(String, List[String])]): Either[String, Option[RemoveSource]] = {
      s match {
        case (_, org :: repo :: Nil) :: Nil => Right(Some(RemoveSource(Repo(org, repo))))
        case Nil => Right(None)
        case _ => Left("remove source parser error")
      }
    }

    val tag = scala.reflect.runtime.universe.typeTag[RemoveSource]
    val argType = org.rogach.scallop.ArgType.LIST
  }

  implicit val listSourceConverter = new ValueConverter[ListSource] {
    def parse(s: List[(String, List[String])]): Either[String, Option[ListSource]] = {
      s match {
        case (_, _) :: Nil => Right(Some(ListSource()))
        case Nil => Right(None)
        case _ => Left("list source parser error")
      }
    }

    val tag = scala.reflect.runtime.universe.typeTag[ListSource]
    val argType = org.rogach.scallop.ArgType.LIST
  }


  def whichCommand(args: Seq[String]): Command = {
    val conf = new Conf(args)

    if (conf.noEnhancement.supplied) Prompt.options = Set('r')

    if (conf.help.supplied) {
      H()

    } else if (conf.version.supplied) {
      V()

    } else {
      conf.subcommand match {
        case Some(conf.list) =>
          ListProject()
        //if (conf.list)

        case Some(conf.fetch) =>
          if (conf.fetch.i.supplied) {
            conf.fetch.i.get.get

          } else {
            Error("The given fetch command does not exist. See --help")
          }

        case Some(conf.unpack) =>
          if (conf.unpack.project.supplied) {
            UnpackProject(conf.unpack.project.get.get)
          } else {
            Error("The given unpack command does not exist. See --help")
          }

        case Some(conf.install) =>
          if (conf.install.i.supplied) {
            conf.install.i.get.get

          } else {
            Error("The given install command does not exist. See --help")
          }

        case Some(conf.update) =>
          if (conf.update.u.supplied) {
            conf.update.u.get.get

          } else {
            Error("The given update command does not exist. See --help")
          }

        case Some(conf.uninstall) =>
          if (conf.uninstall.u.supplied) {
            conf.uninstall.u.get.get

          } else {
            Error(s"The given uninstall command does not exist. See --help")

          }

        case Some(conf.source) =>
          if (conf.source.add.supplied) {
            conf.source.add.get.get

          } else if (conf.source.remove.supplied) {
            conf.source.remove.get.get

          } else if (conf.source.list.supplied) {
            conf.source.list.get.get

          } else {

            Error(s"The given source command does not exist. See --help")
          }

        case _ => Error("No command provided. See --help")
      }
    }
  }
}
