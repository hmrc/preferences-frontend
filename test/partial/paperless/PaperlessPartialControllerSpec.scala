/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package partial.paperless

import connectors.{ EmailPreference, PreferenceResponse, PreferencesConnector, TermsAndConditionsAcceptance }
import controllers.AuthRetrievalsSetup
import model.Language.English
import play.api.Application
import uk.gov.hmrc.auth.core.AuthConnector
import play.api.inject.bind
import uk.gov.hmrc.emailaddress.EmailAddressValidation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.SpecBase
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.*
import play.api.mvc.{ AnyContentAsEmpty, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import utils.TestData.*

import scala.concurrent.{ ExecutionContext, Future }

class PaperlessPartialControllerSpec extends SpecBase with AuthRetrievalsSetup {

  "displayManagePaperlessPartial" should {
    "return OK for successful request processing" in new Setup {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)

      when(mockPreferencesConnector.getPreferences()(any, any)).thenReturn(Future.successful(Some(preferenceResponse)))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(GET, routes.PaperlessPartialController.displayManagePaperlessPartial(hostContext()).url)

      val result: Option[Future[Result]] = route(app, request)

      result.map { resultValue =>
        status(resultValue) mustBe OK
      }
    }
  }

  "displayPaperlessWarningsPartial" should {
    "return OK for successful request processing" in new Setup {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)

      when(mockPreferencesConnector.getPreferences()(any, any)).thenReturn(Future.successful(Some(preferenceResponse)))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(GET, routes.PaperlessPartialController.displayPaperlessWarningsPartial(hostContext()).url)

      val result: Option[Future[Result]] = route(app, request)

      result.map { resultValue =>
        status(resultValue) mustBe OK
      }
    }

    "return NOT_FOUND for successful request processing" in new Setup {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)

      when(mockPreferencesConnector.getPreferences()(any, any)).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(GET, routes.PaperlessPartialController.displayPaperlessWarningsPartial(hostContext()).url)

      val result: Option[Future[Result]] = route(app, request)

      result.map { resultValue =>
        status(resultValue) mustBe NOT_FOUND
      }
    }
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockPreferencesConnector: PreferencesConnector = mock[PreferencesConnector]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val app: Application = applicationBuilder
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector)
      )
      .configure("metrics.enabled" -> false)
      .build()

    lazy val termsAndConditionsAcceptance: TermsAndConditionsAcceptance =
      TermsAndConditionsAcceptance(
        accepted = true,
        updatedAt = Some(TEST_TIME_INSTANT),
        majorVersion = Some(2),
        paperless = Some(true)
      )

    lazy val emailPreference: EmailPreference = EmailPreference(
      email = TEST_EMAIL_VALUE,
      isVerified = true,
      hasBounces = false,
      mailboxFull = false,
      linkSent = Some(TEST_LOCAL_DATE),
      language = Some(English),
      pendingEmail = Some(TEST_EMAIL_VALUE)
    )

    lazy val preferenceResponse: PreferenceResponse =
      PreferenceResponse(
        termsAndConditions = Map(TEST_KEY -> termsAndConditionsAcceptance),
        email = Some(emailPreference)
      )
  }
}
