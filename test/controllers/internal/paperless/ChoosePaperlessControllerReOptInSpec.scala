/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import _root_.connectors.*
import controllers.AuthRetrievalsSetup
import controllers.internal.{ CohortCurrent, OptInCohort }
import helpers.TestFixtures
import model.HostContext
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{ any, eq as is }
import org.mockito.Mockito.*
import org.mockito.{ ArgumentCaptor, Mockito }
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.DefaultMessagesApiProvider
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
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

trait ChoosePaperlessControllerReOptInSetup {
  def assignedCohort: OptInCohort = CohortCurrent.reoptinpage
  val validUtr = SaUtr("1234567890")
  val request = FakeRequest()
  def paramValue(url: String, param: String): Option[String] =
    url.split(Array('=', '?', '&')).drop(1).sliding(2, 2).map(x => x(0) -> x(1)).toMap.get(param)
}

class ChoosePaperlessControllerReOptInSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite
    with ChoosePaperlessControllerReOptInSetup with AuthRetrievalsSetup {

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
  val messageApi = fakeApplication().injector.instanceOf[DefaultMessagesApiProvider].get

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
  val controller = app.injector.instanceOf[ChoosePaperlessController]

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new ChoosePaperlessControllerReOptInSetup {
      reset(emailAddressValidation)
      val page =
        controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(FakeRequest().withFormUrlEncodedBody())

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text mustBe "Confirm if you want paperless notifications"
      verifyNoInteractions(emailAddressValidation)
    }

    "show an error when opting-in if the email is incorrectly formatted" in new ChoosePaperlessControllerReOptInSetup {
      val emailAddress = "invalid-email"

      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress)
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("email.main-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
      verifyNoInteractions(emailAddressValidation)
    }

    "show an error when opting-in if the T&C's are not accepted" in new ChoosePaperlessControllerReOptInSetup {
      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "accept-tc" -> "false")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))

      document
        .getElementById("terms-and-conditions")
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "terms and conditions"

      document
        .getElementById("accept-tc-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "You must agree to the terms and conditions to use this service"
      verifyNoInteractions(emailAddressValidation)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new ChoosePaperlessControllerReOptInSetup {
      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest()
          .withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress)
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("accept-tc-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "You must agree to the terms and conditions to use this service"
      verifyNoInteractions(emailAddressValidation)
    }

    "show an error when opting-in if the email is not set" in new ChoosePaperlessControllerReOptInSetup {

      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> "", "accept-tc" -> "true")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("email.main-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
      verifyNoInteractions(emailAddressValidation)
    }

    "when re-opting-in, do not validate the email address, save the preference and redirect to confirmation page" in new ChoosePaperlessControllerReOptInSetup {
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
      val testHc = TestFixtures.reOptInHostContext("foo@bar.com")
      val page = controller.submitForm(testHc)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      header("Location", page).get must startWith("/paperless/optout-confirmation?digital=true&returnUrl")
      status(page) mustBe 303
      verify(mockPreferencesConnector)
        .optIn(any[TermsAndConditionsUpdate])(
          any[HeaderCarrier],
          any[HostContext],
          any[ExecutionContext]
        )
      verifyNoMoreInteractions(emailAddressValidation)
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from CohortCurrent.reoptinpage" in new ChoosePaperlessControllerReOptInSetup {

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

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "ReOptInPage10"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe "someone@email.com"
      value.request.detail("digital") mustBe "true"
      value.request.detail("userConfirmedReadTandCs") mustBe "true"
      value.request.detail("newUserPreferencesCreated") mustBe "true"
    }

    "be created as EventTypes.Succeeded when an existing user is activated on submitting a print preference from CohortCurrent.reoptinpage" in new ChoosePaperlessControllerReOptInSetup {

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

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "ReOptInPage10"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe "someone@email.com"
      value.request.detail("digital") mustBe "true"
      value.request.detail("userConfirmedReadTandCs") mustBe "true"
      value.request.detail("newUserPreferencesCreated") mustBe "false"
    }

    "be created as EventTypes.Succeeded when choosing to not opt in" in new ChoosePaperlessControllerReOptInSetup {

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
        controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
          FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")
        )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "ReOptInPage10"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe ""
      value.request.detail("digital") mustBe "false"
      value.request.detail("userConfirmedReadTandCs") mustBe "false"
      value.request.detail("newUserPreferencesCreated") mustBe "true"
    }
  }
}
