package connectors

import cats.data.EitherT
import models.{APIError, Repo, User}
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import play.api.libs.ws.{WSClient, WSResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GitHubConnector @Inject()(ws: WSClient) {
    def getUser[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): EitherT[Future, APIError, User] = {
        val request = ws.url(url)
        val response = request.get()
        EitherT {
            response.map {
                result => {
                    Json.toJson(result.json).validate[User] match {
                        case JsSuccess(user, _) => Right(user)
                        case JsError(_) => Left(APIError.BadAPIResponse(400, "No user exists with this login."))
                    }
                }
            }.recover {
                case _: WSResponse => Left(APIError.BadAPIResponse(500, "Could not connect."))
            }
        }
    }

    def getUserRepos[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): EitherT[Future, APIError, Seq[Repo]] = {
        val request = ws.url(url)
        val response = request.get()
        EitherT {
            response.map {
                result => {
                    Json.toJson(result.json).validate[Seq[Repo]] match {
                        case JsSuccess(repos, _) => Right(repos)
                        case JsError(_) => Left(APIError.BadAPIResponse(400, "No user exists with this login."))
                    }
                }
            }.recover {
                case _: WSResponse => Left(APIError.BadAPIResponse(500, "Could not connect."))
            }
        }
    }
}
