package controllers

import baseSpec.BaseSpecWithApplication
import cats.data.{EitherT, OptionT}
import models.{APIError, Owner, Repo, RepoContents, RepoFile, User}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.test.FakeRequest
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers.{contentAsJson, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{GitHubService, RepositoryService}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ExecutionContext, Future}

class GitHubControllerSpec extends BaseSpecWithApplication with MockFactory with ScalaFutures {
    val mockRepoService: RepositoryService = mock[RepositoryService]
    val mockGitHubService: GitHubService = mock[GitHubService]
    val TestGitHubController: GitHubController = new GitHubController(component, mockRepoService, mockGitHubService)
//    implicit val rdsContents: OFormat[RepoContents] = app.injector.instanceOf[OFormat[RepoContents]]
//    implicit val rdsFile: OFormat[RepoFile] = app.injector.instanceOf[OFormat[RepoFile]]

    private val testUser: User = User(
        "testLogin",
        "01/01/2001",
        Some("testLocation"),
        100,
        3
    )

    private val updatedTestUser: User = User(
        "testLogin",
        "01/01/2001",
        Some("updatedTestLocation"),
        200,
        5
    )

    private val differentLoginTestUser: User = User(
        "differentTestLogin",
        "01/01/2001",
        Some("updatedTestLocation"),
        200,
        5
    )

    val repo1: Repo = Repo(name = "name1", `private` = false, owner = Owner("owner1"), description = Some("description1"), url = "url1", created_at = "01/01/2001", updated_at = "02/02/2002", visibility = "public", forks = 0, open_issues = 0, watchers = 0, default_branch = "main")
    val repo2: Repo = Repo(name = "name2", `private` = false, owner = Owner("owner2"), description = Some("description2"), url = "url2", created_at = "03/03/2003", updated_at = "04/04/2004", visibility = "public", forks = 2, open_issues = 3, watchers = 10, default_branch = "main")
    val sequenceOfRepos: Seq[Repo] = Seq(repo1, repo2)

    val repoContents1: RepoContents = RepoContents(name = "name1", path = "path1", url = "url1", html_url = "html_url1", `type` = "file")
    val repoContents2: RepoContents = RepoContents(name = "name2", path = "path2", url = "url2", html_url = "html_url2", `type` = "dir")
    val sequenceOfRepoContents: Seq[RepoContents] = Seq(repoContents1, repoContents2)

    val testRepoFile: RepoFile = RepoFile("name1", "path1", "url1", "html_url1", "file", "content1", "base64")

    "GitHubController .index()" should {

        "return Ok" in {
            val request: FakeRequest[AnyContent] = FakeRequest()

            val sequenceOfUsers = Seq(testUser, updatedTestUser, differentLoginTestUser)

            (mockRepoService.index _)
                .expects
                .returning(Future(Right(sequenceOfUsers)))
                .once()

            val result = TestGitHubController.index()(request)

            status(result) shouldBe Status.OK
            contentAsJson(result) shouldBe Json.toJson(sequenceOfUsers)
        }
    }

    "GitHubController .create()" should {

        "create a new user in the database" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))

            (mockRepoService.create(_: Request[JsValue]))
                .expects(request)
                .returning(Future(Right(testUser)))
                .once()

            val createResult: Future[Result] = TestGitHubController.create()(request)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            afterEach()
        }

        "give a bad input error" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.obj())

            (mockRepoService.create(_: Request[JsValue]))
                .expects(request)
                .returning(Future(Left(APIError.BadAPIResponse(400, "Input can't be parsed as a User."))))
                .once()

            val createResult: Future[Result] = TestGitHubController.create()(request)

            status(createResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(createResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason Input can't be parsed as a User.")

            afterEach()
        }

        "give a user already exists error" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))

            (mockRepoService.create(_: Request[JsValue]))
                .expects(request)
                .returning(Future(Left(APIError.BadAPIResponse(400, "A user with this login already exists."))))
                .once()

            val createResult: Future[Result] = TestGitHubController.create()(request)

            status(createResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(createResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason A user with this login already exists.")

            afterEach()
        }
    }

    "GitHubController .read()" should {

        "find a user by login in the database" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/read/${testUser.login}")

            (mockRepoService.read(_: String))
                .expects(testUser.login)
                .returning(Future(Right(testUser)))
                .once()

            val readResult: Future[Result] = TestGitHubController.read(testUser.login)(request)

            status(readResult) shouldBe Status.OK

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/read/userDoesNotExist")

            (mockRepoService.read(_: String))
                .expects("userDoesNotExist")
                .returning(Future(Left(APIError.BadAPIResponse(404, "User not found."))))
                .once()

            val readResult: Future[Result] = TestGitHubController.read("userDoesNotExist")(readRequest)

            status(readResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(readResult) shouldBe Json.toJson("Bad response from upstream; got status: 404, and got reason User not found.")

            afterEach()
        }
    }

    "GitHubController .update()" should {

        "update a user in the database" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPut(s"/update/${testUser.login}").withBody[JsValue](Json.toJson(updatedTestUser))

            (mockRepoService.update(_: Request[JsValue], _: String))
                .expects(request, testUser.login)
                .returning(Future(Right(updatedTestUser)))
                .once()

            val result: Future[Result] = TestGitHubController.update(testUser.login)(request)

            status(result) shouldBe Status.ACCEPTED
            contentAsJson(result) shouldBe Json.toJson(updatedTestUser)

            afterEach()
        }

        "give a bad input error" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPut(s"/update/${testUser.login}").withBody[JsValue](Json.obj())

            (mockRepoService.update(_: Request[JsValue], _: String))
                .expects(request, testUser.login)
                .returning(Future(Left(APIError.BadAPIResponse(400, "Input can't be parsed as a User."))))
                .once()

            val result: Future[Result] = TestGitHubController.update(testUser.login)(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason Input can't be parsed as a User.")

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPut(s"/update/userDoesNotExist").withBody[JsValue](Json.toJson(updatedTestUser))

            (mockRepoService.update(_: Request[JsValue], _: String))
                .expects(request, "userDoesNotExist")
                .returning(Future(Left(APIError.BadAPIResponse(404, "User not found."))))
                .once()

            val result: Future[Result] = TestGitHubController.update("userDoesNotExist")(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 404, and got reason User not found.")

            afterEach()
        }

        "give an attempt to update login error" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPut(s"/update/${testUser.login}").withBody[JsValue](Json.toJson(differentLoginTestUser))

            (mockRepoService.update(_: Request[JsValue], _: String))
                .expects(request, testUser.login)
                .returning(Future(Left(APIError.BadAPIResponse(400, "The updated login needs to be the same as the current login."))))
                .once()

            val result: Future[Result] = TestGitHubController.update(testUser.login)(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason The updated login needs to be the same as the current login.")

            afterEach()
        }
    }

    "GitHubController .delete()" should {

        "delete a user from the database" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildDelete(s"/delete/${testUser.login}")

            (mockRepoService.delete(_: String))
                .expects(testUser.login)
                .returning(Future(Right(true)))
                .once()

            val result: Future[Result] = TestGitHubController.delete(testUser.login)(request)

            status(result) shouldBe Status.ACCEPTED

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildDelete(s"/delete/userDoesNotExist")

            (mockRepoService.delete(_: String))
                .expects("userDoesNotExist")
                .returning(Future(Left(APIError.BadAPIResponse(404, "User not found."))))
                .once()

            val result: Future[Result] = TestGitHubController.delete("userDoesNotExist")(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 404, and got reason User not found.")

            afterEach()
        }
    }

    "GitHubController .getGitHubUser()" should {

        "return Ok" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/${testUser.login}")

            (mockGitHubService.getUser(_: Option[String], _: String)(_: ExecutionContext))
                .expects(None, testUser.login, executionContext)
                .returning(EitherT.rightT(testUser))
                .once()

            val result: Future[Result] = TestGitHubController.getGitHubUser(testUser.login)(request)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe contentAsString(views.html.viewUser(testUser))

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/userDoesNotExist")

            (mockGitHubService.getUser(_: Option[String], _: String)(_: ExecutionContext))
                .expects(None, "userDoesNotExist", executionContext)
                .returning(EitherT.leftT(APIError.BadAPIResponse(400, "No user exists with this login.")))
                .once()

            val result: Future[Result] = TestGitHubController.getGitHubUser("userDoesNotExist")(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason No user exists with this login.")

            afterEach()
        }
    }

    "GitHubController .createFromGitHub()" should {

        "create a new user from GitHub in the database" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/addgithubuser/${testUser.login}")

            (mockGitHubService.getUser(_: Option[String], _: String)(_: ExecutionContext))
                .expects(None, testUser.login, executionContext)
                .returning(EitherT.rightT(testUser))
                .once()

            (mockRepoService.createFromGitHub(_: User))
                .expects(testUser)
                .returning(Future(Right(testUser)))
                .once()

            val readResult: Future[Result] = TestGitHubController.createFromGitHub(testUser.login)(readRequest)

            status(readResult) shouldBe Status.SEE_OTHER
            redirectLocation(readResult) shouldBe Some(s"/read/${testUser.login}")

            afterEach()
        }

        "give a user already exists error" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/addgithubuser/${testUser.login}")

            (mockGitHubService.getUser(_: Option[String], _: String)(_: ExecutionContext))
                .expects(None, testUser.login, executionContext)
                .returning(EitherT.rightT(testUser))
                .once()

            (mockRepoService.createFromGitHub(_: User))
                .expects(testUser)
                .returning(Future(Left(APIError.BadAPIResponse(400, "A user with this login already exists."))))
                .once()

            val result: Future[Result] = TestGitHubController.createFromGitHub(testUser.login)(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason A user with this login already exists.")

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/addgithubuser/userDoesNotExist")

            (mockGitHubService.getUser(_: Option[String], _: String)(_: ExecutionContext))
                .expects(None, "userDoesNotExist", executionContext)
                .returning(EitherT.leftT(APIError.BadAPIResponse(400, "No user exists with this login.")))
                .once()

            val result: Future[Result] = TestGitHubController.createFromGitHub("userDoesNotExist")(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason No user exists with this login.")

            afterEach()
        }
    }

    "GitHubController .getGitHubUserRepos()" should {

        "return Ok" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/testUser/repos")

            (mockGitHubService.getUserRepos(_: Option[String], _: String)(_: ExecutionContext))
                .expects(None, "testUser", executionContext)
                .returning(EitherT.rightT(sequenceOfRepos))
                .once()

            val result: Future[Result] = TestGitHubController.getGitHubUserRepos("testUser")(request)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe contentAsString(views.html.viewRepos("testUser", sequenceOfRepos))

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/userDoesNotExist/repos")

            (mockGitHubService.getUserRepos(_: Option[String], _: String)(_: ExecutionContext))
                .expects(None, "userDoesNotExist", executionContext)
                .returning(EitherT.leftT(APIError.BadAPIResponse(400, "No user exists with this login.")))
                .once()

            val result: Future[Result] = TestGitHubController.getGitHubUserRepos("userDoesNotExist")(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason No user exists with this login.")

            afterEach()
        }
    }

    "GitHubController .getGitHubRepo()" should {

        "return Ok with the content of the root of the repo" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/testUser/repos/testRepo")

            (mockGitHubService.getRepoContents[RepoContents](_: Option[String], _: String, _: String, _: String)(_: OFormat[RepoContents], _: ExecutionContext))
                .expects(None, "testUser", "testRepo", "", *, executionContext)
                .returning(OptionT.some(Right(sequenceOfRepoContents)))
                .once()

            val result: Future[Result] = TestGitHubController.getGitHubRepo("testUser", "testRepo")(request)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe contentAsString(views.html.viewRepoContents("testUser", "testRepo", sequenceOfRepoContents, ""))

            afterEach()
        }

        "give a repo contents path does not exist error" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/testUser/repos/repoDoesNotExist")

            (mockGitHubService.getRepoContents(_: Option[String], _: String, _: String, _: String)(_: OFormat[RepoContents], _: ExecutionContext))
                .expects(None, "testUser", "repoDoesNotExist", "", *, executionContext)
                .returning(OptionT.some(Left(APIError.BadAPIResponse(400, "This repo contents path does not exist."))))
                .once()

            val result: Future[Result] = TestGitHubController.getGitHubRepo("testUser", "repoDoesNotExist")(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(result) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason This repo contents path does not exist.")

            afterEach()
        }
    }

    "GitHubController .getGitHubRepoContents()" should {

        "return Ok when retrieving a file" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/testUser/repos/testRepo/testFile")

            (mockGitHubService.getRepoContents[RepoFile](_: Option[String], _: String, _: String, _: String)(_: OFormat[RepoFile], _: ExecutionContext))
                .expects(None, "testUser", "testRepo", "testFile", *, executionContext)
                .returning(OptionT.some(Right(Seq(testRepoFile))))
                .once()

            val result: Future[Result] = TestGitHubController.getGitHubRepoContents("testUser", "testRepo", "testFile")(request)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe contentAsString(views.html.viewRepoFile("testUser", "testRepo", testRepoFile))

            afterEach()
        }

        "return Ok when retrieving a directory" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/testUser/repos/testRepo/testDir")

            (mockGitHubService.getRepoContents[RepoFile](_: Option[String], _: String, _: String, _: String)(_: OFormat[RepoFile], _: ExecutionContext))
                .expects(None, "testUser", "testRepo", "testDir", *, executionContext)
                .returning(OptionT.none)
                .once()

            (mockGitHubService.getRepoContents[RepoContents](_: Option[String], _: String, _: String, _: String)(_: OFormat[RepoContents], _: ExecutionContext))
                .expects(None, "testUser", "testRepo", "testDir", *, executionContext)
                .returning(OptionT.some(Right(sequenceOfRepoContents)))
                .once()

            val result: Future[Result] = TestGitHubController.getGitHubRepoContents("testUser", "testRepo", "testDir")(request)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe contentAsString(views.html.viewRepoContents("testUser", "testRepo", sequenceOfRepoContents, "testDir"))

            afterEach()
        }

        "give a repo contents does not exist error" in {
            beforeEach()

            val request: FakeRequest[AnyContent] = buildGet(s"/github/users/testUser/repos/testRepo/content/does/not/exist")

            (mockGitHubService.getRepoContents[RepoFile](_: Option[String], _: String, _: String, _: String)(_: OFormat[RepoFile], _: ExecutionContext))
                .expects(None, "testUser", "testRepo", "content/does/not/exist", *, executionContext)
                .returning(OptionT.none)
                .once()

            (mockGitHubService.getRepoContents[RepoContents](_: Option[String], _: String, _: String, _: String)(_: OFormat[RepoContents], _: ExecutionContext))
                .expects(None, "testUser", "testRepo", "content/does/not/exist", *, executionContext)
                .returning(OptionT.none)
                .once()

//            (mockGitHubService.getRepoContents(_: Option[String], _: String, _: String, _: Option[String])(_: ExecutionContext))
//                .expects(None, "testUser", "testRepo", Some("content/does/not/exist"), executionContext)
//                .returning(EitherT.leftT(APIError.BadAPIResponse(400, "This repo contents path does not exist.")))
//                .once()

            val result: Future[Result] = TestGitHubController.getGitHubRepoContents("testUser", "testRepo", "content/does/not/exist")(request)

            println(contentAsString(result))
            status(result) shouldBe Status.BAD_REQUEST
            contentAsJson(result) shouldBe Json.toJson("This repo content does not exist.")

            afterEach()
        }
    }

    override def beforeEach(): Unit = repository.deleteAll()
    override def afterEach(): Unit = repository.deleteAll()
}
