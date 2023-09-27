package controllers

import baseSpec.BaseSpecWithApplication
import models.{Owner, Repo, User}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.{contentAsJson, contentType, defaultAwaitTimeout, flash, header, headers, redirectLocation, route, status}

import scala.concurrent.Future

class GitHubControllerSpec extends BaseSpecWithApplication {
    val TestGitHubController: GitHubController = new GitHubController(component, repoService, service)

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

    private val myUser: User = User(
        "Aashvin",
        "2019-09-25T12:57:18Z",
        None,
        2,
        2
    )

    "GitHubController .index()" should {

        "return Ok" in {
            val result = TestGitHubController.index()(FakeRequest())
            status(result) shouldBe Status.OK
            contentAsJson(result) shouldBe Json.toJson(Seq[User]())
        }
    }

    "GitHubController .create()" should {

        "create a new user in the database" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult: Future[Result] = TestGitHubController.create()(request)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            afterEach()
        }

        "give a bad input error" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.obj())
            val createResult: Future[Result] = TestGitHubController.create()(request)

            status(createResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(createResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason Input can't be parsed as a User.")

            afterEach()
        }

        "give a user already exists error" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult: Future[Result] = TestGitHubController.create()(request)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            val request2: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult2: Future[Result] = TestGitHubController.create()(request2)

            status(createResult2) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(createResult2) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason A user with this login already exists.")

            afterEach()
        }

    }

    "GitHubController .read()" should {

        "find a user by login in the database" in {
            beforeEach()

            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult: Future[Result] = TestGitHubController.create()(createRequest)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/read/${testUser.login}")
            val readResult: Future[Result] = TestGitHubController.read(testUser.login)(readRequest)

            status(readResult) shouldBe Status.OK

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/read/${testUser.login}")
            val readResult: Future[Result] = TestGitHubController.read(testUser.login)(readRequest)

            status(readResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(readResult) shouldBe Json.toJson("Bad response from upstream; got status: 404, and got reason User not found.")

            afterEach()
        }
    }

    "GitHubController .update()" should {

        "update a user in the database" in {
            beforeEach()

            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult: Future[Result] = TestGitHubController.create()(createRequest)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            val updateRequest: FakeRequest[JsValue] = buildPut(s"/update/${testUser.login}").withBody[JsValue](Json.toJson(updatedTestUser))
            val updateResult: Future[Result] = TestGitHubController.update(testUser.login)(updateRequest)

            status(updateResult) shouldBe Status.ACCEPTED
            contentAsJson(updateResult) shouldBe Json.toJson(updatedTestUser)

            afterEach()
        }

        "give a bad input error" in {
            beforeEach()

            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult: Future[Result] = TestGitHubController.create()(createRequest)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            val updateRequest: FakeRequest[JsValue] = buildPut(s"/update/${testUser.login}").withBody[JsValue](Json.obj())
            val updateResult: Future[Result] = TestGitHubController.update(testUser.login)(updateRequest)

            status(updateResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(updateResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason Input can't be parsed as a User.")

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult: Future[Result] = TestGitHubController.create()(createRequest)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            val updateRequest: FakeRequest[JsValue] = buildPut(s"/update/doesNotExist").withBody[JsValue](Json.toJson(updatedTestUser))
            val updateResult: Future[Result] = TestGitHubController.update("doesNotExist")(updateRequest)

            status(updateResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(updateResult) shouldBe Json.toJson("Bad response from upstream; got status: 404, and got reason User not found.")

            afterEach()
        }

        "give an attempt to update login error" in {
            beforeEach()

            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult: Future[Result] = TestGitHubController.create()(createRequest)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            val updateRequest: FakeRequest[JsValue] = buildPut(s"/update/${testUser.login}").withBody[JsValue](Json.toJson(differentLoginTestUser))
            val updateResult: Future[Result] = TestGitHubController.update(testUser.login)(updateRequest)

            status(updateResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(updateResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason The updated login needs to be the same as the current login.")

            afterEach()
        }
    }

    "GitHubController .delete()" should {

        "delete a user from the database" in {
            beforeEach()

            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))
            val createResult: Future[Result] = TestGitHubController.create()(createRequest)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(testUser)

            val deleteRequest: FakeRequest[AnyContent] = buildDelete(s"/delete/${testUser.login}")
            val deleteResult: Future[Result] = TestGitHubController.delete(testUser.login)(deleteRequest)

            status(deleteResult) shouldBe Status.ACCEPTED

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val deleteRequest: FakeRequest[AnyContent] = buildDelete(s"/delete/${testUser.login}")
            val deleteResult: Future[Result] = TestGitHubController.delete(testUser.login)(deleteRequest)

            status(deleteResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(deleteResult) shouldBe Json.toJson("Bad response from upstream; got status: 404, and got reason User not found.")

            afterEach()
        }
    }

    "GitHubController .getGitHubUser()" should {

        "return Ok" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/github/users/Aashvin")
            val readResult: Future[Result] = TestGitHubController.getGitHubUser("Aashvin")(readRequest)

            status(readResult) shouldBe Status.OK

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/github/users/Aashvins")
            val readResult: Future[Result] = TestGitHubController.getGitHubUser("Aashvins")(readRequest)

            status(readResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(readResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason No user exists with this login.")

            afterEach()
        }
    }

    "GitHubController .createFromGitHub()" should {

        "create a new user from GitHub in the database" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet("/addgithubuser/Aashvin")
            val readResult: Future[Result] = TestGitHubController.createFromGitHub("Aashvin")(readRequest)

            status(readResult) shouldBe Status.SEE_OTHER
            redirectLocation(readResult) shouldBe Some(s"/read/Aashvin")

            afterEach()
        }

        "give a user already exists error" in {
            beforeEach()

            val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(myUser))
            val createResult: Future[Result] = TestGitHubController.create()(request)

            status(createResult) shouldBe Status.CREATED
            contentAsJson(createResult) shouldBe Json.toJson(myUser)

            val readRequest: FakeRequest[AnyContent] = buildGet("/addgithubuser/Aashvin")
            val readResult: Future[Result] = TestGitHubController.createFromGitHub("Aashvin")(readRequest)

            status(readResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(readResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason A user with this login already exists.")

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/addgithubuser/Aashvins")
            val readResult: Future[Result] = TestGitHubController.createFromGitHub("Aashvins")(readRequest)

            status(readResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(readResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason No user exists with this login.")

            afterEach()
        }
    }

    "GitHubController .getGitHubUserRepos()" should {
        val repo1: Repo = Repo("name1", `private` = false, Owner("owner1"), Some("description1"), "url1", "01/01/2001", "02/02/2002", "public", 0, 0, 0, "main")
        val repo2: Repo = Repo("name2", `private` = false, Owner("owner2"), Some("description2"), "url2", "03/03/2003", "04/04/2004", "public", 2, 3, 10, "main")
        val someRepos: Seq[Repo] = Seq(repo1, repo2)

        "return Ok" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/github/users/Aashvin/repos")
            val readResult: Future[Result] = TestGitHubController.getGitHubUserRepos("Aashvin")(readRequest)

            status(readResult) shouldBe Status.OK

            afterEach()
        }

        "give a user not found error" in {
            beforeEach()

            val readRequest: FakeRequest[AnyContent] = buildGet(s"/github/users/Aashvins/repos")
            val readResult: Future[Result] = TestGitHubController.getGitHubUserRepos("Aashvins")(readRequest)

            status(readResult) shouldBe Status.INTERNAL_SERVER_ERROR
            contentAsJson(readResult) shouldBe Json.toJson("Bad response from upstream; got status: 400, and got reason No user exists with this login.")

            afterEach()
        }
    }

    override def beforeEach(): Unit = repository.deleteAll()
    override def afterEach(): Unit = repository.deleteAll()
}
