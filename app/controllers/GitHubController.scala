package controllers

import models.{APIError, User}

import javax.inject._
import play.api._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.ControllerHelpers.TODO
import play.api.mvc._
import services.{GitHubService, RepositoryService}

import scala.concurrent.ExecutionContext

@Singleton
class GitHubController @Inject()(val controllerComponents: ControllerComponents, val repositoryService: RepositoryService, val gitHubService: GitHubService)(implicit val ec: ExecutionContext) extends BaseController {

    def index(): Action[AnyContent] = TODO

    def create(): Action[AnyContent] = TODO

    def read(): Action[AnyContent] = TODO

    def update(): Action[AnyContent] = TODO

    def delete(): Action[AnyContent] = TODO

    def getGitHubUser(login: String): Action[AnyContent] = Action.async { implicit request =>
        gitHubService.getUser(login = login).value.map {
            case Right(user: User) => Ok(views.html.viewUser(user))
            case Left(error: APIError) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
    }
}
