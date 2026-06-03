/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers

import connectors._
import helpers.Resources
import model.HostContext
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.{ AnyContentAsEmpty, Cookie, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.{ status, _ }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

class PaperlessStatusControllerSpec extends AnyWordSpec with MockitoSugar with GuiceOneAppPerSuite {

  "getPaperlessStatus" should {

    "return a 200 with an already opted in json" in new TestContext(
      preference = preferences(statusName = Some(StatusNameResponse.Alright))
    ) {
      val alrightResponse: JsValue = Resources.readJson("PaperlessStatusAlright.json")
      val welshAlrightResponse: JsValue = Resources.readJson("PaperlessStatusAlrightWelsh.json")
      verify(request = request, response = alrightResponse)
      verify(request = welshRequest, response = welshAlrightResponse)
    }

    "return a 200 with a bounced email json" in new TestContext(
      preference = preferences(hasBounces = true, statusName = Some(StatusNameResponse.BouncedEmail))
    ) {
      val bouncedResponse: JsValue = Resources.readJson("PaperlessStatusBounced.json")
      val welshBouncedResponse: JsValue = Resources.readJson("PaperlessStatusBouncedWelsh.json")
      verify(request = request, response = bouncedResponse)
      verify(request = welshRequest, response = welshBouncedResponse)

    }

    "return a 200 with a reoptin modified json" in new TestContext(
      preference =
        preferences(hasBounces = true, majorVersion = Some(0), statusName = Some(StatusNameResponse.ReOptInModified))
    ) {
      val bouncedResponse: JsValue = Resources.readJson("PaperlessStatusReOptInModified.json")
      val welshBouncedResponse: JsValue = Resources.readJson("PaperlessStatusReOptInModifiedWelsh.json")
      verify(request = request, response = bouncedResponse)
      verify(request = welshRequest, response = welshBouncedResponse)

    }

    "return a 200 with a email not verified json" in new TestContext(
      preference = preferences(isVerified = false, statusName = Some(StatusNameResponse.EmailNotVerified))
    ) {
      val notYetVerified: JsValue = Resources.readJson("PaperlessStatusNotVerified.json")
      val welshNotYetVerified: JsValue = Resources.readJson("PaperlessStatusNotVerifiedWelsh.json")
      verify(request = request, response = notYetVerified)
      verify(request = welshRequest, response = welshNotYetVerified)
    }

    "return a 200 with a paper json" in new TestContext(
      preference = preferences(termsAcceptance = false, statusName = Some(StatusNameResponse.Paper))
    ) {
      val paper: JsValue = Resources.readJson("PaperlessStatusPaper.json")
      val welshPaper: JsValue = Resources.readJson("PaperlessStatusPaperWelsh.json")
      verify(request = request, response = paper)
      verify(request = welshRequest, response = welshPaper)
    }

    "return a 200 with a new customer json when no preferences are found" in new TestContext(preference = None) {
      val newCustomer: JsValue = Resources.readJson("PaperlessStatusNewCustomer.json")
      val welshNewCustomer: JsValue = Resources.readJson("PaperlessStatusNewCustomerWelsh.json")
      verify(request = request, response = newCustomer)
      verify(request = welshRequest, response = welshNewCustomer)
    }

    "return a 200 with a no email json when a preference record is found but no has no email" in new TestContext(
      preference = preferences(containsEmail = false, statusName = Some(StatusNameResponse.NoEmail))
    ) {
      val noEmail: JsValue = Resources.readJson("PaperlessStatusNoEmail.json")
      val welshNoEmail: JsValue = Resources.readJson("PaperlessStatusNoEmailWelsh.json")
      verify(request = request, response = noEmail)
      verify(request = welshRequest, response = welshNoEmail)
    }

    "return a 200 with a re-opt-in customer json" in new TestContext(
      preference = preferences(majorVersion = Some(0), statusName = Some(StatusNameResponse.OldVersion))
    ) {
      val reOptIn: JsValue = Resources.readJson("PaperlessStatusReOptIn.json")
      val welshReOptIn: JsValue = Resources.readJson("PaperlessStatusReOptInWelsh.json")
      verify(request = request, response = reOptIn)
      verify(request = welshRequest, response = welshReOptIn)
    }
  }

  private def preferences(
    isVerified: Boolean = true,
    hasBounces: Boolean = false,
    termsAcceptance: Boolean = true,
    containsEmail: Boolean = true,
    majorVersion: Option[Int] = None,
    statusName: Option[StatusNameResponse]
  ): Option[PreferenceResponse] = {
    val email =
      if (!containsEmail)
        None
      else
        Some(
          EmailPreference(
            "pihklyljtgoxeoh@mail.com",
            isVerified = isVerified,
            hasBounces = hasBounces,
            mailboxFull = false,
            linkSent = None
          )
        )
    val termsAndConditions = Map(
      "generic" -> TermsAndConditionsAcceptance(termsAcceptance, majorVersion = majorVersion)
    )
    Some(
      PreferenceResponse(
        termsAndConditions,
        email,
        status = statusName.map(PaperlessStatusResponse.apply)
      )
    )
  }

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val welshRequest: FakeRequest[AnyContentAsEmpty.type] = request.withCookies(Cookie("PLAY_LANG", "CY"))
  private implicit val hostContext: HostContext = HostContext(returnUrl = "", returnLinkText = "")

  private lazy val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private lazy val mockPreferencesConnector = mock[PreferencesConnector]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector)
      )
      .configure(
        "metrics.enabled" -> false
      )
      .build()

  class TestContext(preference: Option[PreferenceResponse]) extends PlaySpec {

    private val controller = app.injector.instanceOf[PaperlessStatusController]

    def submitRequest(request: FakeRequest[AnyContentAsEmpty.type]): Future[Result] =
      controller.getPaperlessStatus(hostContext)(request)

    def verify(request: FakeRequest[AnyContentAsEmpty.type], response: JsValue): Assertion = {
      val result: Future[Result] = submitRequest(request)
      status(result) mustBe 200
      contentAsJson(result) mustBe response
    }

    when(
      mockAuthConnector.authorise[Unit](any[Predicate], any[Retrieval[Unit]])(any[HeaderCarrier], any[ExecutionContext])
    ).thenReturn(Future.successful(()))
    when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(preference))
  }

}
