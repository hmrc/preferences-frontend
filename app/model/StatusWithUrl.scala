/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model

import play.api.libs.json._

enum StatusName(val status: String) {
  case Paper extends StatusName("PAPER")
  case EmailNotVerified extends StatusName("EMAIL_NOT_VERIFIED")
  case BouncedEmail extends StatusName("BOUNCED_EMAIL")
  case Alright extends StatusName("ALRIGHT")
  case NewCustomer extends StatusName("NEW_CUSTOMER")
  case NoEmail extends StatusName("NO_EMAIL")
  case ReOptIn extends StatusName("RE_OPT_IN")
  case ReOptInModified extends StatusName("RE_OPT_IN_MODIFIED")
}

object StatusName {
  implicit val format: Format[StatusName] = new Format[StatusName] {
    def reads(json: JsValue): JsResult[StatusName] = json match {
      case JsString("PAPER")              => JsSuccess(Paper)
      case JsString("EMAIL_NOT_VERIFIED") => JsSuccess(EmailNotVerified)
      case JsString("BOUNCED_EMAIL")      => JsSuccess(BouncedEmail)
      case JsString("ALRIGHT")            => JsSuccess(Alright)
      case JsString("NEW_CUSTOMER")       => JsSuccess(NewCustomer)
      case JsString("NO_EMAIL")           => JsSuccess(NoEmail)
      case JsString("RE_OPT_IN")          => JsSuccess(ReOptIn)
      case JsString("RE_OPT_IN_MODIFIED") => JsSuccess(ReOptInModified)
      case _                              => JsError("Invalid Status Name")
    }

    def writes(statusName: StatusName): JsValue = JsString(statusName.status)
  }
}

enum Category(val name: String) {
  case ActionRequired extends Category("ACTION_REQUIRED")
  case Info extends Category("INFO")
  // OptionAvailable
}

object Category {
  import StatusName._

  private val statusByCategory: Map[Category, List[StatusName]] =
    Map(
      ActionRequired -> List(NewCustomer, Paper, EmailNotVerified, BouncedEmail, NoEmail, ReOptIn, ReOptInModified),
      //      OptionAvailable -> List(WelshAvailable),
      Info -> List(Alright)
    )
  private val categoryByStatus: Map[StatusName, Category] =
    for {
      (category, statuses) <- statusByCategory
      status               <- statuses
    } yield status -> category

  def apply(statusName: StatusName): Category = categoryByStatus(statusName)

  implicit val format: Format[Category] = new Format[Category] {
    def reads(json: JsValue): JsResult[Category] = json match {
      case JsString("ACTION_REQUIRED") => JsSuccess(ActionRequired)
      case JsString("INFO")            => JsSuccess(Info)
      case _                           => JsError("Invalid Category")
    }

    def writes(category: Category): JsValue = JsString(category.name)
  }
}

case class Url(link: String, text: String)

object Url {
  implicit val formats: OFormat[Url] = Json.format[Url]
}

case class PaperlessStatus(
  name: StatusName,
  category: Category,
  text1: String,
  text2: String,
  majorVersion: Option[Int] = None
)

object PaperlessStatus {
  implicit val formats: OFormat[PaperlessStatus] = Json.format[PaperlessStatus]
}

case class StatusWithUrl(
  status: PaperlessStatus,
  url: Url
)

object StatusWithUrl {
  implicit val formats: OFormat[StatusWithUrl] = Json.format[StatusWithUrl]
}
