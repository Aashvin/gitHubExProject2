package controllers

import models.{APIError, Repo, User}

import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.{GitHubService, RepositoryService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubController @Inject()(val controllerComponents: ControllerComponents, val repositoryService: RepositoryService, val gitHubService: GitHubService)(implicit val ec: ExecutionContext) extends BaseController {

    def index(): Action[AnyContent] = Action.async { implicit request =>
        repositoryService.index().map {
            case Right(users: Seq[User]) => Ok(Json.toJson(users))
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }

    def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
        repositoryService.create.map {
            case Right(user: User) => Created(Json.toJson(user))
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }

    def read(login: String): Action[AnyContent] = Action.async { implicit request =>
        repositoryService.read(login).map {
            case Right(user: User) => Ok(views.html.viewUser(user))
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }

    def update(login: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
        repositoryService.update(request, login).map {
            case Right(user: User) => Accepted(Json.toJson(user))
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }

    def delete(login: String): Action[AnyContent] = Action.async { implicit request =>
        repositoryService.delete(login).map {
            case Right(_: Boolean) => Accepted
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }

    def getGitHubUser(login: String): Action[AnyContent] = Action.async { implicit request =>
        gitHubService.getUser(login = login).value.map {
            case Right(user: User) => Ok(views.html.viewUser(user))
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }

    def createFromGitHub(login: String): Action[AnyContent] = Action.async { implicit request =>
        gitHubService.getUser(login = login).value.flatMap {
            case Right(user: User) => repositoryService.createFromGitHub(user).flatMap {
                    case Right(user: User) => Future(Redirect(routes.GitHubController.read(user.login)))
                    case Left(error: APIError) => Future(Status(error.httpResponseStatus)(Json.toJson(error.reason)))
                }
            case Left(error: APIError) => Future(Status(error.httpResponseStatus)(Json.toJson(error.reason)))
        }
    }

    def getGitHubUserRepos(login: String): Action[AnyContent] = Action.async { implicit request =>
        gitHubService.getUserRepos(login = login).value.map {
            case Right(repos: Seq[Repo]) => Ok(Json.toJson(repos))
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }
}
