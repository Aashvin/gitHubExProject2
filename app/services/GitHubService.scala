package services

import cats.data.EitherT
import connectors.GitHubConnector
import models.{APIError, Repo, RepoContents, RepoFile, User}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubService @Inject()(connector: GitHubConnector) {

    def getUser(urlOverride: Option[String] = None, login: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, User] =
        connector.getUser[User](urlOverride.getOrElse(s"https://api.github.com/users/$login"))

    def getUserRepos(urlOverride: Option[String] = None, login: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, Seq[Repo]] =
        connector.getUserRepos[Repo](urlOverride.getOrElse(s"https://api.github.com/users/$login/repos"))

    def getRepoContents(urlOverride: Option[String] = None, login: String, repoName: String, path: Option[String])(implicit ec: ExecutionContext): EitherT[Future, APIError, Seq[RepoContents]] = {
        path match {
            case Some(path) => connector.getRepoContents[RepoContents](urlOverride.getOrElse(s"https://api.github.com/repos/$login/$repoName/contents/$path"))
            case None => connector.getRepoContents[RepoContents](urlOverride.getOrElse(s"https://api.github.com/repos/$login/$repoName/contents"))
        }
    }

    def getRepoFile(urlOverride: Option[String] = None, login: String, repoName: String, path: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, RepoFile] =
        connector.getRepoFile[RepoFile](urlOverride.getOrElse(s"https://api.github.com/repos/$login/$repoName/contents/$path"))
}
