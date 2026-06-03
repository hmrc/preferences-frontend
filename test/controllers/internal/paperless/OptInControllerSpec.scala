/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import connectors.{ PreferencesConnector, PreferencesCreated }
import controllers.{ AuthRetrievalsSetup, REGIME_ITSA }
import controllers.internal.OptInEmailForm
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.jsoup.Jsoup
import org.mockito.Mockito.when
import play.api.Application
import play.api.inject.bind
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailaddress.EmailAddressValidation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.SpecBase
import org.mockito.ArgumentMatchers.*
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc.{ AnyContentAsEmpty, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval

import scala.concurrent.{ ExecutionContext, Future }

class OptInControllerSpec extends SpecBase with AuthRetrievalsSetup {

  "displayOptInEmail" should {
    "return OK for successful request processing" in new Setup {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(GET, routes.OptInController.displayOptInEmail(Some(true), hostContext()).url)

      val result: Option[Future[Result]] = route(app, request)

      result.map { resultValue =>
        status(resultValue) mustBe OK
      }
    }

    "return OK and correct title for successful request processing" when {

      "regime is itsa" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          fakeRequest(
            GET,
            routes.OptInController.displayOptInEmail(Some(true), hostContext().copy(regime = Some(REGIME_ITSA))).url
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) mustBe OK

          val pageDocument = Jsoup.parse(contentAsString(resultValue))

          pageDocument.title() mustBe messages("sa_printing_preference.itsa.sps_opt_in_email")
        }
      }

      "regime is not itsa" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          fakeRequest(
            GET,
            routes.OptInController.displayOptInEmail(Some(true), hostContext().copy(regime = Some("nino"))).url
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) mustBe OK

          val pageDocument = Jsoup.parse(contentAsString(resultValue))

          pageDocument.title() mustBe messages("sa_printing_preference.sps_opt_in_email")
        }
      }
    }
  }

  "submitOptIn" should {
    "contain correct title" when {
      "form has an error and regime is itsa" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        val body: JsValue = Json.toJson(Map("sps-opt-in" -> ""))

        val request: FakeRequest[JsValue] =
          fakeRequestWithBody[JsValue](
            POST,
            routes.OptInController.submitOptIn(hostContext().copy(regime = Some(REGIME_ITSA))).url,
            body
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) must be(BAD_REQUEST)

          val pageDocument = Jsoup.parse(contentAsString(resultValue))

          pageDocument.title() mustBe messages("i_page56.fg_page.itsa.title.error")
        }
      }

      "form has an error and regime is not itsa" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        val body: JsValue = Json.toJson(Map("sps-opt-in" -> ""))

        val request: FakeRequest[JsValue] =
          fakeRequestWithBody[JsValue](
            POST,
            routes.OptInController.submitOptIn(hostContext().copy(regime = Some("nino"))).url,
            body
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) must be(BAD_REQUEST)

          val pageDocument = Jsoup.parse(contentAsString(resultValue))

          pageDocument.title() mustBe messages("i_page56.fg_page.title.error")
        }
      }
    }
  }

  "submitOptInEmail" should {
    "redirect for successful request processing" when {
      "changeEmail option is true" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        when(mockPreferencesConnector.changeEmailAddress(any, any)(any, any))
          .thenReturn(Future.successful(HttpResponse()))

        val body: JsValue = Json.toJson(Map("sps-opt-in-email" -> "test@gmail.com"))

        val request: FakeRequest[JsValue] =
          fakeRequestWithBody[JsValue](
            POST,
            routes.OptInController.submitOptInEmail(Some(true), hostContext()).url,
            body
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) mustBe SEE_OTHER
        }
      }

      "changeEmail option is false" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        when(mockPreferencesConnector.optIn(any)(any, any, any)).thenReturn(Future.successful(PreferencesCreated))

        val body: JsValue = Json.toJson(Map("sps-opt-in-email" -> "test@gmail.com"))

        val request: FakeRequest[JsValue] =
          fakeRequestWithBody[JsValue](
            POST,
            routes.OptInController.submitOptInEmail(Some(false), hostContext()).url,
            body
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) mustBe SEE_OTHER
        }
      }
    }

    "contain correct title" when {

      "form has an error and regime is itsa" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        val body: JsValue = Json.toJson(Map("sps-opt-in-email" -> ""))

        val request: FakeRequest[JsValue] =
          fakeRequestWithBody[JsValue](
            POST,
            routes.OptInController.submitOptInEmail(Some(true), hostContext().copy(regime = Some(REGIME_ITSA))).url,
            body
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) must be(BAD_REQUEST)

          val pageDocument = Jsoup.parse(contentAsString(resultValue))

          pageDocument.title() mustBe messages("sa_printing_preference.itsa.sps_opt_in_email_error")
        }
      }

      "form has an error and regime is not itsa" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        val body: JsValue = Json.toJson(Map("sps-opt-in-email" -> ""))

        val request: FakeRequest[JsValue] =
          fakeRequestWithBody[JsValue](
            POST,
            routes.OptInController.submitOptInEmail(Some(true), hostContext().copy(regime = Some("nino"))).url,
            body
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) must be(BAD_REQUEST)

          val pageDocument = Jsoup.parse(contentAsString(resultValue))

          pageDocument.title() mustBe messages("sa_printing_preference.sps_opt_in_email_error")
        }
      }
    }
  }

  "displayOptInConfirmation" should {
    "return OK and correct title for successful request processing" when {

      "regime is itsa" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          fakeRequest(
            GET,
            routes.OptInController.displayOptInConfirmation(hostContext().copy(regime = Some(REGIME_ITSA))).url
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) mustBe OK

          val pageDocument = Jsoup.parse(contentAsString(resultValue))

          pageDocument.title() mustBe messages("sa_printing_preference.sps_email_confirm.itsa.title")
        }
      }

      "regime is not itsa" in new Setup {
        when(
          mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(retrievalResult)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          fakeRequest(
            GET,
            routes.OptInController.displayOptInConfirmation(hostContext().copy(regime = Some("nino"))).url
          )

        val result: Option[Future[Result]] = route(app, request)

        result.map { resultValue =>
          status(resultValue) mustBe OK

          val pageDocument = Jsoup.parse(contentAsString(resultValue))

          pageDocument.title() mustBe messages("sa_printing_preference.sps_email_confirm")
        }
      }
    }
  }

  trait Setup {
    val mockAuditConnector: AuditConnector = mock[AuditConnector]
    val mockPreferencesConnector: PreferencesConnector = mock[PreferencesConnector]
    val emailAddressValidation: EmailAddressValidation = mock[EmailAddressValidation]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val app: Application = applicationBuilder
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector),
        bind[EmailAddressValidation].toInstance(emailAddressValidation)
      )
      .configure("metrics.enabled" -> false)
      .build()
  }
}
