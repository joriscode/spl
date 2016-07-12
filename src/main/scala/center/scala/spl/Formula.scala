package center.scala.spl

import center.scala.spl.Git.Repo
import rapture.codec._
import encodings.`UTF-8`._
import rapture.io._
import rapture.uri._
import rapture.json._
import jsonBackends.jawn._

object Formula {
  /**
    * Default central repository for the formulas
    */
  val centralRepo = Repo("joriscode", "spl-central")

  /**
    * File listing additional repositories of formulas
    */
  private val sourcesFile = Spl.splDir / "sources.json"
  if (!sourcesFile.exists) "[]".copyTo(sourcesFile)
  if (!sourcesFile.exists) throw new Exception(s"Could not create $sourcesFile")

  /*
  def list(): List[InfoFile] = {
    val files = Pool(centralRepo.org, centralRepo.repo, None).listFiles()
    files.filter(_.`type` == "file").map(_.name.stripSuffix(".json"))
  }
  */

  private def writeSources(sources: List[Repo]) = {
    Json.format(Json(sources.distinct)).copyTo(sourcesFile)
  }

  private def readSources(): List[Repo] = {
    Json.parse(sourcesFile.slurp[Char]).as[List[Repo]]
  }

  def addSource(repo: Repo) = {
    writeSources(repo :: readSources())
  }

  def removeSource(repo: Repo) = {
    writeSources(readSources().filterNot(_ == repo))
  }

  def listSources() = {
    val data = readSources()
    if (data.isEmpty) {
      Prompt.info("No extra repositories of formulas defined")
    }

    data.foreach { source =>
      Prompt.display(source.org + " " + source.repo)
    }
  }

  def query(name: String): Option[Repo] = {
    val content = Repo(centralRepo.org, centralRepo.repo).download("formulas/" + name + ".json")

    content match {
      case None => None
      case Some(c) =>
        val json = Json.parse(c)
        val formula = json.as[Formula]
        Some(formula.repo)
    }
  }

  def get(name: String): Option[Project.Project] = {
    Project.findFormula(name)
  }

  def fetch(name: String, allReleases: Boolean) = query(name) match {
    case Some(repo) =>
      Project.fetch(repo, allReleases, Some(name))
    case None =>
      Prompt.error(s"No formula with the name $name")
  }

  def unpack(name: String) = get(name) match {
    case Some(project) => project.unpack()
    case None =>
      Prompt.error(s"No installed project with the formula name $name")
  }

  def install(name: String, allReleases: Boolean) = query(name) match {
    case Some(repo) =>
      Project.install(repo, allReleases, Some(name))
    case None =>
      Prompt.error(s"No formula with the name $name")
  }

  def update(name: String, allReleases: Boolean) = get(name) match {
    case Some(project) =>
      project.update(allReleases, Some(name))
    case None =>
      Prompt.error(s"No installed project with the formula name $name")
  }

  def uninstall(name: String) = get(name) match {
    case Some(project) =>
      project.uninstall()
    case None =>
      Prompt.error(s"No installed project with the formula name $name")
  }
}

/*
def create(name: String, repo: Repo) = {
  val formula = Formula(repo)
  val content = Json.parse(Json(formula))

  Pool(centralRepo.org, centralRepo.repo).upload()
}
*/

case class Formula(repo: Repo)
