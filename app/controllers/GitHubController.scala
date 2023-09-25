package controllers

import javax.inject._
import play.api._
import play.api.mvc.ControllerHelpers.TODO
import play.api.mvc._

@Singleton
class GitHubController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

    def index(): Action[AnyContent] = TODO

    def create(): Action[AnyContent] = TODO

    def read(): Action[AnyContent] = TODO

    def update(): Action[AnyContent] = TODO

    def delete(): Action[AnyContent] = TODO
}
