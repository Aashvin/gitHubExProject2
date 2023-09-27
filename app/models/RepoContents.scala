package models

import play.api.libs.json.{Json, OFormat}

case class RepoContents(
    name: String,
    path: String,
    url: String,
    html_url: String,
    `type`: String
)

object RepoContents {
    implicit val formats: OFormat[RepoContents] = Json.format[RepoContents]
}