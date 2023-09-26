package models

import play.api.libs.json.{Json, OFormat}

case class Owner(
    login: String,
)

object Owner {
    implicit val formats: OFormat[Owner] = Json.format[Owner]
}

case class Repo(
    name: String,
    `private`: Boolean,
    owner: Owner,
    description: Option[String],
    url: String,
    created_at: String,
    updated_at: String,
    visibility: String,
    forks: Int,
    open_issues: Int,
    watchers: Int,
    default_branch: String
)

object Repo {
    implicit val formats: OFormat[Repo] = Json.format[Repo]
}