/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import _root_.connectors.*
import controllers.internal.{ CohortCurrent, paperless }
import helpers.TestFixtures
import model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{ any, eq as is }
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{ Messages, MessagesApi }
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import service.PreCheckService
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailaddress.{ EmailAddress, EmailAddressValidation }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ EventTypes, MergedDataEvent }
import utils.TestData.EMPTY_STRING

import scala.concurrent.{ ExecutionContext, Future }

class ChoosePaperlessControllerSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite
    with ChoosePaperlessControllerSetup with ScalaFutures {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockPreferencesConnector: PreferencesConnector = mock[PreferencesConnector]
  val emailAddressValidation: EmailAddressValidation = mock[EmailAddressValidation]
  val mockPreCheckService: PreCheckService = mock[PreCheckService]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  when(
    mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )
  ).thenReturn(retrievalResult)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector),
        bind[EmailAddressValidation].toInstance(emailAddressValidation),
        bind[PreCheckService].toInstance(mockPreCheckService)
      )
      .configure(
        "metrics.enabled" -> false
      )
      .build()

  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuthConnector)
    reset(mockPreferencesConnector)
    reset(emailAddressValidation)
    reset(mockPreCheckService)
    when(
      mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(retrievalResult)

    when(mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(None))))
  }

  val controller = app.injector.instanceOf[ChoosePaperlessController]
  val optInController = app.injector.instanceOf[OptInController]
  val emailController = app.injector.instanceOf[EmailController]

  "The preferences action on non login version page" should {

    "show main banner" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 303
      redirectLocation(page).get must startWith("/paperless/optin")
      val page2 = optInController.displayOptIn(TestFixtures.sampleHostContext)(request)
      val document = Jsoup.parse(contentAsString(page2))
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "have correct form action to save preferences" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 303
      redirectLocation(page).get must startWith("/paperless/optin")
      val page2 = optInController.displayOptIn(TestFixtures.sampleHostContext)(request)
      val document = Jsoup.parse(contentAsString(page2))
      val url =
        paperless.routes.OptInController
          .submitOptIn(TestFixtures.sampleHostContext)
          .url
      document.getElementById("form-submit-email-address").attr("action") must endWith(url)
    }

    "audit the cohort information for the account details page" in new ChoosePaperlessControllerSetup {
      reset(mockAuditConnector)
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 303
      redirectLocation(page).get must startWith("/paperless/optin")
      optInController.displayOptIn(TestFixtures.sampleHostContext)(request)
      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Show Print Preference Option")
      value.request.detail("cohort") mustBe assignedCohort.toString
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new ChoosePaperlessControllerSetup {
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val page = controller
        .displayForm(cohort = None, emailAddress = None, hostContext = TestFixtures.sampleHostContext)(request)

      status(page) mustBe 303
      header("Location", page).get must be(
        paperless.routes.ChoosePaperlessController
          .displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)
          .url
      )
    }
  }

  "The preferences form" should {

    "render NO! email input field no email address is supplied, and no option selected" in new ChoosePaperlessControllerSetup {
      val page = optInController.displayOptIn(TestFixtures.sampleHostContext)(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") must be(null)
      document.getElementById("sps-opt-in").attr("checked") must be(empty)
      document.getElementById("sps-opt-in-2").attr("checked") must be(empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new ChoosePaperlessControllerSetup {
      val page = optInController.displayOptIn(
        TestFixtures.sampleHostContext
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") must be(null)
      document.getElementById("sps-opt-in").attr("checked") must be(empty)
      document.getElementById("sps-opt-in-2").attr("checked") must be(empty)
    }
  }

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new ChoosePaperlessControllerSetup {
      reset(emailAddressValidation)
      val page = optInController.submitOptIn(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody())

      status(page) mustBe 400
      val document = Jsoup.parse(contentAsString(page))
      document
        .select(".govuk-error-summary__title")
        .text mustBe "There is a problem"
      verifyNoInteractions(emailAddressValidation)
    }

    "show an error when opting-in if the email is incorrectly formatted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "invalid-email"

      val page = optInController.submitOptInEmail(None, TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("sps-opt-in-email" -> emailAddress)
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("sps-opt-in-email-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
      verifyNoInteractions(emailAddressValidation)
    }

    "show an error when opting-in if the email is not set" in new ChoosePaperlessControllerSetup {
      val page = optInController.submitOptInEmail(None, TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("sps-opt-in-email" -> "")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("sps-opt-in-email-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
      verifyNoInteractions(emailAddressValidation)
    }

    "successfully submit opt-in email with valid form and save preference with correct language type" in new ChoosePaperlessControllerSetup {
      reset(emailAddressValidation)
      val emailAddress = "test@example.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = optInController.submitOptInEmail(None, TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("sps-opt-in-email" -> emailAddress)
      )

      status(page) mustBe 303
      header("Location", page).get must include("/paperless/optin-confirmation")

      val prefsCaptor: ArgumentCaptor[TermsAndConditionsUpdate] =
        ArgumentCaptor.forClass(classOf[TermsAndConditionsUpdate])
      verify(mockPreferencesConnector)
        .optIn(prefsCaptor.capture())(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      private val capturedPrefs: TermsAndConditionsUpdate = prefsCaptor.getValue
      capturedPrefs.email mustBe Some(emailAddress)
    }

    "successfully submit opt-in email and create OptInPage from CohortCurrent.ipage for terms acceptance" in new ChoosePaperlessControllerSetup {
      reset(emailAddressValidation)
      val emailAddress = "test@example.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = optInController.submitOptInEmail(None, TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("sps-opt-in-email" -> emailAddress)
      )

      status(page) mustBe 303

      val prefsCaptor: ArgumentCaptor[TermsAndConditionsUpdate] =
        ArgumentCaptor.forClass(classOf[TermsAndConditionsUpdate])
      verify(mockPreferencesConnector)
        .optIn(prefsCaptor.capture())(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      private val capturedPrefs: TermsAndConditionsUpdate = prefsCaptor.getValue
      private val optInPageFromCohort = OptInPage.from(CohortCurrent.ipage)

      capturedPrefs.generic.isDefined mustBe true
      capturedPrefs.generic.get.optInPage.isDefined mustBe true
      capturedPrefs.generic.get.optInPage.get.version mustBe optInPageFromCohort.version
      capturedPrefs.generic.get.optInPage.get.cohort mustBe optInPageFromCohort.cohort
      capturedPrefs.generic.get.accepted mustBe true
    }

    "successfully submit opt-in email with changeEmail flag set to true" in new ChoosePaperlessControllerSetup {
      reset(emailAddressValidation)
      val emailAddress = "changed.email@example.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
      when(
        mockPreferencesConnector
          .changeEmailAddress(any[String], any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(HttpResponse(OK, "")))

      val page = optInController.submitOptInEmail(Some(true), TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("sps-opt-in-email" -> emailAddress)
      )

      status(page) mustBe 303
      header("Location", page).get must include("/paperless/optin-confirmation")

      verify(mockPreferencesConnector)
        .changeEmailAddress(is(emailAddress), any[Option[String]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )

      verifyNoInteractions(mockAuditConnector)
    }

    "audit the opted-in preference when submitting valid opt-in email" in new ChoosePaperlessControllerSetup {
      reset(mockAuditConnector)
      reset(emailAddressValidation)
      val emailAddress = "audit.test@example.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = optInController.submitOptInEmail(None, TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("sps-opt-in-email" -> emailAddress)
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.detail("email") mustBe emailAddress
      value.request.detail("digital") mustBe "true"
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      reset(emailAddressValidation)
      val emailAddress = "someone@email.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        paperless.routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString()
      )

      verify(emailAddressValidation).isValid(is(emailAddress))
      verify(mockPreferencesConnector)
        .optIn(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verifyNoMoreInteractions(emailAddressValidation)
    }

    "when opting-in, validate the email address, failed to save the preference and so not activate user and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        paperless.routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString()
      )

      verify(mockPreferencesConnector)
        .optIn(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verify(emailAddressValidation).isValid(is(emailAddress))

      verifyNoMoreInteractions(emailAddressValidation)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted when the user has no email address stored" in new ChoosePaperlessControllerSetup {
      reset(emailAddressValidation)
      val emailAddress = "someone@email.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          "accept-tc"          -> "true",
          "emailAlreadyStored" -> "false"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        paperless.routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString()
      )

      verify(emailAddressValidation).isValid(is(emailAddress))
      verify(mockPreferencesConnector)
        .optIn(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verifyNoMoreInteractions(emailAddressValidation)
    }

    "when opting-out, save the preference and redirect to the thank you page if survey was already presented" in new ChoosePaperlessControllerSetup {
      when(
        mockPreferencesConnector
          .optOut(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitForm(TestFixtures.sampleHostContextWithNoSurveyRequest)(
          FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")
        )

      status(page) mustBe 303
      header("Location", page).get must be(TestFixtures.sampleHostContext.returnUrl)

      verify(mockPreferencesConnector)
        .optOut(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verifyNoMoreInteractions(emailAddressValidation)
    }

    "when opting-out multipage, save the preference and redirect to the confirmation page" in new ChoosePaperlessControllerSetup {
      when(
        mockPreferencesConnector
          .optOut(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        optInController.submitOptIn(TestFixtures.sampleHostContextWithNoSurveyRequest)(
          FakeRequest().withFormUrlEncodedBody("sps-opt-in" -> "false")
        )

      status(page) mustBe 303
      header("Location", page).get must startWith("/paperless/optout-confirmation?digital=false")

      verify(mockPreferencesConnector)
        .optOut(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verifyNoMoreInteractions(emailAddressValidation)
    }

    "when opting-out, save the preference and redirect to the confirmation page" in new ChoosePaperlessControllerSetup {
      when(
        mockPreferencesConnector
          .optOut(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        optInController.submitOptIn(TestFixtures.sampleHostContextWithSurveyRequest)(
          FakeRequest().withFormUrlEncodedBody("sps-opt-in" -> "false")
        )

      status(page) mustBe 303
      header("Location", page).get must be(
        "/paperless/optout-confirmation?digital=false&returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D&cohort=mpaa8eDGyWF5GE8%2FdExi6A%3D%3D&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"
      )

      verify(mockPreferencesConnector)
        .optOut(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verifyNoMoreInteractions(emailAddressValidation)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "true"),
          "accept-tc" -> "true"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        paperless.routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString()
      )

      verify(mockPreferencesConnector)
        .optIn(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verifyNoMoreInteractions(emailAddressValidation)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@gmail.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(false)

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "false"),
          "accept-tc" -> "true"
        )
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)
    }

    "if the verified flag is any value other than true, treat it as false" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@gmail.com"
      when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(false)

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "hjgjhghjghjgj"),
          "accept-tc" -> "true"
        )
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)
    }

    "An audit event" should {

      "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from CohortCurrent.ipage" in new ChoosePaperlessControllerSetup {

        override def assignedCohort = CohortCurrent.ipage

        val emailAddress = "someone@email.com"
        when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
        when(
          mockPreferencesConnector
            .optIn(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        ).thenReturn(Future.successful(PreferencesCreated))

        val page = controller.submitForm(TestFixtures.sampleHostContext)(
          FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
        )

        status(page) mustBe 303

        val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
        verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

        private val value: MergedDataEvent = eventArg.getValue
        value.auditSource mustBe "preferences-frontend"
        value.auditType mustBe EventTypes.Succeeded
        value.request.tags must contain("transactionName" -> "Set Print Preference")
        value.request.detail("cohort") mustBe "IPage56"
        value.request.detail("journey") mustBe "AccountDetails"
        value.request.detail("utr") mustBe validUtr.value
        value.request.detail("nino") mustBe "N/A"
        value.request.detail("email") mustBe "someone@email.com"
        value.request.detail("digital") mustBe "true"
        value.request.detail("userConfirmedReadTandCs") mustBe "true"
        value.request.detail("newUserPreferencesCreated") mustBe "true"
      }

      "be created as EventTypes.Succeeded when an existing user is activated on submitting a print preference from CohortCurrent.ipage" in new ChoosePaperlessControllerSetup {

        override def assignedCohort = CohortCurrent.ipage

        val emailAddress = "someone@email.com"
        when(emailAddressValidation.isValid(is(emailAddress))).thenReturn(true)
        when(
          mockPreferencesConnector
            .optIn(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        ).thenReturn(Future.successful(PreferencesExists))

        val page = controller.submitForm(TestFixtures.sampleHostContext)(
          FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
        )

        status(page) mustBe 303

        val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
        verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

        private val value: MergedDataEvent = eventArg.getValue
        value.auditSource mustBe "preferences-frontend"
        value.auditType mustBe EventTypes.Succeeded
        value.request.tags must contain("transactionName" -> "Set Print Preference")
        value.request.detail("cohort") mustBe "IPage56"
        value.request.detail("journey") mustBe "AccountDetails"
        value.request.detail("utr") mustBe validUtr.value
        value.request.detail("nino") mustBe "N/A"
        value.request.detail("email") mustBe "someone@email.com"
        value.request.detail("digital") mustBe "true"
        value.request.detail("userConfirmedReadTandCs") mustBe "true"
        value.request.detail("newUserPreferencesCreated") mustBe "false"
      }

      "be created as EventTypes.Succeeded when choosing to not opt in" in new ChoosePaperlessControllerSetup {

        override def assignedCohort = CohortCurrent.ipage

        when(
          mockPreferencesConnector
            .optOut(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        ).thenReturn(Future.successful(PreferencesCreated))

        val page =
          controller.submitForm(TestFixtures.sampleHostContext)(
            FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")
          )

        status(page) mustBe 303

        val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
        verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

        private val value: MergedDataEvent = eventArg.getValue
        value.auditSource mustBe "preferences-frontend"
        value.auditType mustBe EventTypes.Succeeded
        value.request.tags must contain("transactionName" -> "Set Print Preference")
        value.request.detail("cohort") mustBe "IPage56"
        value.request.detail("journey") mustBe "AccountDetails"
        value.request.detail("utr") mustBe validUtr.value
        value.request.detail("nino") mustBe "N/A"
        value.request.detail("email") mustBe ""
        value.request.detail("digital") mustBe "false"
        value.request.detail("userConfirmedReadTandCs") mustBe "false"
        value.request.detail("newUserPreferencesCreated") mustBe "true"
      }
    }

    "/paperless/choose/capture" should {
      "redirect to returnUrl and include entityId in the query parameters " in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa")
        )
        when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(SilentRedirectJourney("test-entityId")))

        val res = controller.capture(testHostContext)(request)
        status(res) mustBe 303
        res.futureValue.header.headers("Location") must startWith("someReturnUrl")
        res.futureValue.header.headers("Location") must include("entityId=")
      }
      "redirect to OptIn page when no preference records found" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa")
        )
        when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(OptInJourney("No preferences found")))

        val page = controller.capture(testHostContext)(request)
        status(page) mustBe 303
        redirectLocation(page).get must startWith("/paperless/optin")
        val document = Jsoup.parse(contentAsString(page))
        document.getElementsByTag("sps-opt-in").attr("checked") must be(empty)
      }
      "redirect to re-OptIn page when preference found terms and conditions outdated" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa")
        )
        when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(ReOptInJourney("Preferences found", None)))
        val page = controller.capture(testHostContext)(request)
        status(page) mustBe 303
        redirectLocation(page).get must startWith("/paperless/choose/")
      }
      "redirect to email re-verification page when T&Cs accepted, but email is not verfied" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa")
        )
        when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(EmailVerificationJourney("T&C accepted, but email is not verified yet", "test@test.com"))
          )
        val page = controller.capture(testHostContext)(request)
        status(page) mustBe 303
        redirectLocation(page).get must startWith("/paperless/email-re-verify")
      }
    }

    "/paperless/choose/precheck" should {
      "handle silently redirect journey" in {
        when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(SilentRedirectJourney("test-entityId")))

        val expectedJsonBody =
          Json.obj("entityId" -> "test-entityId", "reason" -> "", "journeyType" -> "SILENT_REDIRECT")
        val res = controller.preCheck()(request)
        status(res) mustBe OK
        contentAsJson(res) mustBe expectedJsonBody
      }
      "handle email reverification journey" in {
        when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(EmailVerificationJourney("reason", "email")))

        val expectedJsonBody = Json.obj("reason" -> "reason", "email" -> "email", "journeyType" -> "EMAIL_VERIFICATION")
        val res = controller.preCheck()(request)
        status(res) mustBe OK
        contentAsJson(res) mustBe expectedJsonBody
      }
      "handle conflict journey" in {
        when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(ConflictJourney("Multiple Preferences Found")))

        val expectedJsonBody = Json.obj("reason" -> "Multiple Preferences Found", "journeyType" -> "CONFLICT")
        val res = controller.preCheck()(request)
        status(res) mustBe CONFLICT
        contentAsJson(res) mustBe expectedJsonBody
      }
      "handle other journey" in {
        when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(BounceEmailJourney("reason", "email")))

        val expectedJsonBody =
          Json.obj(
            "reason"      -> "reason",
            "email"       -> "email",
            "journeyType" -> "BOUNCE_EMAIL",
            "_type"       -> "model.BounceEmailJourney"
          )
        val res = controller.preCheck()(request)
        status(res) mustBe BAD_REQUEST
        contentAsJson(res) mustBe expectedJsonBody
      }
    }

    "EmailReVerification" should {
      "display email re-verification page when a valid email exists in host context" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa"),
          email = Some("test@test.com")
        )
        val page = emailController.processEmailReVerificationJourney(testHostContext)(request)
        status(page) mustBe 200
      }
      "not display email re-verification page when email does not exist" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa")
        )
        val page = emailController.processEmailReVerificationJourney(testHostContext)(request)
        status(page) mustBe 400
      }
    }

    "EmailBounce" should {
      "display email bounce page when a valid email exists in the host context" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa"),
          email = Some("test@test.com")
        )
        val page = emailController.processEmailBounceJourney(testHostContext)(request)
        val document = Jsoup.parse(contentAsString(page))

        document
          .getElementsByClass("govuk-service-navigation__service-name")
          .first()
          .text() mustBe "Sign up for Making Tax Digital for Income Tax"

        status(page) mustBe 200
      }
      "not display email bounce page when email does not exist" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa")
        )
        val page = emailController.processEmailBounceJourney(testHostContext)(request)
        status(page) mustBe 400
      }
    }

    "resendVerificationEmail" should {
      "display email confirmation page when a valid email exists in host context" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa"),
          email = Some("test@test.com")
        )
        when(
          mockPreferencesConnector
            .changeEmailAddress(any[String], any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(HttpResponse(OK, "")))
        val page = emailController.resendVerificationEmail(testHostContext)(request)

        status(page) mustBe 200

        verify(mockPreferencesConnector).changeEmailAddress(any, any)(any, any)
      }

      "not display email confirmation page when email does not exist" in {
        val testHostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText",
          regime = Some("itsa")
        )
        val page = emailController.resendVerificationEmail(testHostContext)(request)
        status(page) mustBe 400
      }
    }
  }

  "displayOptOutConfirmation" should {

    "display correct title" when {

      "regime is itsa" in new ChoosePaperlessControllerSetup {

        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", "someone@email.com"),
          ("email.confirm", "someone@email.com"),
          ("emailVerified", "true"),
          "accept-tc" -> "true"
        )

        val result: Future[Result] =
          controller.displayOptOutConfirmation(true, TestFixtures.reOptInHostContextWithRegime(Some("itsa")))(
            fakeRequest
          )

        status(result) must be(OK)

        val pageDocument: Document = Jsoup.parse(contentAsString(result))

        val messageApi: MessagesApi = app.injector.instanceOf[MessagesApi]
        val messages: Messages = messageApi.preferred(
          FakeRequest(EMPTY_STRING, EMPTY_STRING)
            .asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
            .withHeaders(newHeaders = "X-Session-Id" -> "someSessionId")
        )

        pageDocument.title() mustBe messages("sa_printing_preference.sps.itsa.opt_out_by_post_confimation_title")
      }

      "regime is not itsa" in new ChoosePaperlessControllerSetup {

        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", "someone@email.com"),
          ("email.confirm", "someone@email.com"),
          ("emailVerified", "true"),
          "accept-tc" -> "true"
        )

        val result: Future[Result] =
          controller.displayOptOutConfirmation(true, TestFixtures.reOptInHostContextWithRegime(Some("nino")))(
            fakeRequest
          )

        status(result) must be(OK)

        val pageDocument: Document = Jsoup.parse(contentAsString(result))

        val messageApi: MessagesApi = app.injector.instanceOf[MessagesApi]
        val messages: Messages = messageApi.preferred(
          FakeRequest(EMPTY_STRING, EMPTY_STRING)
            .asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
            .withHeaders(newHeaders = "X-Session-Id" -> "someSessionId")
        )

        pageDocument.title() mustBe messages("sa_printing_preference.sps.opt_out_by_post_confimation_title")
      }
    }
  }
}
