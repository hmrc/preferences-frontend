/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package connectors

import model.{ StatusName, Survey }
import java.time.{ Instant, LocalDate }
import play.api.libs.json._

sealed trait EmailVerificationLinkResponse
case object Validated extends EmailVerificationLinkResponse
case class ValidatedWithReturn(returnLinkText: String, returnUrl: String) extends EmailVerificationLinkResponse
case object ValidationExpired extends EmailVerificationLinkResponse
case object WrongToken extends EmailVerificationLinkResponse
case object ValidationError extends EmailVerificationLinkResponse
case class ValidationErrorWithReturn(returnLinkText: String, returnUrl: String) extends EmailVerificationLinkResponse
import model.Language

object SaPreferenceSimplified {
  implicit val formats: OFormat[SaPreferenceSimplified] = Json.format[SaPreferenceSimplified]
}

case class SaPreferenceSimplified(digital: Boolean, email: Option[String] = None)

case class UpdateEmail(email: String, journey: Option[String] = None)

object UpdateEmail {
  implicit def formats: Format[UpdateEmail] = Json.format[UpdateEmail]
}

object SaEmailPreference {

  sealed trait Status

  object Status {

    case object Pending extends Status

    case object Bounced extends Status

    case object Verified extends Status

    val reads: Reads[Status] = new Reads[Status] {
      override def reads(json: JsValue): JsResult[Status] =
        json match {
          case JsString("pending")  => JsSuccess(Pending)
          case JsString("bounced")  => JsSuccess(Bounced)
          case JsString("verified") => JsSuccess(Verified)
          case _                    => JsError()
        }
    }

    val writes: OWrites[Status] = new OWrites[Status] {
      override def writes(o: Status): JsObject = JsString(o.toString.toLowerCase).as[JsObject]
    }
    implicit val formats: OFormat[Status] = OFormat(reads, writes)
  }
  implicit val formats: OFormat[SaEmailPreference] = Json.format[SaEmailPreference]

}

case class SaEmailPreference(
  email: String,
  status: SaEmailPreference.Status,
  mailboxFull: Boolean = false,
  message: Option[String] = None,
  linkSent: Option[LocalDate] = None
) {

  implicit class emailPreferenceOps(saEmail: SaEmailPreference) {
    def toEmailPreference: EmailPreference =
      EmailPreference(
        saEmail.email,
        saEmail.status == SaEmailPreference.Status.Verified,
        saEmail.status == SaEmailPreference.Status.Bounced,
        saEmail.mailboxFull,
        saEmail.linkSent
      )

  }

}

case class EmailPreference(
  email: String,
  isVerified: Boolean,
  hasBounces: Boolean,
  mailboxFull: Boolean,
  linkSent: Option[LocalDate],
  language: Option[Language] = None,
  pendingEmail: Option[String] = None
)

object EmailPreference {
  implicit val formats: OFormat[EmailPreference] = Json.format[EmailPreference]
}

case class TermsAndConditionsAcceptance(
  accepted: Boolean,
  updatedAt: Option[Instant] = None,
  majorVersion: Option[Int] = None,
  paperless: Option[Boolean] = None
)
object TermsAndConditionsAcceptance {

  implicit val dateFormatDefault: Format[Instant] = new Format[Instant] {
    override def reads(json: JsValue): JsResult[Instant] = Reads.DefaultInstantReads.reads(json)
    override def writes(o: Instant): JsValue = Writes.DefaultInstantWrites.writes(o)
  }
  implicit val formats: OFormat[TermsAndConditionsAcceptance] = Json.format[TermsAndConditionsAcceptance]
}

case class PaperlessStatusResponse(
  name: StatusNameResponse
)

object PaperlessStatusResponse {
  implicit val formats: OFormat[PaperlessStatusResponse] = Json.format[PaperlessStatusResponse]
}

case class PreferenceResponse(
  termsAndConditions: Map[String, TermsAndConditionsAcceptance],
  email: Option[EmailPreference],
  surveys: Option[List[Survey]] = None,
  entityId: Option[String] = None,
  status: Option[PaperlessStatusResponse] = None
) {
  val genericTermsAccepted: Boolean = termsAndConditions.get("generic").fold(false)(_.accepted)

  val isOnNewTerms = termsAndConditions.get("generic").fold(false)(_.majorVersion.exists(_ >= 1))
}

object PreferenceResponse {

  val defaultRead = Json.reads[PreferenceResponse]

  val reads = new Reads[PreferenceResponse] {
    override def reads(json: JsValue): JsResult[PreferenceResponse] =
      json.validate(defaultRead) match {
        case jSucc @ JsSuccess(_, _) => jSucc
        case _                       => SaPreference.formats.reads(json).map(_.toNewPreference())
      }
  }

  implicit val formats: OFormat[PreferenceResponse] = OFormat(reads, Json.writes[PreferenceResponse])

  implicit class saPreferenceOps(saPreference: SaPreference) {
    def toNewPreference(): PreferenceResponse = {
      def toNewEmail: SaEmailPreference => EmailPreference = { saEmail =>
        EmailPreference(
          saEmail.email,
          saEmail.status == SaEmailPreference.Status.Verified,
          saEmail.status == SaEmailPreference.Status.Bounced,
          saEmail.mailboxFull,
          saEmail.linkSent
        )
      }

      PreferenceResponse(
        termsAndConditions = Map("generic" -> TermsAndConditionsAcceptance(saPreference.digital)),
        email = saPreference.email.map(toNewEmail),
        surveys = saPreference.surveys
      )
    }
  }

  implicit class preferenceResponseOps(pref: PreferenceResponse) {
    def lang(): Option[Language] = pref.email.flatMap(_.language)
  }
}

enum StatusNameResponse(val status: String) {
  case Paper extends StatusNameResponse("PAPER")
  case EmailNotVerified extends StatusNameResponse("EMAIL_NOT_VERIFIED")
  case BouncedEmail extends StatusNameResponse("BOUNCED_EMAIL")
  case Alright extends StatusNameResponse("ALRIGHT")
  case NewCustomer extends StatusNameResponse("NEW_CUSTOMER")
  case NoEmail extends StatusNameResponse("NO_EMAIL")
  case OldVersion extends StatusNameResponse("OLD_VERSION")
  case ReOptInModified extends StatusNameResponse("RE_OPT_IN_MODIFIED")
}

object StatusNameResponse {
  implicit val formats: Format[StatusNameResponse] = new Format[StatusNameResponse] {
    def reads(json: JsValue): JsResult[StatusNameResponse] = json match {
      case JsString("PAPER")              => JsSuccess(Paper)
      case JsString("EMAIL_NOT_VERIFIED") => JsSuccess(EmailNotVerified)
      case JsString("BOUNCED_EMAIL")      => JsSuccess(BouncedEmail)
      case JsString("ALRIGHT")            => JsSuccess(Alright)
      case JsString("NEW_CUSTOMER")       => JsSuccess(NewCustomer)
      case JsString("NO_EMAIL")           => JsSuccess(NoEmail)
      case JsString("OLD_VERSION")        => JsSuccess(OldVersion)
      case JsString("RE_OPT_IN_MODIFIED") => JsSuccess(ReOptInModified)
      case _                              => JsError("Invalid Status Name Response")
    }

    def writes(statusNameResponse: StatusNameResponse): JsValue = JsString(statusNameResponse.status)
  }

  def toStatusName(statusNameResponse: StatusNameResponse): StatusName =
    statusNameResponse match {
      case Paper            => StatusName.Paper
      case EmailNotVerified => StatusName.EmailNotVerified
      case BouncedEmail     => StatusName.BouncedEmail
      case Alright          => StatusName.Alright
      case NewCustomer      => StatusName.NewCustomer
      case NoEmail          => StatusName.NoEmail
      case OldVersion       => StatusName.ReOptIn
      case ReOptInModified  => StatusName.ReOptInModified
    }
}

case class SaPreference(digital: Boolean, email: Option[SaEmailPreference] = None, surveys: Option[List[Survey]] = None)

object SaPreference {
  implicit val formats: OFormat[SaPreference] = Json.format[SaPreference]
}

object ValidateEmail {
  implicit val formats: OFormat[ValidateEmail] = Json.format[ValidateEmail]
}

case class ValidateEmail(token: String)
