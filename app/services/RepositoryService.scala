package services

import models.{APIError, User}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.Request
import repositories.{GitHubRepository, RepositoryTrait}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RepositoryService @Inject()(gitHubRepository: RepositoryTrait)(implicit ec: ExecutionContext) {
    def index(): Future[Either[APIError, Seq[User]]] = {
        gitHubRepository.index()
    }

    def create(implicit request: Request[JsValue]): Future[Either[APIError, User]] = {
        request.body.validate[User] match {
            case JsSuccess(user, _) => gitHubRepository.create(user)
            case JsError(_) => Future(Left(APIError.BadAPIResponse(400, "Input can't be parsed as a User.")))
        }
    }

    def read(login: String): Future[Either[APIError, User]] = {
        gitHubRepository.read(login)
    }

    def update(implicit request: Request[JsValue], login: String): Future[Either[APIError, User]] = {
        request.body.validate[User] match {
            case JsSuccess(user, _) => gitHubRepository.update(login, user)
            case JsError(_) => Future(Left(APIError.BadAPIResponse(400, "Input can't be parsed as a User.")))
        }
    }

    def delete(login: String): Future[Either[APIError, Boolean]] = {
        gitHubRepository.delete(login)
    }

    def createFromGitHub(user: User): Future[Either[APIError, User]] = {
        gitHubRepository.create(user)
    }
}
