package services

import cats.data.EitherT
import connectors.GitHubConnector
import models.{APIError, User}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubService @Inject()(connector: GitHubConnector) {

    def getUser(urlOverride: Option[String] = None, login: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, User] =
        connector.getUser[User](urlOverride.getOrElse(s"https://api.github.com/users/$login"))
}
