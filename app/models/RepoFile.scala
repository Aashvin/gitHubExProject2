package models

import play.api.libs.json.{Json, OFormat}

case class RepoFile (
    override val name: String,
    override val path: String,
    override val url: String,
    override val html_url: String,
    override val `type`: String,
    content: String,
    encoding: String
) extends RepoContent(name, path, url, html_url, `type`)

object RepoFile {
    implicit val formats: OFormat[RepoFile] = Json.format[RepoFile]
}
