/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

import connectors.{ PreferenceResponse, PreferencesConnector }
import controllers.internal.IPage7
import model.HostContext
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import stubs.{ WireMockStubs, WireMockUtil }
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FileLoader

import scala.concurrent.ExecutionContext.Implicits.*

class ChoosePaperlessControllerISpec
    extends PlaySpec with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with BeforeAndAfterAll
    with WireMockUtil with WireMockStubs {
  spec =>

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "play.filters.csrf.header.bypassHeaders.Csrf-Token"               -> "nocheck",
        "controllers.controllers.internal.ActivationController.needsAuth" -> true,
        "play.http.router"                                                -> "legacy.Routes",
        "metrics.enabled"                                                 -> false,
        "auditing.enabled"                                                -> false,
        "metrics.graphite.enabled"                                        -> false,
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter"),
        "microservice.services.auth-login-api.port" -> wireMockServer.port(),
        "microservice.services.auth.port"           -> wireMockServer.port(),
        "microservice.services.preferences.port"    -> wireMockServer.port()
      )
      .build()

  "submitForm" should {
    "should create a preference with specified cohort" in {
      val utr = Generate.utr
      buildAuthStub(withUtr = Some(utr))

      val queryString = model.HostContext.hostContextBinder
        .unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar", cohort = Some(IPage7)))

      val email = "test@foo.com"

      stubForOptIn
      val stubResponse =
        FileLoader.readAndSubstitute("PreferenceResponseOptedIn.json", Map("email" -> email, "pendingEmail" -> email))
      stubForPreferencesWithResponse(stubResponse)

      val fakeRequest = FakeRequest(POST, s"/paperless/choose?$queryString")
        .withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", email),
          ("email.confirm", email),
          ("emailVerified", "true"),
          "accept-tc" -> "true"
        )
        .withSession(SessionKeys.authToken -> "Bearer testToken")
        .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
        .withHeaders("Csrf-Token" -> "nocheck")
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(fakeRequest)

      val result = route(app, fakeRequest).get
      status(result) mustBe 303

      val preferencesConnector = app.injector.instanceOf[PreferencesConnector]
      val preferencesResponse: Option[PreferenceResponse] =
        preferencesConnector.getPreferences().futureValue

      preferencesResponse.get.termsAndConditions("generic").majorVersion.get mustBe IPage7.majorVersion
      preferencesResponse.get.email.get.pendingEmail.get mustBe "test@foo.com"
    }
  }
}
