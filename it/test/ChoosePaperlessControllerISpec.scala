/*
 * Copyright 2026 HM Revenue & Customs
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
