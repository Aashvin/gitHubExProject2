package repositories

import com.google.inject.ImplementedBy
import models.{APIError, User}
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[GitHubRepository])
trait RepositoryTrait {
    def index(): Future[Either[APIError, Seq[User]]]
    def create(user: User): Future[Either[APIError, User]]
    def read(login: String): Future[Either[APIError, User]]
    def update(login: String, user: User): Future[Either[APIError, User]]
    def delete(login: String): Future[Either[APIError, Boolean]]
    def deleteAll(): Future[Unit]
}

@Singleton
class GitHubRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext) extends PlayMongoRepository[User] (
    collectionName = "users",
    mongoComponent = mongoComponent,
    domainFormat = User.formats,
    indexes = Seq(IndexModel(
        Indexes.ascending("login"), IndexOptions().unique(true)
    )),
    replaceIndexes = false
) with RepositoryTrait {
    def index(): Future[Either[APIError, Seq[User]]] = {
        collection.find().toFuture().map {
            case users: Seq[User] => Right(users)
            case _ => Left(APIError.BadAPIResponse(400, "Could not retrieve users."))
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
            case Some(user: User) => Future(Right(user))
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
                        case _ => Future(Left(APIError.BadAPIResponse(400, "Could not update user.")))
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
