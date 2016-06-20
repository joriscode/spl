package center.scala.spl

import rapture.core._
import rapture.net._
import rapture.json._
import jsonBackends.jawn._
import rapture.fs._
import rapture.uri._
import rapture.io._

import scala.util.{Failure, Success}

object Git {

  case class RepoInfo(
    id: Int,
    `private`: Boolean,
    updated_at: String,
    clone_url: String,
    name: String,
    //full_name: String,
    url: String
  ) {
    def cloneGit(dest: FsUrl, releaseTag: Option[String]) = {
      if (this.`private`) {
        throw new Exception(s"The repository $name is private. Cannot clone it")

      } else {
        if ((dest / name).exists) {
          throw new Exception(s"Could not clone because the destination $dest already exists and is not an empty directory")

        } else {
          val path = Helper.fsToPath(dest)
          Prompt.info(s"Cloning $clone_url")
          clone(path)

          val dir = Helper.fsToPath(dest / name)

          releaseTag match {
            case None =>
            case Some(r) => checkout(dir, r)
          }
        }
      }
    }

    def updateGit(dest: FsUrl, releaseTag: Option[String]) = {
      if (! dest.exists) throw new Exception(s"The repository at $dest does not exist")

      val path = Helper.fsToPath(dest)
      fetchAll(path)

      releaseTag match {
        case None =>
        case Some(tag) => checkout(path, tag)
      }
    }

    private def clone(dir: String) = {
      if (! Cli.exec(dir, Seq("git", "clone", clone_url))) throw new Exception(s"Failed to clone $clone_url")
    }

    private def fetchAll(dir: String) = {
      if (! Cli.exec(dir, Seq("git", "fetch", "--all")))
        throw new Exception(s"Failed to fetch the repository")
      //throw new Exception(s"Failed to fetch the repository $full_name")
    }

    private def checkout(dir: String, releaseTag: String) = {
      if (! Cli.exec(dir, Seq("git", "checkout", "tags/" + releaseTag, "-b", releaseTag)))
        throw new Exception(s"Failed to checkout to the release $releaseTag")
    }
  }

  case class Release(
    id: Int,
    tag_name: String,
    url: String,
    published_at: String
  )

  /**
    * A Github repository
    * @param org the organisation
    * @param repo the repository
    */
  case class Repo(org: String, repo: String) {
    /**
      * The url to the Github repo
      */
    val url: HttpQuery = uri"https://api.github.com/repos/$org/$repo"

    def info: RepoInfo = {
      val response = url.httpGet(oAuth2)

      import rapture.core.modes.returnTry._
      val refFile = for {
        str <- response.slurp[Char]
        json <- Json.parse(str)
        rf <- json.as[RepoInfo]
      } yield rf

      refFile match {
        case Success(file) => file
        case Failure(_) => throw new Exception(s"Could not get the info about $this")
      }
    }

    def releases: List[Release] = {
      val response = (url.httpUrl / "releases").httpGet(oAuth2)
      import rapture.core.modes.returnTry._
      val refFile = for {
        str <- response.slurp[Char]
        json <- Json.parse(str)
        rf <- json.as[List[Release]]
      } yield rf

      // TODO show message if it rate limit message: {"message":"API rate limit exceeded for 185.25.192.172. (But here's the good news: Authenticated requests get a higher rate limit. Check out the documentation for more details.)","documentation_url":"https://developer.github.com/v3/#rate-limiting"}

      refFile match {
        case Success(file) => file
        case Failure(_) => throw new Exception(s"Could not get the list of releases about $this")
      }
    }

    // TODO improve token
    private val token = "df1ab34df6c3dd5dae4d1fdb5b0e7262371865aa"
    private val oAuth2 = Map("Authorization" -> ("token " + token.trim)) // TODO does not work Initialization error

    /**
      * Returns the latest release
      *
      * @param all true to also accept drafts and pre-releases
      * @return latest release
      */
    def latestRelease(all: Boolean): Option[Release] = all match {
      case true => releases.sortBy(_.published_at).lastOption
      case false =>
        val response = (url.httpUrl / "releases" / "latest").httpGet(oAuth2)
        import rapture.core.modes.returnTry._
        val refFile = for {
          str <- response.slurp[Char]
          json <- Json.parse(str)
          rf <- json.as[Release]
        } yield rf

        refFile.toOption
    }

    /**
      * Information and content of a file on Github.
      */
    case class RefFile(
      `type`: String,
      name: String,
      path: String,
      sha: String,
      size: Int,
      url: String,
      download_url: String,
      encoding: String,
      content: String
    ) {

      /**
        * Checks if this RefFile is of type file
        *
        * @return true if this.type == "file"
        */
      def isFile: Boolean = this.`type` == "file"
    }

    /**
      * Returns the RefFile of @name.
      *
      * @return the path to the requested file
      */
    def get(name: String): RefFile = {
      val response = (url.httpUrl / ("contents" + "/" + name)).httpGet()

      import rapture.core.modes.returnTry._
      val refFile = for {
        str <- response.slurp[Char]
        json <- Json.parse(str)
        rf <- json.as[RefFile]
      } yield rf

      import scala.util.{Success, Failure}
      refFile match {
        case Success(file) => file
        case Failure(_) => throw new Exception(s"Could not get the file $name on pool $this")
      }
    }

    /**
      * Gets the content of the script @name.
      *
      * @param name path to the script of the script for this repo
      * @return Some(content) or None if @name is not a file
      */
    def download(name: String): Option[String] = {

      // could certain use decode64(rf.content)
      def downloadContent(urlString: String): String = {
        val response = urlString.as[HttpUrl].httpGet()
        response.slurp[Char]
      }

      val file = get(name)

      if (file.isFile) {
        Some(downloadContent(file.download_url))

      } else {
        None
      }
    }
  }
}
