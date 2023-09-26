package services

import cats.data.EitherT
import connectors.GitHubConnector
import models.{APIError, Repo, User}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubService @Inject()(connector: GitHubConnector) {

    def getUser(urlOverride: Option[String] = None, login: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, User] =
        connector.getUser[User](urlOverride.getOrElse(s"https://api.github.com/users/$login"))

    def getUserRepos(urlOverride: Option[String] = None, login: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, Seq[Repo]] =
        connector.getUserRepos[Repo](urlOverride.getOrElse(s"https://api.github.com/users/$login/repos"))
}
