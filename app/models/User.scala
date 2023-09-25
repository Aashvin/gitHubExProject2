package models

import play.api.libs.json.{OFormat, Json}

case class User(
    login: String,
    created_at: String,
    location: Option[String],
    followers: Int,
    following: Int
)

object User {
    implicit val formats: OFormat[User] = Json.format[User]
}
