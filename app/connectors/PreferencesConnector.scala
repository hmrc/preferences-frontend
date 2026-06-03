/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import config.ServicesCircuitBreaker
import model.VerifyStatus.{ AlreadyVerified, AlreadyVerifiedLinks }
import model.{ EmailVerification, HostContext, ReturnLink }
import play.api.http.HeaderNames
import play.api.http.Status.{ BAD_REQUEST, CONFLICT, CREATED, GONE, NOT_FOUND, OK, PRECONDITION_FAILED, UNAUTHORIZED }
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.{ Configuration, Logging }
import uk.gov.hmrc.domain.{ Nino, SaUtr, TaxIdentifier }
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream4xxResponse
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, HttpReads, HttpResponse, NotFoundException, UpstreamErrorResponse }
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.{ URI, URL }
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

@Singleton
class PreferencesConnector @Inject() (
  config: Configuration,
  http: HttpClientV2
) extends ServicesConfig(config) with ServicesCircuitBreaker with Logging {

  override val externalServiceName = "preferences"

  val serviceUrl: String = baseUrl("preferences")

  def url(path: String): URL = new URI(s"$serviceUrl$path").toURL

  def getPreferences()(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[PreferenceResponse]] = {
    implicit val rds: HttpReads[Option[PreferenceResponse]] =
      HttpReads.Implicits.readOptionOfNotFound(using HttpReads.Implicits.readFromJson)

    http
      .get(url(s"/preferences"))
      .execute[Option[PreferenceResponse]]
      .recover {
        case response: UpstreamErrorResponse if response.statusCode == GONE => None
        case _: NotFoundException                                           => None
      }
  }

  def getPreferencesStatus(
    termsAndCond: String = "generic"
  )(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[Int, PreferenceStatus]] =
    getPreferencesStatusFinal(termsAndCond, url(s"/preferences"))

  private def getPreferencesStatusFinal(termsAndCond: String, request_url: URL)(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Int, PreferenceStatus]] = {
    implicit val rds: HttpReads[Option[PreferenceResponse]] =
      HttpReads.Implicits.readOptionOfNotFound(using HttpReads.Implicits.readFromJson)
    withCircuitBreaker {
      http.get(request_url).execute[Option[PreferenceResponse]].map {
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
    }.recover {
      case Upstream4xxResponse(e) if e.statusCode == NOT_FOUND    => Left(NOT_FOUND)
      case Upstream4xxResponse(e) if e.statusCode == UNAUTHORIZED => Left(UNAUTHORIZED)
      case _: BadRequestException                                 => Left(BAD_REQUEST)
    }
  }

  def getEmailAddress(
    taxId: TaxIdentifier
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    def basedOnTaxIdType =
      taxId match {
        case SaUtr(utr) => s"/preferences/verify/email-address?regime=sa&taxId=$utr"
        case Nino(nino) => s"/preferences/verify/email-address?regime=paye&taxId=$nino"
        case _          => throw new IllegalArgumentException(s"Unsupported taxId: $taxId")
      }

    implicit val rds: HttpReads[Option[Email]] =
      HttpReads.Implicits.readOptionOfNotFound(using HttpReads.Implicits.readFromJson)
    withCircuitBreaker(http.get(url(basedOnTaxIdType)).execute[Option[Email]]).map(_.map(_.email))
  }

  def getPreferencesUnresolved()(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[PreferenceStatus, PreferenceResponse]] = {
    implicit val rds: HttpReads[Option[PreferenceResponse]] =
      HttpReads.Implicits.readOptionOfNotFound(using HttpReads.Implicits.readFromJson)
    withCircuitBreaker {
      http
        .get(url("/preferences?resolve=false"))
        .execute[Option[PreferenceResponse]]
        .map {
          case Some(preference) => Right(preference)
          case _                => Left(PreferenceNotFound(None))
        }
    }.recover {
      case Upstream4xxResponse(e) if e.statusCode == NOT_FOUND => Left(PreferenceNotFound(None))
      case Upstream4xxResponse(e) if e.statusCode == CONFLICT  => Left(MultiplePreferenceFound())
    }
  }

  private def optInUrl(implicit hostContext: HostContext): String =
    if (hostContext.isItsa) "/preferences/regime/optin" else "/preferences/optin"

  def optIn(
    termsAndConditionsUpdate: TermsAndConditionsUpdate
  )(implicit hc: HeaderCarrier, hostContext: HostContext, ec: ExecutionContext): Future[PreferencesStatus] =
    withCircuitBreaker(
      http
        .post(url(optInUrl))
        .withBody(Json.toJson(termsAndConditionsUpdate))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Left(err)       => throw err
          case Right(response) => response
        }
    )
      .map(_.status)
      .map {
        case OK      => PreferencesExists
        case CREATED => PreferencesCreated
        case status  => throw new Exception(s"Unhandled status in optIn(...): $status")
      }

  private def optOutUrl(implicit hostContext: HostContext): String =
    if (hostContext.isItsa) "/preferences/regime/optout" else "/preferences/optout"

  def optOut(
    termsAndConditionsUpdate: TermsAndConditionsUpdate
  )(implicit hc: HeaderCarrier, hostContext: HostContext, ec: ExecutionContext): Future[PreferencesStatus] =
    withCircuitBreaker(
      http
        .post(url(optOutUrl))
        .withBody(Json.toJson(termsAndConditionsUpdate))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Left(err)       => throw err
          case Right(response) => response
        }
    )
      .map(_.status)
      .map {
        case OK      => PreferencesExists
        case CREATED => PreferencesCreated
        case status  => throw new Exception(s"Unhandled status in optOut(...): $status")
      }

  def updateEmailValidationStatusUnsecured(
    token: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailVerificationLinkResponse] = {
    implicit val rds: HttpReads[Either[UpstreamErrorResponse, HttpResponse]] =
      HttpReads.Implicits.readEitherOf(using HttpReads.Implicits.readRaw)

    responseToEmailVerificationLinkStatus(
      withCircuitBreaker(
        http
          .put(url("/preferences/email"))
          .withBody(Json.toJson(ValidateEmail(token)))
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
          .map {
            case Left(err) =>
              logger.error(s"$err")
              throw err
            case Right(response) =>
              response
          }
      )
    )
  }

  private[connectors] def responseToEmailVerificationLinkStatus(
    response: Future[HttpResponse]
  )(implicit ec: ExecutionContext): Future[EmailVerificationLinkResponse] =
    response
      .map(processResponse)
      .recover {
        case Upstream4xxResponse(e) if e.statusCode == GONE     => ValidationExpired
        case Upstream4xxResponse(e) if e.statusCode == CONFLICT => WrongToken
        case Upstream4xxResponse(e) if e.statusCode == PRECONDITION_FAILED =>
          val body = e.message.substring(e.message.indexOf("Response body: '") + 16).stripSuffix("'")
          val link = ReturnLink.fromString(body)
          ValidationErrorWithReturn(link.linkText, link.linkUrl)
        case Upstream4xxResponse(_) | _: NotFoundException | _: BadRequestException => ValidationError
      }

  private def processResponse(response: HttpResponse): EmailVerificationLinkResponse =
    response match {
      case _ if response.status == CREATED =>
        val link = ReturnLink.fromString(response.body)
        ValidatedWithReturn(link.linkText, link.linkUrl)
      case _ if response.status == OK =>
        val emailVerification = response.json.as[EmailVerification]
        emailVerification.verifyStatus match {
          case AlreadyVerified =>
            ValidationError
          case AlreadyVerifiedLinks =>
            ValidationErrorWithReturn(
              emailVerification.returnLinkText.getOrElse("missing-linktext"),
              emailVerification.returnUrl.getOrElse("missing-linkurl")
            )
          case _ =>
            Validated
        }

      case _ => Validated
    }

  def changeEmailLanguage(
    termsAndConditionsUpdate: TermsAndConditionsUpdate
  )(implicit hc: HeaderCarrier, hostContext: HostContext, ec: ExecutionContext): Future[PreferencesStatus] = {
    val endPointStr =
      if (hostContext.isItsa) "/preferences/regime/email-language"
      else
        "/preferences/email-language"
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
    ).map(_.status)
      .map {
        case OK      => PreferencesExists
        case CREATED => PreferencesCreated
        case status  => throw new Exception(s"Unhandled status in changeEmailLanguage(...): $status")
      }
  }

  def changeEmailAddress(
    newEmail: String,
    journey: Option[String] = None
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    implicit val rds: HttpReads[Either[UpstreamErrorResponse, HttpResponse]] =
      HttpReads.Implicits.readEitherOf(using HttpReads.Implicits.readRaw)
    withCircuitBreaker(
      http
        .put(url("/preferences/pending-email"))
        .withBody(Json.toJson(UpdateEmail(newEmail, journey)))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Left(err)       => throw err
          case Right(response) => response
        }
    )
  }
}
