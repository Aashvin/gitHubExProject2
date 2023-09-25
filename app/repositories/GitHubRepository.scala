package repositories

import models.User
import org.mongodb.scala.model.{IndexModel, Indexes}
import play.api.mvc.{Action, AnyContent}
import play.api.mvc.ControllerHelpers.TODO
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GitHubRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext) extends PlayMongoRepository[User] (
    collectionName = "dataModels",
    mongoComponent = mongoComponent,
    domainFormat = User.formats,
    indexes = Seq(IndexModel(
        Indexes.ascending("_id")
    )),
    replaceIndexes = false
) {
    def index(): Action[AnyContent] = TODO

    def create(): Action[AnyContent] = TODO

    def read(): Action[AnyContent] = TODO

    def update(): Action[AnyContent] = TODO

    def delete(): Action[AnyContent] = TODO
}
