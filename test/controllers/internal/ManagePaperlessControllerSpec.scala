/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.PreferenceResponse.*
import connectors.*
import controllers.AuthRetrievalsSetup
import controllers.auth.AuthenticatedRequest
import helpers.TestFixtures
import model.{ Encrypted, HostContext }
import model.Language.Welsh
import org.jsoup.Jsoup
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, Retrieval, ~ }
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.{ EmailAddress, EmailAddressValidation }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.{ ZoneOffset, ZonedDateTime }
import scala.concurrent.{ ExecutionContext, Future }

class ManagePaperlessControllerSpec
    extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach with AuthRetrievalsSetup {

  import org.mockito.ArgumentMatchers.{ any, eq => is }

  val validUtr = SaUtr("1234567890")

  val request = AuthenticatedRequest(FakeRequest(), None, None, None, None)

  val hc = new HeaderCarrier()

  val mockPreferencesConnector = mock[PreferencesConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]
  val emailAddressValidation = mock[EmailAddressValidation]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "preferences-frontend.host" -> "",
        "metrics.enabled"           -> false
      )
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector),
        bind[EmailAddressValidation].toInstance(emailAddressValidation)
      )
      .build()
  val controller = app.injector.instanceOf[ManagePaperlessController]

  when(
    mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )
  ).thenReturn(retrievalResult)

  override def beforeEach(): Unit = {
    reset(mockPreferencesConnector)
    reset(mockAuditConnector)
    reset(emailAddressValidation)
  }

  "clicking on Change email address link in the account details page" should {
    "display update email address form when accessed from Account Details" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val result = controller.displayChangeEmailAddress(None)(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text mustBe "test@test.com."
      page.getElementById("email.main") mustNot be(null)
      page.getElementById("email.main").attr("value") mustBe ""
      page.getElementById("email.confirm") mustNot be(null)
      page.getElementById("email.confirm").attr("value") mustBe ""
    }

    "display update email address form with the email input field pre-populated when coming back from the warning page" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val existingEmailAddress = "existing@email.com"
      val result = controller.displayChangeEmailAddress(Some(Encrypted(EmailAddress(existingEmailAddress))))(
        request,
        TestFixtures.sampleHostContext,
        hc
      )

      status(result) mustBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text mustBe "test@test.com."
      page.getElementById("email.main") mustNot be(null)
      page.getElementById("email.main").attr("value") mustBe existingEmailAddress
      page.getElementById("email.confirm") mustNot be(null)
      page.getElementById("email.confirm").attr("value") mustBe existingEmailAddress
    }

    "return bad request if the SA user has opted into paper" in {

      val saPreferences = SaPreference(false, None).toNewPreference()
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val result = controller.displayChangeEmailAddress(None)(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 400
    }
  }

  "Clicking Resend validation email link on account details page" should {

    "call preferences as if opting-in and send the email as a part of the process" in {

      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Pending))).toNewPreference()

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))
      when(
        mockPreferencesConnector
          .changeEmailAddress(is("test@test.com"), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(HttpResponse(OK, "")))

      val page = controller.resendVerificationEmail(request, TestFixtures.sampleHostContext, hc)

      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("verification-mail-message") must not be null
      document.getElementById("return-to-dashboard-button").attr("href") must be(
        "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      )

      verify(mockPreferencesConnector).changeEmailAddress(is("test@test.com"), any[Option[String]])(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    }
  }

  "Viewing the email address change thank you page" should {

    "display the confirmation page with the current email address obscured" in {
      val emailAddress = "someone@email.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference(emailAddress, SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val page = controller.displayChangeEmailAddressConfirmed(request, TestFixtures.sampleHostContext, hc)

      status(page) mustBe 200

      val doc = Jsoup.parse(contentAsString(page))
      doc.getElementById("updated-email-address").text must be("someone@email.com")
    }
  }

  "A POST to update email address with no emailVerifiedFlag" should {

    "validate the email address, update the address for SA user and redirect to confirmation page" in {
      val emailAddress = "someone@email.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))
      when(
        mockPreferencesConnector
          .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

      val page =
        controller.submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress)),
            None,
            None,
            None,
            None
          ),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(TestFixtures.sampleHostContext).toString()
      )

      verify(mockPreferencesConnector)
        .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      verify(emailAddressValidation).isValid(is(emailAddress))
      verify(mockPreferencesConnector).getPreferences()(any[HeaderCarrier], any[ExecutionContext])
      verifyNoMoreInteractions(emailAddressValidation)
    }

    "show error if the 2 email address fields do not match" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller.submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody("email.main" -> "a@abc.com", "email.confirm" -> "b@abc.com"),
            None,
            None,
            None,
            None
          ),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("email.confirm-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Check your email addresses - they don't match."
    }

    "show error if the email address is not syntactically valid" in {
      val emailAddress = "invalid-email"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))
      val page =
        controller.submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress)),
            None,
            None,
            None,
            None
          ),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("email.main-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
    }

    "show error if the email field is empty" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller.submitChangeEmailAddress(
          AuthenticatedRequest(FakeRequest().withFormUrlEncodedBody(("email.main", "")), None, None, None, None),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("email.main-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
    }

    "show a warning page if the email has a valid structure but does not pass validation by the email micro service" in {

      val emailAddress = "someone@abc.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@abc.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(false)
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller.submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress)),
            None,
            None,
            None,
            None
          ),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verify(emailAddressValidation).isValid(is(emailAddress))
    }

  }

  "A POST to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in {
      val emailAddress = "someone@email.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))
      when(
        mockPreferencesConnector
          .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(HttpResponse(OK, "")))

      val page =
        controller.submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(
              ("email.main", emailAddress),
              ("email.confirm", emailAddress),
              ("emailVerified", "true")
            ),
            None,
            None,
            None,
            None
          ),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(TestFixtures.sampleHostContext).toString
      )

      verify(mockPreferencesConnector)
        .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      verify(mockPreferencesConnector).getPreferences()(any[HeaderCarrier], any[ExecutionContext])
      verifyNoMoreInteractions(emailAddressValidation)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in {

      val emailAddress = "someone@gmail.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(false)
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller.submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(
              ("email.main", emailAddress),
              ("email.confirm", emailAddress),
              ("emailVerified", "false")
            ),
            None,
            None,
            None,
            None
          ),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verify(mockPreferencesConnector).getPreferences()(any[HeaderCarrier], any[ExecutionContext])
      verify(emailAddressValidation).isValid(is(emailAddress))
    }

    "if the verified flag is any value other than true, treat it as false" in {

      val emailAddress = "someone@abc.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(false)
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller.submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(
              ("email.main", emailAddress),
              ("email.confirm", emailAddress),
              ("emailVerified", "hjgjhghjghjgj")
            ),
            None,
            None,
            None,
            None
          ),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verify(mockPreferencesConnector).getPreferences()(any[HeaderCarrier], any[ExecutionContext])
      verify(emailAddressValidation).isValid(is(emailAddress))
    }
  }

  "clicking on opt-out of email reminders link in the account details page" should {

    "display the <are you sure> page" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val result = controller.displayStopPaperless(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("confirm-opt-out").text mustBe "Get tax letters by post"
      page.getElementById("cancel-link").text mustBe "Keep online tax letters"
      page.text() must not include "test@test.com"
    }

    "return bad request if the user has not opted into digital" in {
      val saPreferences = SaPreference(false, None).toNewPreference()
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))

      val result = controller.displayStopPaperless(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 400
    }
  }

  "A POST to confirm opt out of email reminders" should {

    "return a redirect to opt out survey page" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(saPreferences)))
      when(
        mockPreferencesConnector
          .optOut(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesExists))

      val result = controller.submitStopPaperless(lang = Some(Welsh))(TestFixtures.sampleHostContext, hc)

      status(result) mustBe 303
      header("Location", result).get must include(
        routes.SurveyController.displayOptoutSurvey(TestFixtures.sampleHostContext).url
      )

      verify(mockPreferencesConnector)
        .optOut(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )
    }
  }

}
