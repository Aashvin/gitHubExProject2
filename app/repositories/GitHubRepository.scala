package repositories

import models.{APIError, User}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GitHubRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext) extends PlayMongoRepository[User] (
    collectionName = "dataModels",
    mongoComponent = mongoComponent,
    domainFormat = User.formats,
    indexes = Seq(IndexModel(
        Indexes.ascending("_id")
    )),
    replaceIndexes = false
) {
    def index(): Future[Either[APIError, Seq[User]]] = {
        collection.find().toFuture().map {
            case users: Seq[User] => Right(users)
            case _ => Left(APIError.BadAPIResponse(404, "No users found."))
        }
    }

    def create(user: User): Future[Either[APIError, User]] = {
        collection.find(Filters.equal("login", user.login)).headOption flatMap {
            case Some(_) => Future(Left(APIError.BadAPIResponse(400, "A user with this login already exists.")))
            case None => collection
                .insertOne(user)
                .toFutureOption()
                .map {
                    case Some(value) if value.wasAcknowledged() => Right(user)
                    case _ => Left(APIError.BadAPIResponse(400, "Could not create user."))
                }
        }
    }

    def read(login: String): Future[Either[APIError, User]] = {
        collection.find(Filters.equal("login", login)).headOption flatMap {
            case Some(data) => Future(Right(data))
            case None => Future(Left(APIError.BadAPIResponse(404, "User not found.")))
        }
    }

    def update(login: String, updatedUser: User): Future[Either[APIError, User]] = {
        collection.find(Filters.equal("login", login)).headOption().flatMap {
            case Some(user: User) =>
                if (updatedUser.login != user.login) {
                    Future(Left(APIError.BadAPIResponse(400, "The updated login needs to be the same as the current login.")))
                } else {
                    collection.replaceOne(
                        filter = Filters.equal("login", login),
                        replacement = updatedUser,
                        options = new ReplaceOptions().upsert(true)
                    ).toFutureOption().flatMap {
                        case Some(result) if result.getMatchedCount == 1 && result.getModifiedCount == 1 => read(login)
                        case _ => Future(Left(APIError.BadAPIResponse(400, "No user fields were updated.")))
                    }
                }
            case None => Future(Left(APIError.BadAPIResponse(404, "User not found.")))
        }
    }

    def delete(login: String): Future[Either[APIError, Boolean]] = {
        collection.deleteOne(
            filter = Filters.equal("login", login)
        ).toFutureOption().map {
            case Some(value) if value.getDeletedCount == 1 => Right(true)
            case _ => Left(APIError.BadAPIResponse(404, "User not found."))
        }
    }

    def deleteAll(): Future[Unit] = collection.deleteMany(Filters.empty()).toFuture().map(_ => ())
}
