package services

import baseSpec.BaseSpec
import cats.data.EitherT
import connectors.GitHubConnector
import models.{APIError, Owner, Repo, User}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json, OFormat}

import scala.concurrent.{ExecutionContext, Future}

class GitHubServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {
    val mockConnector: GitHubConnector = mock[GitHubConnector]
    implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    val testService = new GitHubService(mockConnector)

    val someUser: JsValue = Json.obj(
        "login" -> "someLogin",
        "created_at" -> "someDate",
        "location" -> "someLocation",
        "followers" -> 3,
        "following" -> 100
    )

    val repo1: Repo = Repo("name1", `private` = false, Owner("owner1"), Some("description1"), "url1", "01/01/2001", "02/02/2002", "public", 0, 0, 0, "main")
    val repo2: Repo = Repo("name2", `private` = false, Owner("owner2"), Some("description2"), "url2", "03/03/2003", "04/04/2004", "public", 2, 3, 10, "main")
    val someRepos: Seq[Repo] = Seq(repo1, repo2)

    "GitHubService .getUser" should {
        val url: String = "testUrl"

        "return a user" in {
            (mockConnector.getUser[User](_: String)(_: OFormat[User], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.rightT[Future, APIError](someUser.as[User]))
                .once()

            whenReady(testService.getUser(urlOverride = Some(url), login = "someLogin").value) { result =>
                result shouldBe Right(User("someLogin", "someDate", Some("someLocation"), 3, 100))
            }
        }

        "return a connection error" in {
            val url: String = "testUrl"

            (mockConnector.getUser[User](_: String)(_: OFormat[User], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.leftT[Future, User](APIError.BadAPIResponse(500, "Could not connect.")))
                .once()

            whenReady(testService.getUser(urlOverride = Some(url), login = "someLogin").value) { result =>
                result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect."))
            }
        }

        "return a can't find user error" in {
            val url: String = "testUrl"

            (mockConnector.getUser[User](_: String)(_: OFormat[User], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.leftT[Future, User](APIError.BadAPIResponse(400, "No user exists with this login.")))
                .once()

            whenReady(testService.getUser(urlOverride = Some(url), login = "someLogin").value) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "No user exists with this login."))
            }
        }
    }

    "GitHubService .getUserRepos" should {
        val url: String = "testUrl"

        "return a user" in {
            (mockConnector.getUserRepos[Repo](_: String)(_: OFormat[Repo], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.rightT[Future, APIError](someRepos))
                .once()

            whenReady(testService.getUserRepos(urlOverride = Some(url), login = "someLogin").value) { result =>
                result shouldBe Right(someRepos)
            }
        }

        "return a connection error" in {
            val url: String = "testUrl"

            (mockConnector.getUserRepos[Repo](_: String)(_: OFormat[Repo], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.leftT[Future, Seq[Repo]](APIError.BadAPIResponse(500, "Could not connect.")))
                .once()

            whenReady(testService.getUserRepos(urlOverride = Some(url), login = "someLogin").value) { result =>
                result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect."))
            }
        }

        "return a can't find user error" in {
            val url: String = "testUrl"

            (mockConnector.getUserRepos[Repo](_: String)(_: OFormat[Repo], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.leftT[Future, Seq[Repo]](APIError.BadAPIResponse(400, "No user exists with this login.")))
                .once()

            whenReady(testService.getUserRepos(urlOverride = Some(url), login = "someLogin").value) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "No user exists with this login."))
            }
        }
    }
}
