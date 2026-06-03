/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import _root_.connectors.*
import controllers.AuthRetrievalsSetup
import controllers.internal.{ OptInCohort, ReOptInPage54 }
import model.HostContext
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.mockito.{ ArgumentCaptor, Mockito }
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{ DefaultMessagesApiProvider, Lang }
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, Retrieval, ~ }
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, ConfidenceLevel }
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddressValidation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ EventTypes, MergedDataEvent }
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.{ ZoneOffset, ZonedDateTime }
import scala.concurrent.{ ExecutionContext, Future }

trait ChoosePaperlessControllerEmailVerificationPageSetup {
  val validUtr = SaUtr("1234567890")
  val request = FakeRequest()
  val welshRequest = request.withCookies(Cookie("PLAY_LANG", "CY"))

  def assignedCohort: OptInCohort = ReOptInPage54

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

class ChoosePaperlessControllerEmailVerificationPageSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite
    with ChoosePaperlessControllerEmailVerificationPageSetup with AuthRetrievalsSetup {

  val mockAuditConnector = mock[AuditConnector]
  val mockPreferencesConnector = mock[PreferencesConnector]
  val emailAddressValidation = mock[EmailAddressValidation]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  when(
    mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )
  ).thenReturn(retrievalResult)
  val messageApi = fakeApplication().injector.instanceOf[DefaultMessagesApiProvider].get
  val controller = app.injector.instanceOf[ChoosePaperlessController]
  val optInController = app.injector.instanceOf[OptInController]
  val emailController = app.injector.instanceOf[EmailController]

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
        bind[Metrics].toInstance(Mockito.mock(classOf[Metrics]))
      )
      .configure("metrics.enabled" -> false)
      .build()

  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuthConnector)
    reset(mockPreferencesConnector)
    reset(emailAddressValidation)
    when(
      mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(retrievalResult)

    when(mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(None))))

  }

  "displayEmailConfirmation" should {

    "display form for the email confirmation" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val page = emailController.displayEmailConfirmation(reOptInHostContext(email))(request)
      status(page) mustBe 200
    }

    "email confirmation page includes the email given" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val page = emailController.displayEmailConfirmation(reOptInHostContext(email))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.text() must include(email)
    }

    "contain close button and link to 'Use a different email address'" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val close =
        messageApi.translate("sa_printing_preference.sps_email_confirm.button.close", Nil)(Lang("en", "")).get
      val linkForDifferentEmailAddress =
        messageApi.translate("sa_printing_preference.sps_email_confirm.another.email", Nil)(Lang("en", "")).get
      val page = emailController.displayEmailConfirmation(reOptInHostContext(email))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByClass("govuk-button").eachText() must contain(close)
      document.getElementsByClass("govuk-link").eachText() must contain(linkForDifferentEmailAddress)
    }

    "audit the button click information on 'Close'" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      reset(mockAuditConnector)
      val email = "foo@bar.com"
      val page = emailController.updateEmailVerificationStatus("anyJourney", reOptInHostContext(email))(request)
      status(page) mustBe 303

      redirectLocation(page).get must startWith("someReturnUrl")

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.detail("email") mustBe email
      value.request.detail("status") mustBe "Close Email Confirmation Page"
    }

  }
  "displayEmailConfirmation_welsh" should {

    "display form for the email confirmation" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val page = emailController.displayEmailConfirmation(reOptInHostContext(email))(welshRequest)
      status(page) mustBe 200
    }

    "email confirmation page includes the email given" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val page = emailController.displayEmailConfirmation(reOptInHostContext(email))(welshRequest)
      val document = Jsoup.parse(contentAsString(page))
      document.text() must include(email)
    }

    "contain close button and link to 'Use a different email address'" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val close =
        messageApi.translate("sa_printing_preference.sps_email_confirm.button.close", Nil)(Lang("cy", "")).get
      val linkForDifferentEmailAddress =
        messageApi.translate("sa_printing_preference.sps_email_confirm.another.email", Nil)(Lang("cy", "")).get
      val page = emailController.displayEmailConfirmation(reOptInHostContext(email))(welshRequest)
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByClass("govuk-button").eachText() must contain(close)
      document.getElementsByClass("govuk-link").eachText() must contain(linkForDifferentEmailAddress)
    }

    "audit the button click information on 'Close'" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      reset(mockAuditConnector)
      val email = "foo@bar.com"
      val page =
        emailController.updateEmailVerificationStatus("anyJourney", reOptInHostContext(email))(welshRequest)
      status(page) mustBe 303

      redirectLocation(page).get must startWith("someReturnUrl")

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.detail("email") mustBe email
      value.request.detail("status") mustBe "Close Email Confirmation Page"
    }
  }

  "displayOptInConfirmation" should {

    "display form for the email confirmation" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val page = optInController.displayOptInConfirmation(reOptInHostContext(email))(request)
      status(page) mustBe 200
    }

    "email confirmation page includes the email given" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val page = optInController.displayOptInConfirmation(reOptInHostContext(email))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.text() must include(email)
    }

    "contain close button and link to 'Use a different email address'" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val close =
        messageApi.translate("sa_printing_preference.sps_email_confirm.button.close", Nil)(Lang("en", "")).get
      val linkForDifferentEmailAddress =
        messageApi.translate("sa_printing_preference.sps_email_confirm.another.email", Nil)(Lang("en", "")).get

      val page = optInController.displayOptInConfirmation(reOptInHostContext(email))(request)
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByClass("govuk-button").eachText() must contain(close)
      document.getElementsByClass("govuk-link").eachText() must contain(linkForDifferentEmailAddress)
    }

    "audit the button click information on 'Close'" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      reset(mockAuditConnector)
      val email = "foo@bar.com"
      val page = emailController.updateEmailVerificationStatus("anyJourney", reOptInHostContext(email))(request)
      status(page) mustBe 303

      redirectLocation(page).get must startWith("someReturnUrl")

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.detail("email") mustBe email
      value.request.detail("status") mustBe "Close Email Confirmation Page"
    }
  }
  "displayOptInConfirmation_welsh" should {

    "display form for the email confirmation" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val page = optInController.displayOptInConfirmation(reOptInHostContext(email))(welshRequest)
      status(page) mustBe 200
    }

    "email confirmation page includes the email given" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val page = optInController.displayOptInConfirmation(reOptInHostContext(email))(welshRequest)
      val document = Jsoup.parse(contentAsString(page))
      document.text() must include(email)
    }

    "contain close button and link to 'Use a different email address'" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      val email = "foo@bar.com"
      val close =
        messageApi.translate("sa_printing_preference.sps_email_confirm.button.close", Nil)(Lang("cy", "")).get
      val linkForDifferentEmailAddress =
        messageApi.translate("sa_printing_preference.sps_email_confirm.another.email", Nil)(Lang("cy", "")).get

      val page = optInController.displayOptInConfirmation(reOptInHostContext(email))(welshRequest)
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByClass("govuk-button").eachText() must contain(close)
      document.getElementsByClass("govuk-link").eachText() must contain(linkForDifferentEmailAddress)
    }

    "audit the button click information on 'Close'" in new ChoosePaperlessControllerEmailVerificationPageSetup {
      reset(mockAuditConnector)
      val email = "foo@bar.com"
      val page =
        emailController.updateEmailVerificationStatus("anyJourney", reOptInHostContext(email))(welshRequest)
      status(page) mustBe 303

      redirectLocation(page).get must startWith("someReturnUrl")

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.detail("email") mustBe email
      value.request.detail("status") mustBe "Close Email Confirmation Page"
    }
  }

}
