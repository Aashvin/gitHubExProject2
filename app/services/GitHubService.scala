package services

import cats.data.EitherT
import connectors.GitHubConnector
import models.{APIError, Repo, RepoContent, User}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsError, JsSuccess, JsValue, OFormat}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubService @Inject()(connector: GitHubConnector) {

    def getUser(urlOverride: Option[String] = None, login: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, User] =
        connector.getUser[User](urlOverride.getOrElse(s"https://api.github.com/users/$login"))

    def getUserRepos(urlOverride: Option[String] = None, login: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, Seq[Repo]] =
        connector.getUserRepos[Repo](urlOverride.getOrElse(s"https://api.github.com/users/$login/repos"))

    def getRepoContents[T <: RepoContent](urlOverride: Option[String] = None, login: String, repoName: String, path: String)(implicit rds: OFormat[T], ec: ExecutionContext): Future[Option[Either[APIError, Seq[T]]]] = {
        connector.getRepoContents(urlOverride.getOrElse(s"https://api.github.com/repos/$login/$repoName/contents/$path")).map {
            case Right(value: JsValue) =>
                value.validate[T] match {
                    case JsSuccess(repoFile, _) => Some(Right(Seq(repoFile)))
                    case JsError(_) => value.validate[Seq[T]] match {
                        case JsSuccess(repoContents, _) => Some(Right(repoContents))
                        case JsError(_) => None
                    }
                }
            case Left(error: APIError) => Some(Left(error))
        }
    }
}
