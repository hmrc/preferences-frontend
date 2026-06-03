/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package connectors

import _root_.controllers.internal.OptInCohort
import config.ServicesCircuitBreaker
import model.{ HostContext, Language, PageType, ReturnLink, Survey }

import java.time.Instant
import play.api.http.Status._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.{ Configuration, Logging }
import uk.gov.hmrc.domain.{ Nino, SaUtr, TaxIdentifier }
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import _root_.model.SurveyType
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream4xxResponse
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.{ URI, URL }

case class Email(email: String)

object Email {
  implicit val readsEmail: Reads[Email] = Json.reads[Email]
}

sealed trait TermsType

case object GenericTerms extends TermsType

sealed trait PreferencesStatus

case object PreferencesExists extends PreferencesStatus

case object PreferencesCreated extends PreferencesStatus

case object PreferencesFailure extends PreferencesStatus

case class Version(major: Int, minor: Int)

object Version {
  implicit val versionFormat: OFormat[Version] = Json.format[Version]
}

case class OptInPage private (version: Version, cohort: OptInCohort, pageType: PageType)

object OptInPage {
  implicit val optInPageWrites: Writes[OptInPage] = Json.writes[OptInPage]
  def from(cohort: OptInCohort): OptInPage =
    OptInPage(Version(cohort.majorVersion, cohort.minorVersion), cohort, cohort.pageType)

  implicit val optInPageReads: Reads[OptInPage] = ((__ \ "version").read[Version] and
    Reads[OptInCohort](j =>
      (__ \ "cohort").asSingleJson(j) match {
        case JsDefined(value) =>
          value
            .validate[Int]
            .flatMap(v =>
              OptInCohort
                .fromId(v)
                .fold[JsResult[OptInCohort]](
                  JsError(Seq(JsPath -> Seq(JsonValidationError(s"invalid cohort number $value"))))
                )(c => JsSuccess(c))
            )
        case _: JsUndefined => JsError(Seq(JsPath -> Seq(JsonValidationError("missing cohort"))))
      }
    )
    and
    (__ \ "pageType").read[PageType])((ver, cohort, pageType) =>
    if (ver.major == cohort.majorVersion && ver.minor == cohort.minorVersion && cohort.pageType == pageType)
      OptInPage(ver, cohort, pageType)
    else
      throw new IllegalArgumentException("Invalid constructor arguments")
  )
  implicit val optInPageFormat: Format[OptInPage] = Format(optInPageReads, optInPageWrites)
}

case class TermsAccepted(accepted: Boolean, optInPage: Option[OptInPage] = None, surveyType: Option[SurveyType] = None)

object TermsAccepted {
  implicit val format: OFormat[TermsAccepted] = Json.format[TermsAccepted]
}

trait PreferenceStatus
case class PreferenceFound(
  accepted: Boolean,
  email: Option[EmailPreference],
  updatedAt: Option[Instant] = None,
  majorVersion: Option[Int] = None,
  paperless: Option[Boolean],
  surveys: Option[List[Survey]] = None
) extends PreferenceStatus
case class PreferenceNotFound(email: Option[EmailPreference]) extends PreferenceStatus
case class MultiplePreferenceFound() extends PreferenceStatus

sealed case class TermsAndConditionsUpdate(
  generic: Option[TermsAccepted],
  email: Option[String],
  returnUrl: Option[String] = None,
  returnText: Option[String] = None,
  language: Option[Language],
  journey: Option[String] = None
)

object TermsAndConditionsUpdate {
  def from(
    terms: (TermsType, TermsAccepted),
    email: Option[String],
    language: Some[Language],
    journey: Option[String] = None
  ): TermsAndConditionsUpdate =
    terms match {
      case (GenericTerms, accepted) =>
        new TermsAndConditionsUpdate(Some(accepted), email, None, None, language, journey) {}
    }
  def fromLanguage(language: Some[Language]): TermsAndConditionsUpdate =
    new TermsAndConditionsUpdate(None, None, None, None, language) {}

  implicit val writes: Writes[TermsAndConditionsUpdate] = (
    (JsPath \ "generic").write[Option[TermsAccepted]] and
      (JsPath \ "email").write[Option[String]] and
      (JsPath \ "returnUrl").write[Option[String]] and
      (JsPath \ "returnText").write[Option[String]] and
      (JsPath \ "language").write[Option[Language]] and
      (JsPath \ "journey").write[Option[String]]
  )(t => Tuple.fromProductTyped[TermsAndConditionsUpdate](t))
}
@Singleton
class EntityResolverConnector @Inject() (config: Configuration, http: HttpClientV2)
    extends ServicesConfig(config) with ServicesCircuitBreaker with Logging {

  override val externalServiceName = "entity-resolver"
  val serviceUrl = baseUrl("entity-resolver")

  protected[connectors] case class ActivationStatus(active: Boolean)

  protected[connectors] object ActivationStatus {
    implicit val format: OFormat[ActivationStatus] = Json.format[ActivationStatus]
  }

  def url(path: String): URL = new URI(s"$serviceUrl$path").toURL

  def getPreferencesStatusFinal(termsAndCond: String, request_url: String)(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Int, PreferenceStatus]] = {
    implicit val rds: HttpReads[Option[PreferenceResponse]] =
      HttpReads.Implicits.readOptionOfNotFound(using HttpReads.Implicits.readFromJson)
    withCircuitBreaker {
      val result = http
        .get(url(request_url))
        .execute[Option[PreferenceResponse]]
      result
        .map {
          case Some(preference) =>
            preference.termsAndConditions
              .get(termsAndCond)
              .fold[Either[Int, PreferenceStatus]](Right(PreferenceNotFound(preference.email))) { acceptance =>
                Right(
                  PreferenceFound(
                    acceptance.accepted,
                    preference.email,
                    acceptance.updatedAt,
                    acceptance.majorVersion,
                    paperless = acceptance.paperless,
                    surveys = preference.surveys
                  )
                )
              }
          case None => Right(PreferenceNotFound(None))
        }
        .recover {
          case Upstream4xxResponse(e) if e.statusCode == NOT_FOUND    => Left(NOT_FOUND)
          case Upstream4xxResponse(e) if e.statusCode == UNAUTHORIZED => Left(UNAUTHORIZED)
          case _: BadRequestException                                 => Left(BAD_REQUEST)
        }
    }
  }

  protected[connectors] def changeEmailAddress(newEmail: String, journey: Option[String] = None)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[HttpResponse] = {
    implicit val rds: HttpReads[Either[UpstreamErrorResponse, HttpResponse]] =
      HttpReads.Implicits.readEitherOf(using HttpReads.Implicits.readRaw)
    withCircuitBreaker(
      http
        .put(url(s"/preferences/pending-email"))
        .withBody(Json.toJson(UpdateEmail(newEmail, journey)))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Left(err)       => throw err
          case Right(response) => response
        }
    )
  }

  protected[connectors] def optIn(
    termsAndConditionsUpdate: TermsAndConditionsUpdate
  )(implicit hc: HeaderCarrier, hostContext: HostContext, ec: ExecutionContext): Future[PreferencesStatus] = {
    val endPointStr =
      if (hostContext.isItsa) "/preferences/regime/optin"
      else
        "/preferences/optin"
    implicit val rds: HttpReads[Either[UpstreamErrorResponse, HttpResponse]] =
      HttpReads.Implicits.readEitherOf(using HttpReads.Implicits.readRaw)
    withCircuitBreaker(
      http
        .post(url(endPointStr))
        .withBody(Json.toJson(termsAndConditionsUpdate))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Left(err)       => throw err
          case Right(response) => response
        }
        .map(_.status)
        .map {
          case OK      => PreferencesExists
          case CREATED => PreferencesCreated
          case status  => throw new Exception(s"Unhandled status in optIn(...): $status")
        }
    )
  }

  protected[connectors] def optOut(
    termsAndConditionsUpdate: TermsAndConditionsUpdate
  )(implicit hc: HeaderCarrier, hostContext: HostContext, ec: ExecutionContext): Future[PreferencesStatus] = {
    val endPointStr =
      if (hostContext.isItsa) "/preferences/regime/optout"
      else
        "/preferences/optout"
    implicit val rds: HttpReads[Either[UpstreamErrorResponse, HttpResponse]] =
      HttpReads.Implicits.readEitherOf(using HttpReads.Implicits.readRaw)
    withCircuitBreaker(
      http
        .post(url(endPointStr))
        .withBody(Json.toJson(termsAndConditionsUpdate))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Left(err)       => throw err
          case Right(response) => response
        }
        .map(_.status)
        .map {
          case OK      => PreferencesExists
          case CREATED => PreferencesCreated
          case status  => throw new Exception(s"Unhandled status in optOut(...): $status")
        }
    )
  }

  private[connectors] def responseToEmailVerificationLinkStatus(
    response: Future[HttpResponse]
  )(implicit ec: ExecutionContext): Future[EmailVerificationLinkResponse] =
    response
      .map { response =>
        response.status match {
          case CREATED =>
            val link = ReturnLink.fromString(response.body)
            ValidatedWithReturn(link.linkText, link.linkUrl)
          case _ => Validated
        }
      }
      .recover {
        case Upstream4xxResponse(e) if e.statusCode == GONE     => ValidationExpired
        case Upstream4xxResponse(e) if e.statusCode == CONFLICT => WrongToken
        case Upstream4xxResponse(e) if e.statusCode == PRECONDITION_FAILED =>
          val body = e.message.substring(e.message.indexOf("Response body: '") + 16).stripSuffix("'")
          val link = ReturnLink.fromString(body)
          ValidationErrorWithReturn(link.linkText, link.linkUrl)
        case Upstream4xxResponse(_) | _: NotFoundException | _: BadRequestException => ValidationError
      }
}
