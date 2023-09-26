package services

import baseSpec.BaseSpecWithApplication
import models.{APIError, User}
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import repositories.RepositoryTrait

import scala.concurrent.Future

class RepositoryServiceSpec extends BaseSpecWithApplication with MockFactory {
    val mockRepository: RepositoryTrait = mock[RepositoryTrait]
    val TestRepositoryService = new RepositoryService(mockRepository)

    private val testUser: User = User(
        "testLogin",
        "01/01/2001",
        Some("testLocation"),
        100,
        3
    )

    private val testUser2: User = User(
        "testLogin2",
        "02/02/2002",
        Some("testLocation2"),
        200,
        200
    )

    private val differentLoginTestUser: User = User(
        "differentTestLogin",
        "01/01/2001",
        Some("updatedTestLocation"),
        200,
        5
    )

    "RepositoryService .index()" should {

        val sequenceForIndex = Seq(testUser, testUser2)

        "get a sequence of users" in {
            (() => mockRepository.index()).expects()
                .returning(Future(Right(sequenceForIndex))).once()

            whenReady(TestRepositoryService.index()) { result =>
                result shouldBe Right(sequenceForIndex)
            }
        }

        "give an API error" in {
            (() => mockRepository.index()).expects()
                .returning(Future(Left(APIError.BadAPIResponse(400, "Could not retrieve users.")))).once()

            whenReady(TestRepositoryService.index()) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "Could not retrieve users."))
            }
        }
    }

    "RepositoryService .create()" should {

        "get and return a user from the database" in {
            (mockRepository.create(_: User)).expects(testUser)
                .returning(Future(Right(testUser))).once()

            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))

            whenReady(TestRepositoryService.create(createRequest)) { result =>
                result shouldBe Right(testUser)
            }
        }

        "validate an incorrect user model input" in {
            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.obj())

            whenReady(TestRepositoryService.create(createRequest)) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "Input can't be parsed as a User."))
            }
        }

        "give a user already exists error" in {
            (mockRepository.create(_: User)).expects(testUser)
                .returning(Future(Left(APIError.BadAPIResponse(400, "A user with this login already exists.")))).once()

            val createRequest: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(testUser))

            whenReady(TestRepositoryService.create(createRequest)) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "A user with this login already exists."))
            }
        }
    }

    "RepositoryService .read()" should {

        "get a user" in {
            (mockRepository.read(_: String)).expects(testUser.login)
                .returning(Future(Right(testUser))).once()

            whenReady(TestRepositoryService.read(testUser.login)) { result =>
                result shouldBe Right(testUser)
            }
        }

        "give a user not found error" in {
            (mockRepository.read(_: String)).expects("fakeLogin")
                .returning(Future(Left(APIError.BadAPIResponse(404, "User not found.")))).once()

            whenReady(TestRepositoryService.read("fakeLogin")) { result =>
                result shouldBe Left(APIError.BadAPIResponse(404, "User not found."))
            }
        }
    }

    "RepositoryService .update()" should {

        "update a user" in {
            (mockRepository.update(_: String, _: User)).expects(testUser.login, testUser2)
                .returning(Future(Right(testUser2))).once()

            val updateRequest: FakeRequest[JsValue] = buildPost(s"/update/${testUser.login}").withBody[JsValue](Json.toJson(testUser2))

            whenReady(TestRepositoryService.update(updateRequest, testUser.login)) { result =>
                result shouldBe Right(testUser2)
            }
        }

        "validate an incorrect user model input" in {
            val updateRequest: FakeRequest[JsValue] = buildPost(s"/update/${testUser.login}").withBody[JsValue](Json.obj())

            whenReady(TestRepositoryService.update(updateRequest, testUser.login)) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "Input can't be parsed as a User."))
            }
        }

        "give a user not found error" in {
            (mockRepository.update(_: String, _: User)).expects("fakeLogin", testUser2)
                .returning(Future(Left(APIError.BadAPIResponse(404, "User not found.")))).once()

            val updateRequest: FakeRequest[JsValue] = buildPost(s"/update/fakeLogin").withBody[JsValue](Json.toJson(testUser2))

            whenReady(TestRepositoryService.update(updateRequest, "fakeLogin")) { result =>
                result shouldBe Left(APIError.BadAPIResponse(404, "User not found."))
            }
        }

        "give an attempt to update login error" in {
            (mockRepository.update(_: String, _: User)).expects(testUser.login, differentLoginTestUser)
                .returning(Future(Left(APIError.BadAPIResponse(400, "The updated login needs to be the same as the current login.")))).once()

            val updateRequest: FakeRequest[JsValue] = buildPost(s"/update/${testUser.login}").withBody[JsValue](Json.toJson(differentLoginTestUser))

            whenReady(TestRepositoryService.update(updateRequest, testUser.login)) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "The updated login needs to be the same as the current login."))
            }
        }
    }

    "RepositoryService .delete()" should {

        "remove a user" in {
            (mockRepository.delete(_: String)).expects(testUser.login)
                .returning(Future(Right(true))).once()

            whenReady(TestRepositoryService.delete(testUser.login)) { result =>
                result shouldBe Right(true)
            }
        }

        "give a user not found error" in {
            (mockRepository.delete(_: String)).expects("fakeLogin")
                .returning(Future(Left(APIError.BadAPIResponse(404, "User not found.")))).once()

            whenReady(TestRepositoryService.delete("fakeLogin")) { result =>
                result shouldBe Left(APIError.BadAPIResponse(404, "User not found."))
            }
        }
    }

    "RepositoryService .createFromGitHub()" should {

        "get and return a user from the database" in {
            (mockRepository.create(_: User)).expects(testUser)
                .returning(Future(Right(testUser))).once()

            whenReady(TestRepositoryService.createFromGitHub(testUser)) { result =>
                result shouldBe Right(testUser)
            }
        }

        "give a user already exists error" in {
            (mockRepository.create(_: User)).expects(testUser)
                .returning(Future(Left(APIError.BadAPIResponse(400, "A user with this login already exists.")))).once()

            whenReady(TestRepositoryService.createFromGitHub(testUser)) { result =>
                result shouldBe Left(APIError.BadAPIResponse(400, "A user with this login already exists."))
            }
        }
    }
}
