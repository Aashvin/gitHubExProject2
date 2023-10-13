package models

import play.api.libs.json.{Json, OFormat}

abstract class RepoContent (
    val name: String,
    val path: String,
    val url: String,
    val html_url: String,
    val `type`: String,
)

case class RepoContents (
    override val name: String,
    override val path: String,
    override val url: String,
    override val html_url: String,
    override val `type`: String,
) extends RepoContent(name, path, url, html_url, `type`)

object RepoContents {
    implicit val formats: OFormat[RepoContents] = Json.format[RepoContents]
}