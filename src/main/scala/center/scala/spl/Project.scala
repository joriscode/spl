package center.scala.spl

import center.scala.spl.Git.{Repo, RepoInfo}
import rapture.uri._
import rapture.io._
import rapture.codec._
import encodings.`UTF-8`._
import rapture.json._
import jsonBackends.jawn._
import rapture.fs.FsUrl


object Project {
  private val shellsDir = Spl.splDir / "shells"
  if (!shellsDir.exists) shellsDir.mkdir()
  if (!shellsDir.exists) throw new Exception(s"Could not create the directory $shellsDir")

  private val projectsDir = Spl.splDir / "projects"
  if (!projectsDir.exists) projectsDir.mkdir()
  if (!projectsDir.exists) throw new Exception(s"Could not create the directory $projectsDir")

  private val listing = Spl.splDir / "project.json"
  if (!listing.exists) "{}".copyTo(listing)
  if (!listing.exists) throw new Exception(s"Could not create $listing")

  private val binaries = "/usr/local/bin"
  private val binariesFs = Helper.pathToFs(binaries)
  if (!binariesFs.exists) throw new Exception(s"The directory $binariesFs does not exist")

  /**
    * Represents a project on the disk
    * It is used to differentiate between a Github repository (project online)
    * and the project on the disk.
    *
    * @param org the Github organisation
    * @param repo the Github repository
    */
  case class Project(org: String, repo: String) {
    override def toString: String = org + "/" + repo

    def compiledDestination: FsUrl = shellsDir / org / repo

    def cloneDestination: FsUrl = projectsDir / org / repo

    def buildLocation: FsUrl = cloneDestination / "build.sbt"

    private def readBuild(): List[String] = {
      val build = buildLocation
      if (!build.exists) throw new Exception(s"Could not find the build of the project at $build")

      val all = build.slurp[Char]
      val content = all.split("\n")

      import fastparse.all._
      import fastparse.core.Parsed
      val space = P(" ")
      val spaces = P(space.rep(1)) // one or more
      val anyString = P(CharIn(".", "_", "-", 'a' to 'z', 'A' to 'Z', '0' to '9').rep)

      val projectParser = P(space.rep ~ "lazy" ~ spaces ~ "val" ~ spaces ~ anyString.! ~ spaces ~ "=" ~ spaces ~ "project" ~ anyString).?

      val runs = content.map { s =>
        val Parsed.Success(x, _) = projectParser.parse(s)
        x
      }

      runs.flatten.toList
    }

    /**
      * Tries to create the commands from the downloaded repository
      */
    def unpack() = {
      /*
        * Creates a Unix command
        *
        * @param file     the file to symlink
        * @param binaries the location of the binaries (/usr/local/bin/)
        */
      def createSymlink(file: FsUrl, binaries: String) = {
        val cmd = Seq("install", "-S", Helper.fsToPath(file), binaries)
        if (!Cli.exec(".", cmd)) throw new Exception(s"Could not create the command ${file.filename}.")
      }

      val build = cloneDestination / "build.sbt"
      if (!build.exists) throw new Exception(s"Could not find the build of the project at $build")

      val shellDir = compiledDestination
      if (!shellDir.exists) shellDir.mkdir(true)
      if (!shellDir.exists) throw new Exception(s"Could not create the directory $shellDir")

      val subProjects = readBuild()
      val cmds = subProjects match {
        case Nil => List("run")
        case xs => xs.map(_ + "/run")
      }

      val dir = Helper.fsToPath(cloneDestination)
      val shells = cmds.map { cmd =>
        "#! /bin/sh\n" + "cd " + dir + "\n" + "CMD=\"" + cmd + " $@\"\n" + "sbt $CMD"
      }

      val filenames = subProjects match {
        case Nil => List(repo.toLowerCase)
        case xs => xs.map(repo.toLowerCase + "-" + _)
      }
      val files = filenames.map(shellDir / _)

      files.zip(shells).foreach { case (file, shell) =>
        shell.copyTo(file)

        val symlinkFile = binariesFs / file.filename
        if (!symlinkFile.exists) {
          // Create a symlink between .sbk/shells/org/project/... to /usr/local/bin/...
          createSymlink(file, binaries)
          Prompt.info(s"Created command ${file.filename}")

        } else {
          Prompt.warn(s"Did not create the command ${file.filename} because it already exists on the system")
        }
      }
    }


    /**
      * Uninstalls a repository
      *
      */
    def uninstall() = {
      val shellDir = compiledDestination

      if (shellDir.exists) {
        val children = shellDir.children
        children.foreach{ child =>
          val symlink = binariesFs / child.filename
          symlink.delete()
        }

        Helper.deleteDir(shellDir)
        if (shellDir.exists) Prompt.error(s"Could not delete directory $shellDir. Please do it manually.")
      }

      val projectDir = cloneDestination
      if (projectDir.exists) {
        Helper.deleteDir(projectDir)
        if (projectDir.exists) Prompt.error(s"Could not delete directory $projectDir. Please do it manually.")
      }

      remove(this)

      Prompt.info(s"Uninstalled $repo")
    }

    // could propose --force to update any way
    def update(allReleases: Boolean, formula: Option[String]) = {
      val repo = Repo(this.org, this.repo)
      val infoRepo = repo.info
      val localInfo = get(this) match {
        case Some(i) => i
        case None => throw new Exception(s"Could not find locally the repository $repo")
      }

      /* Tag name of the latest release or None */
      val latestTag = repo.latestRelease(allReleases).map(_.tag_name)
      if (latestTag.isEmpty) Prompt.warn("This repository has no tagged release")

      // update
      //if (infoRepo.updated_at > localInfo.updated) { // updated_at corresponds to push only? it doesn't seem to be the last commit date
      if (latestTag != localInfo.latest) {
        val dest = cloneDestination

        infoRepo.updateGit(dest, latestTag)
        remove(this)
        add(this, infoRepo, latestTag, formula, localInfo.installed)

      } else {
        Prompt.info(s"The repository $repo is up-to-date")
      }
    }
  }
  /**
    * Installs (fetches & unpacks) a repository
    *
    * @param repo        the Github repository
    * @param allReleases accept pre-releases and drafts
    * @param formula     if the repo to install was identified by a formula
    */
  def install(repo: Repo, allReleases: Boolean, formula: Option[String]) = {
    fetch(repo, allReleases, formula).unpack()
  }

  /**
    * Clones the repository but do not install it
    *
    * @param repo the Github repository
    * @param allReleases accepts draft and pre-releases
    * @param formula if the repository is defined by a formula
    */
  def fetch(repo: Repo, allReleases: Boolean, formula: Option[String]): Project = {
    val dest = projectsDir / repo.org
    if (! dest.exists) dest.mkdir()
    if (! dest.exists) throw new Exception(s"Could not create the directory $dest")

    val latestTag = repo.latestRelease(allReleases).map(_.tag_name)
    if (latestTag.isEmpty) Prompt.warn("This repository has no tagged release")

    val infoRepo = repo.info
    infoRepo.cloneGit(dest, latestTag)

    val project = Project(repo.org, repo.repo)
    add(project, infoRepo, latestTag, formula, false)
    Project(repo.org, repo.repo)
  }

  /**
    * Content of the listing file
 *
    * @param id the Github repository id
    * @param url the Github repository url
    * @param latest the latest release tag if defined
    * @param formula the formula if the Github repo has been defined by a formula
    * @param installed if installed or simply cloned
    */
  case class Info(id: Int, url: String, latest: Option[String], formula: Option[String], installed: Boolean)

  /**
    * Returns the project associated to the formula
    *
    * @param name the formula
    * @return the project
    */
  def findFormula(name: String): Option[Project] = {
    val repos = read().map { case (k, v) =>
      v.formula match {
        case Some(f) if f == name => Some(k)
        case _ => None
      }
    }

    repos.flatten match {
      case formula :: Nil => Some(formula)
      case Nil => None
      case xs => throw new Exception("More than one repository corresponding to the formula. Use full name.")
    }
  }

  def list() = {
    val data = read()
    if (data.isEmpty) {
      Prompt.info("No project managed")
    }

    data.foreach { case (k, v) =>
      Prompt.display(s"$k => unpacked: ${v.installed}, formula: ${v.formula.getOrElse("not obtained from a formula")}, " +
        s"version: ${v.latest.getOrElse("not based on a release")}, url: ${v.url}")
    }
  }

  private def get(project: Project): Option[Info] = read().get(project)

  private def add(project: Project, info: RepoInfo, releaseTag: Option[String], formula: Option[String], installed: Boolean) =
    write(read() ++ Map(project -> Info(info.id, info.url, releaseTag, formula, installed)))
  private def remove(project: Project) = write(read() - project)

  private def read(): Map[Project, Info] ={
    val res = Json.parse(listing.slurp[Char]).as[Map[String, Info]]
    // ugly extractor
    res.map{ case (k, v) =>
      k.split("/").toList match {
        case org :: repo :: Nil => (Project(org, repo), v)
        case x => throw new Exception(s"Error at parsing Json. Could not parse map key $x")
      }
    }
  }

  private def write(map: Map[Project, Info]) = {
    // ugly serializer
    val serialized = map.map{ case (k, v) =>
      (k.toString, v)
    }
    Json.format(Json(serialized)).copyTo(listing)
  }
}