package services

import baseSpec.BaseSpec
import cats.data.EitherT
import connectors.GitHubConnector
import models.{APIError, Owner, Repo, RepoContents, RepoFile, User}
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

    val repoContents1: RepoContents = RepoContents("name1", "path1", "url1", "html_url1", "file")
    val repoContents2: RepoContents = RepoContents("name2", "path2", "url2", "html_url2", "dir")
    val someRepoContents: Seq[RepoContents] = Seq(repoContents1, repoContents2)

    val repoFile: RepoFile = RepoFile("name1", "path1", "url1", "html_url1", "file", "content1", "base64")

    "GitHubService .getUser()" should {
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

    "GitHubService .getUserRepos()" should {
        val url: String = "testUrl"

        "return a sequence of repos" in {
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

    "GitHubService .getRepoContents()" should {
        val url: String = "testUrl"

        "return a sequence of repo contents" in {
            (mockConnector.getRepoContents[RepoContents](_: String)(_: OFormat[RepoContents], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.rightT[Future, APIError](someRepoContents))
                .once()

            whenReady(testService.getRepoContents(urlOverride = Some(url), login = "someLogin", repoName = "someRepo", path = None).value) { result =>
                result shouldBe Right(someRepoContents)
            }
        }

        "return a connection error" in {
            val url: String = "testUrl"

            (mockConnector.getRepoContents[RepoContents](_: String)(_: OFormat[RepoContents], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.leftT[Future, Seq[RepoContents]](APIError.BadAPIResponse(500, "Could not connect.")))
                .once()

            whenReady(testService.getRepoContents(urlOverride = Some(url), login = "someLogin", repoName = "someRepo", path = None).value) { result =>
                result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect."))
            }
        }

        "return a can't find path error" in {
            val url: String = "testUrl"

            (mockConnector.getRepoContents[RepoContents](_: String)(_: OFormat[RepoContents], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.leftT[Future, Seq[RepoContents]](APIError.BadAPIResponse(400, "This repo contents path does not exist.")))
                .once()

            whenReady(testService.getRepoContents(urlOverride = Some(url), login = "someLogin", repoName = "someRepo", path = None).value) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "This repo contents path does not exist."))
            }
        }
    }

    "GitHubService .getRepoFile()" should {
        val url: String = "testUrl"

        "return a repo file" in {
            (mockConnector.getRepoFile[RepoFile](_: String)(_: OFormat[RepoFile], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.rightT[Future, APIError](repoFile))
                .once()

            whenReady(testService.getRepoFile(urlOverride = Some(url), login = "someLogin", repoName = "someRepo", path = "somePath").value) { result =>
                result shouldBe Right(repoFile)
            }
        }

        "return a connection error" in {
            val url: String = "testUrl"

            (mockConnector.getRepoFile[RepoFile](_: String)(_: OFormat[RepoFile], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.leftT[Future, RepoFile](APIError.BadAPIResponse(500, "Could not connect.")))
                .once()

            whenReady(testService.getRepoFile(urlOverride = Some(url), login = "someLogin", repoName = "someRepo", path = "somePath").value) { result =>
                result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect."))
            }
        }

        "return a can't find path error" in {
            val url: String = "testUrl"

            (mockConnector.getRepoFile[RepoFile](_: String)(_: OFormat[RepoFile], _: ExecutionContext))
                .expects(url, *, *)
                .returning(EitherT.leftT[Future, RepoFile](APIError.BadAPIResponse(400, "This repo file does not exist.")))
                .once()

            whenReady(testService.getRepoFile(urlOverride = Some(url), login = "someLogin", repoName = "someRepo", path = "somePath").value) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "This repo file does not exist."))
            }
        }
    }
}
