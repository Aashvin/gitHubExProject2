package connectors

import baseSpec.BaseSpec
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import models.{APIError, Owner, Repo, RepoContents, RepoFile, User}
import models.User.formats
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.ws.WSClient
import play.api.test.Injecting
import Responses._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class GitHubConnectorSpec extends BaseSpec with Injecting with Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {
    implicit val ws: WSClient = app.injector.instanceOf[WSClient]
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    val wireMockServer = new WireMockServer(wireMockConfig().port(9000))
    val testConnector = new GitHubConnector(ws = ws)

    val userResponseAsUser: User = User(
        login = "Aashvin",
        created_at = "2019-09-25T12:57:18Z",
        location = None,
        followers = 2,
        following = 2
    )

    val incorrectResponse: String =
        """{
          |  "login": "Aashvin",
          |  "followers": 2,
          |  "following": 2
          |}""".stripMargin

    val repoResponseAsRepos: Seq[Repo] = Seq(
        Repo(
            name = "AutoFAQ-Website-Link",
            `private` = false,
            owner = Owner(login = "Aashvin"),
            description = None,
            url = "https://api.github.com/repos/Aashvin/AutoFAQ-Website-Link",
            created_at = "2021-09-24T15:10:17Z",
            updated_at = "2021-09-24T15:10:51Z",
            visibility = "public",
            forks = 0,
            open_issues = 0,
            watchers = 0,
            default_branch = "main"
        ),
        Repo(
            name = "COMP0031-PlantBot",
            `private` = false,
            owner = Owner(login = "Aashvin"),
            description = None,
            url = "https://api.github.com/repos/Aashvin/COMP0031-PlantBot",
            created_at = "2022-01-31T13:47:34Z",
            updated_at = "2022-03-26T10:50:25Z",
            visibility = "public",
            forks = 0,
            open_issues = 0,
            watchers = 0,
            default_branch = "main"
        )
    )

    val repoContentsResponseAsRepoContents: Seq[RepoContents] = Seq(
        RepoContents(
            name = ".gitignore",
            path = ".gitignore",
            url = "https://api.github.com/repos/Aashvin/COMP0031-PlantBot/contents/.gitignore?ref=main",
            html_url = "https://github.com/Aashvin/COMP0031-PlantBot/blob/main/.gitignore",
            `type` = "file"
        ),
        RepoContents(
            name = "README.md",
            path = "README.md",
            url = "https://api.github.com/repos/Aashvin/COMP0031-PlantBot/contents/README.md?ref=main",
            html_url = "https://github.com/Aashvin/COMP0031-PlantBot/blob/main/README.md",
            `type` = "file"
        ),
        RepoContents(
            name = "plantbot",
            path = "plantbot",
            url = "https://api.github.com/repos/Aashvin/COMP0031-PlantBot/contents/plantbot?ref=main",
            html_url = "https://github.com/Aashvin/COMP0031-PlantBot/tree/main/plantbot",
            `type` = "dir"
        ),
        RepoContents(
            name = "yolo-configs",
            path = "yolo-configs",
            url = "https://api.github.com/repos/Aashvin/COMP0031-PlantBot/contents/yolo-configs?ref=main",
            html_url = "https://github.com/Aashvin/COMP0031-PlantBot/tree/main/yolo-configs",
            `type` = "dir"
        ),
    )

    val repoFileResponseAsRepoFile: RepoFile = RepoFile(
        name = ".gitignore",
        path = ".gitignore",
        url = "https://api.github.com/repos/Aashvin/COMP0031-PlantBot/contents/.gitignore?ref=main",
        html_url = "https://github.com/Aashvin/COMP0031-PlantBot/blob/main/.gitignore",
        `type` = "file",
        content = "KiovX19weWNhY2hlX18KKiovLnZzY29kZQoqKi5weWMKKiovLmlweW5iX2No\nZWNrcG9pbnRz\n",
        encoding = "base64"
    )

    "GitHubConnector .getUser()" should {

        "return a user" in {
            wireMockServer.start()

            wireMockServer.stubFor(get("/github/users/Aashvin")
                .willReturn(ok()
                    .withHeader("Content-Type", "text.html; charset=UTF-8")
                    .withBody(userResponse)
                )
            )

            Await.result(testConnector.getUser("http://localhost:9000/github/users/Aashvin").value.map {
                case Right(user: User) => user shouldBe userResponseAsUser
                case Left(error) => fail(s"Test failed from unexpected error:\n $error")
            }, 2.minute)

            wireMockServer.stop()
        }

        "give a no user exists error" in {
            wireMockServer.start()

            wireMockServer.stubFor(get("/github/users/Aashvin")
                .willReturn(ok()
                    .withHeader("Content-Type", "text.html; charset=UTF-8")
                    .withBody(incorrectResponse)
                )
            )

            Await.result(testConnector.getUser("http://localhost:9000/github/users/Aashvin").value.map {
                case Right(user: User) => fail(s"Test unexpectedly passed with user:\n$user")
                case Left(error: APIError.BadAPIResponse) =>
                    error.upstreamStatus shouldBe 400
                    error.upstreamMessage shouldBe "No user exists with this login."
            }, 2.minute)

            wireMockServer.stop()
        }
    }

    "GitHubConnector .getUserRepos()" should {

        "return a user's repos" in {
            wireMockServer.start()

            wireMockServer.stubFor(get("/github/users/Aashvin/repos")
                .willReturn(ok()
                    .withHeader("Content-Type", "text.html; charset=UTF-8")
                    .withBody(repoResponse)
                )
            )

            Await.result(testConnector.getUserRepos("http://localhost:9000/github/users/Aashvin/repos").value.map {
                case Right(repos: Seq[Repo]) => repos shouldBe repoResponseAsRepos
                case Left(error) => fail(s"Test failed from unexpected error:\n $error")
            }, 2.minute)

            wireMockServer.stop()
        }

        "give a no user exists error" in {
            wireMockServer.start()

            wireMockServer.stubFor(get("/github/users/Aashvin/repos")
                .willReturn(ok()
                    .withHeader("Content-Type", "text.html; charset=UTF-8")
                    .withBody(incorrectResponse)
                )
            )

            Await.result(testConnector.getUserRepos("http://localhost:9000/github/users/Aashvin/repos").value.map {
                case Right(repos: Seq[Repo]) => fail(s"Test unexpectedly passed with repo:\n$repos")
                case Left(error: APIError.BadAPIResponse) =>
                    error.upstreamStatus shouldBe 400
                    error.upstreamMessage shouldBe "No user exists with this login."
            }, 2.minute)

            wireMockServer.stop()
        }
    }

    "GitHubConnector .getRepoContents()" should {

        "return the contents of a repository" in {
            wireMockServer.start()

            wireMockServer.stubFor(get("/github/users/Aashvin/repos/COMP0031-PlantBot")
                .willReturn(ok()
                    .withHeader("Content-Type", "text.html; charset=UTF-8")
                    .withBody(repoContentsResponse)
                )
            )

            Await.result(testConnector.getRepoContents("http://localhost:9000/github/users/Aashvin/repos/COMP0031-PlantBot").value.map {
                case Right(repos: JsValue) => repos shouldBe Json.parse(repoContentsResponse)
                case Left(error) => fail(s"Test failed from unexpected error:\n $error")
            }, 2.minute)

            wireMockServer.stop()
        }

        //        "give a path does not exist error" in {
        //            wireMockServer.start()
        //
        //            wireMockServer.stubFor(get("/github/users/Aashvin/repos/COMP0031-PlantBot")
        //                .willReturn(ok()
        //                    .withHeader("Content-Type", "text.html; charset=UTF-8")
        //                    .withBody(incorrectResponse)
        //                )
        //            )
        //
        //            Await.result(testConnector.getRepoContents("http://localhost:9000/github/users/Aashvin/repos/COMP0031-PlantBot").value.map {
        //                case Right(repoContents: JsValue) => fail(s"Test unexpectedly received repo:\n$repoContents")
        //                case Left(error: APIError.BadAPIResponse) =>
        //                    error.upstreamStatus shouldBe 400
        //                    error.upstreamMessage shouldBe "This repo contents path does not exist."
        //            }, 2.minute)
        //
        //            wireMockServer.stop()
        //        }
    }

    //    "GitHubConnector .getRepoFile()" should {
    //
    //        "return a file within a repository" in {
    //            wireMockServer.start()
    //
    //            wireMockServer.stubFor(get("/github/users/Aashvin/repos/COMP0031-PlantBot/.gitignore")
    //                .willReturn(ok()
    //                    .withHeader("Content-Type", "text.html; charset=UTF-8")
    //                    .withBody(repoFileResponse)
    //                )
    //            )
    //
    //            Await.result(testConnector.getRepoFile("http://localhost:9000/github/users/Aashvin/repos/COMP0031-PlantBot/.gitignore").value.map {
    //                case Right(repoFile: RepoFile) => repoFile shouldBe repoFileResponseAsRepoFile
    //                case Left(error) => fail(s"Test failed from unexpected error:\n $error")
    //            }, 2.minute)
    //
    //            wireMockServer.stop()
    //        }
    //
    //        "give a file does not exist error" in {
    //            wireMockServer.start()
    //
    //            wireMockServer.stubFor(get("/github/users/Aashvin/repos/COMP0031-PlantBot")
    //                .willReturn(ok()
    //                    .withHeader("Content-Type", "text.html; charset=UTF-8")
    //                    .withBody(incorrectResponse)
    //                )
    //            )
    //
    //            Await.result(testConnector.getRepoFile("http://localhost:9000/github/users/Aashvin/repos/COMP0031-PlantBot").value.map {
    //                case Right(repoFile: RepoFile) => fail(s"Test unexpectedly passed with repo:\n$repoFile")
    //                case Left(error: APIError.BadAPIResponse) =>
    //                    error.upstreamStatus shouldBe 400
    //                    error.upstreamMessage shouldBe "This repo file does not exist."
    //            }, 2.minute)
    //
    //            wireMockServer.stop()
    //        }
    //    }
}