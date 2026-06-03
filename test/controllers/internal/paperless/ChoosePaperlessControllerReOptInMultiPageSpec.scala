/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import _root_.connectors.*
import controllers.AuthRetrievalsSetup
import controllers.internal.{ OptInCohort, ReOptInPage54, paperless }
import helpers.TestFixtures
import helpers.TestFixtures.reOptInHostContextWithRegime
import model.Language.English
import model.*
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{ any, eq as is }
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{ DefaultMessagesApiProvider, Lang }
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import service.PreCheckService
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, Retrieval, ~ }
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, ConfidenceLevel }
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddressValidation
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.{ Instant, LocalDate, ZoneOffset, ZonedDateTime }
import scala.concurrent.{ ExecutionContext, Future }

trait ChoosePaperlessControllerReOptInMultiPageSetup {
  def assignedCohort: OptInCohort = ReOptInPage54
  val validUtr = SaUtr("1234567890")
  val request = FakeRequest()

  def paramValue(url: String, param: String): Option[String] =
    url.split(Array('=', '?', '&')).drop(1).sliding(2, 2).map(x => x(0) -> x(1)).toMap.get(param)

  def reOptInHostContext(email: String) =
    HostContext(
      returnUrl = "someReturnUrl",
      returnLinkText = "someReturnLinkText",
      email = Some(email),
      cohort = Some(ReOptInPage54)
    )

  def reOptInHostContext() =
    HostContext(
      returnUrl = "someReturnUrl",
      returnLinkText = "someReturnLinkText",
      cohort = Some(ReOptInPage54)
    )
}

class ChoosePaperlessControllerReOptInMultiPageSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite
    with ChoosePaperlessControllerReOptInMultiPageSetup with AuthRetrievalsSetup {

  val mockAuditConnector = mock[AuditConnector]
  val mockPreferencesConnector = mock[PreferencesConnector]
  val emailAddressValidation = mock[EmailAddressValidation]
  val mockPreCheckService = mock[PreCheckService]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val localDate: LocalDate = LocalDate.now()

  def emailPref(
    email: String = "test@test",
    isVerified: Boolean = true,
    hasBounces: Boolean = false,
    mailboxFull: Boolean = false,
    linkSent: Option[LocalDate] = Some(localDate),
    language: Option[Language] = Some(English),
    pendingEmail: Option[String] = None
  ): EmailPreference =
    EmailPreference(email, isVerified, hasBounces, mailboxFull, linkSent, language, pendingEmail)

  def getTnc(
    accepted: Boolean = true,
    updatedAt: Option[Instant] = None,
    majorVersion: Option[Int] = Some(1),
    paperless: Option[Boolean] = Some(true)
  ): Map[String, TermsAndConditionsAcceptance] =
    Map("generic" -> TermsAndConditionsAcceptance(accepted, updatedAt, majorVersion, paperless))

  def preferenceResponse(
    tnc: Map[String, TermsAndConditionsAcceptance] = getTnc(majorVersion = Some(0)),
    email: Option[EmailPreference] = Some(emailPref()),
    surveys: Option[List[Survey]] = None,
    entityId: Option[String] = None
  ): PreferenceResponse =
    PreferenceResponse(tnc, email, surveys, entityId)

  when(
    mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )
  ).thenReturn(retrievalResult)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "sso.encryption.key"          -> "P5xsJ9Nt+quxGZzB4DeLfw==",
        "sso.encryption.previousKeys" -> Seq.empty
      )
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector),
        bind[EmailAddressValidation].toInstance(emailAddressValidation),
        bind[PreCheckService].toInstance(mockPreCheckService),
        bind[Metrics].toInstance(Mockito.mock(classOf[Metrics]))
      )
      .configure(
        "metrics.enabled" -> false
      )
      .build()
  val messageApi = fakeApplication().injector.instanceOf[DefaultMessagesApiProvider].get

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
  val reOptInController = app.injector.instanceOf[ReOptInController]

  "displayForm for ReOptInPage54 cohort request" should {
    "redirect to /paperless/reoptin" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val page =
        controller.displayForm(Some(assignedCohort), None, reOptInHostContext("foo@bar.com"))(request)
      status(page) mustBe 303
      redirectLocation(page).get must startWith("/paperless/reoptin")
    }
  }

  "displayReOptIn" should {

    "show main banner" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val page2 = reOptInController.displayMultiPageReOptIn(reOptInHostContext("foo@bar.com"))(request)
      val document = Jsoup.parse(contentAsString(page2))
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "show reoptin title" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val reOptInTitle = messageApi.translate("reoptin_page54.fg_page.title", Nil)(Lang("en", "")).get
      val page2 = reOptInController.displayMultiPageReOptIn(reOptInHostContext("foo@bar.com"))(request)
      val document = Jsoup.parse(contentAsString(page2))
      document.getElementsByTag("title").get(0).text mustBe reOptInTitle
    }

    "have correct form action" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val page = reOptInController.displayMultiPageReOptIn(reOptInHostContext("foo@bar.com"))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") must endWith(
        paperless.routes.ReOptInController.submitMultiPageReOptIn(reOptInHostContext("foo@bar.com")).url
      )
    }

  }

  "submitReOptIn" should {

    "when opting-out multipage, save the preference and redirect to the confirm page" in new ChoosePaperlessControllerSetup {
      when(
        mockPreferencesConnector
          .optOut(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesExists))

      val page =
        reOptInController.submitMultiPageReOptIn(TestFixtures.sampleHostContextWithSurveyRequest)(
          FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "false")
        )

      status(page) mustBe 303
      header("Location", page).get must startWith("/paperless/optout-confirmation")

      verify(mockPreferencesConnector)
        .optOut(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verifyNoMoreInteractions(emailAddressValidation)
    }

    "when opting-out multipage for ITSA, save the preference and show the confirmation page" in new ChoosePaperlessControllerSetup {
      when(
        mockPreferencesConnector
          .optOut(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesExists))

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(preferenceResponse())))

      val page =
        reOptInController.submitMultiPageReOptIn(reOptInHostContextWithRegime(Some("itsa")))(
          FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "false")
        )

      status(page) mustBe 303
      header("Location", page).get must startWith("/paperless/optout-confirmation")

      verify(mockPreferencesConnector)
        .optOut(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )
    }

    "when opting-in multipage, do not save the preference and redirect to the email capture page" in new ChoosePaperlessControllerSetup {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Right[PreferenceStatus, PreferenceResponse](preferenceResponse())))
      when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(ReOptInJourney("Preferences found", None)))

      val page =
        reOptInController.submitMultiPageReOptIn(TestFixtures.sampleHostContextWithSurveyRequest)(
          FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "true")
        )

      status(page) mustBe 303
      header("Location", page).get must startWith("/paperless/reoptin-email")

      verifyNoMoreInteractions(mockPreferencesConnector, emailAddressValidation)
    }

    "when opting-in multipage itsa with bounced email and out of date preferences redirect to enter new email only" in new ChoosePaperlessControllerSetup {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(
            Right[PreferenceStatus, PreferenceResponse](
              preferenceResponse(email = Some(emailPref(hasBounces = true)))
            )
          )
        )
      when(mockPreCheckService.determineJourney()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(ReOptInModifiedJourney("Preferences found", None)))

      val page =
        reOptInController.submitMultiPageReOptIn(reOptInHostContextWithRegime(Some("itsa")))(
          FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "true")
        )
      status(page) mustBe 303
      header("Location", page).get must startWith("/paperless/reoptin-bounce-email")
      verifyNoMoreInteractions(emailAddressValidation)
    }

    "show the email only page when displaying the optin journey for reoptin with a bounce" in new ChoosePaperlessControllerSetup {
      val page = reOptInController.displayMultiPageReOptInBounceEmail(reOptInHostContext("foo@bar.com"))(request)
      val document = Jsoup.parse(contentAsString(page))
      val reOptInTitle =
        messageApi.translate("sa_printing_preference.sps_opt_in_email", Nil)(Lang("en", "")).get
      document.getElementsByTag("title").get(0).text mustBe reOptInTitle
    }
  }
  "displayMultiPageReOptIn" should {

    "show main banner" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val page = reOptInController.displayMultiPageReOptInEmail(reOptInHostContext("foo@bar.com"))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "show reoptin title" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val reOptInTitle =
        messageApi.translate("sa_printing_preference.sps_re_opt_in_email.title", Nil)(Lang("en", "")).get
      val page = reOptInController.displayMultiPageReOptInEmail(reOptInHostContext("foo@bar.com"))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("title").get(0).text mustBe reOptInTitle
    }

    "have correct form action" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val page = reOptInController.submitMultiPageReOptInEmail(false, reOptInHostContext("foo@bar.com"))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") must endWith(
        paperless.routes.ReOptInController
          .submitMultiPageReOptInEmail(false, reOptInHostContext("foo@bar.com"))
          .url
      )
    }

    "have correct form action for a bounce" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val page = reOptInController.submitMultiPageReOptInEmail(true, reOptInHostContext("foo@bar.com"))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") must endWith(
        paperless.routes.ReOptInController
          .submitMultiPageReOptInEmail(true, reOptInHostContext("foo@bar.com"))
          .url
      )
    }

    "show an error if no email has been chosen" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      reset(emailAddressValidation)
      val page = reOptInController.submitMultiPageReOptInEmail(false, reOptInHostContext("foo@bar.com"))(request)

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("sps-re-opt-in-error").text() must include("Select which email address to use")
      verifyNoInteractions(emailAddressValidation)
    }

    "show an error if no email has been chosen and is a bounce" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      reset(emailAddressValidation)
      val page = reOptInController.submitMultiPageReOptInEmail(true, reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "true")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("sps-re-opt-in-email-error").text() must include(
        "Enter an email address in the correct format, like name@example.com"
      )
      verifyNoInteractions(emailAddressValidation)
    }

    "go back to input page if no email has been chosen for a bounce" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      reset(emailAddressValidation)
      val page = reOptInController.submitMultiPageReOptInEmail(true, reOptInHostContext("foo@bar.com"))(request)

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("sps-re-opt-in-email").text() mustNot be(null)
      verifyNoInteractions(emailAddressValidation)
    }

    "show an error if change email has been chosen but no email" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      reset(emailAddressValidation)
      val page = reOptInController.submitMultiPageReOptInEmail(false, reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "true")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("sps-re-opt-in-email-error").text() must include(
        "Enter an email address in the correct format"
      )

      verifyNoInteractions(emailAddressValidation)
    }

    "show an error if change email has been chosen and empty email" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      reset(emailAddressValidation)
      val page = reOptInController.submitMultiPageReOptInEmail(false, reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "true", "sps-re-opt-in-email" -> "")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("sps-re-opt-in-email-error").text() must include(
        "Enter an email address in the correct format"
      )

      verifyNoInteractions(emailAddressValidation)
    }

    "show an error if change email has been chosen and email in wrong format" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      reset(emailAddressValidation)
      val page = reOptInController.submitMultiPageReOptInEmail(false, reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "true", "sps-re-opt-in-email" -> "not-an-email")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("sps-re-opt-in-email-error").text() must include(
        "Enter an email address in the correct format"
      )

      verifyNoInteractions(emailAddressValidation)
    }

    "update preferences and change email address if change email has been chosen and email in the correct format" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val emailAddress = "foo@abc.com"
      reset(emailAddressValidation)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      when(
        mockPreferencesConnector
          .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(HttpResponse(OK, "")))

      val page = reOptInController.submitMultiPageReOptInEmail(false, reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "true", "sps-re-opt-in-email" -> emailAddress)
      )

      status(page) mustBe 303

      verify(mockPreferencesConnector)
        .optIn(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verify(mockPreferencesConnector)
        .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
    }

    "update preferences and change email address if change email has been chosen and email in the correct format for a bounce" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val emailAddress = "foo@abc.com"
      reset(emailAddressValidation)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      when(
        mockPreferencesConnector
          .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(HttpResponse(OK, "")))

      val page = reOptInController.submitMultiPageReOptInEmail(true, reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "true", "sps-re-opt-in-email" -> emailAddress)
      )

      status(page) mustBe 303

      verify(mockPreferencesConnector)
        .optIn(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verify(mockPreferencesConnector)
        .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
    }

    "only update preferences if current email address has been chosen" in new ChoosePaperlessControllerReOptInMultiPageSetup {
      val emailAddress = "foo@bar.com"
      reset(emailAddressValidation)
      when(
        mockPreferencesConnector
          .optIn(any[TermsAndConditionsUpdate])(
            any[HeaderCarrier],
            any[HostContext],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      when(
        mockPreferencesConnector
          .changeEmailAddress(is(emailAddress), any[Option[String]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(HttpResponse(OK, "")))

      val page = reOptInController.submitMultiPageReOptInEmail(false, reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("sps-re-opt-in" -> "false")
      )

      status(page) mustBe 303

      verify(mockPreferencesConnector)
        .optIn(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )

      verify(mockPreferencesConnector, never()).changeEmailAddress(is(emailAddress), any[Option[String]])(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    }
  }

}
