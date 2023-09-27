package models

import play.api.libs.json.{Json, OFormat}

case class RepoFile(
    name: String,
    path: String,
    url: String,
    html_url: String,
    `type`: String,
    content: String,
    encoding: String
)

object RepoFile {
    implicit val formats: OFormat[RepoFile] = Json.format[RepoFile]
}
