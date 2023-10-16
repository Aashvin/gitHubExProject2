package controllers

import models._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.{GitHubService, RepositoryService}

import javax.inject._
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
            case Right(repos: Seq[Repo]) => Ok(views.html.viewRepos(login, repos))
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }

    def getGitHubRepo(login: String, repoName: String): Action[AnyContent] = Action.async { implicit request =>
        gitHubService.getRepoContents[RepoContents](login = login, repoName = repoName, path = "").value.map {
            case Some(Right(repoContents: Seq[RepoContents])) => Ok(views.html.viewRepoContents(login, repoName, repoContents, ""))
            case Some(Left(error: APIError)) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
            case None => Status(400)(Json.toJson("This repo content does not exist."))
        }
    }

    def getGitHubRepoContents(login: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
        gitHubService.getRepoContents[RepoFile](login = login, repoName = repoName, path = path).value.flatMap {
            case Some(Right(Seq(repoFile: RepoFile))) => Future(Ok(views.html.viewRepoFile(login, repoName, repoFile)))
            case Some(Right(repoContent)) => Future(Status(400)(Json.toJson(s"Incorrectly fetched content: $repoContent")))
            case Some(Left(error: APIError)) => Future(Status(error.httpResponseStatus)(Json.toJson(error.reason)))

            case None => gitHubService.getRepoContents[RepoContents](login = login, repoName = repoName, path = path).value.flatMap {
                case Some(Right(repoContents: Seq[RepoContents])) => Future(Ok(views.html.viewRepoContents(login, repoName, repoContents, path)))
                case Some(Left(error: APIError)) => Future(Status(error.httpResponseStatus)(Json.toJson(error.reason)))
                case None => Future(Status(400)(Json.toJson("This repo content does not exist.")))
            }
        }
    }
}
