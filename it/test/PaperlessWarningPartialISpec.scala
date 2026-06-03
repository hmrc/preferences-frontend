/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

import com.github.tomakehurst.wiremock.client.WireMock.{ getRequestedFor, postRequestedFor, urlEqualTo }
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import stubs.{ WireMockStubs, WireMockUtil }
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto

import java.net.URLEncoder
import java.util.UUID

class PaperlessWarningPartialISpec
    extends PlaySpec with TestUser with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience
    with WireMockUtil with WireMockStubs {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "controllers.controllers.internal.ActivationController.needsAuth" -> false,
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

  trait Fixture {
    self =>
    val ac = app.injector.instanceOf[ApplicationCrypto]

    def encryptAndEncode(ac: ApplicationCrypto, value: String): String =
      URLEncoder.encode(ac.QueryParameterCrypto.encrypt(PlainText(value)).value, "UTF-8")

    def buildReturnUrlAndLink = {
      val returnUrl = "http://some/other/url"
      val returnLinkText = "Continue"
      (
        encryptAndEncode(ac, returnUrl),
        encryptAndEncode(ac, returnLinkText)
      )
    }

    def buildUrl: String = {
      val (returnUrl, text) = buildReturnUrlAndLink
      s"/paperless/warnings?returnUrl=$returnUrl&returnLinkText=$text"
    }
  }

  "Paperless warnings partial " should {
    "return not authorised when no credentials supplied" in new Fixture {
      stubForUnauthorised
      val request = FakeRequest(GET, buildUrl)
      val response = route(app, request).get
      status(response) must be(UNAUTHORIZED)
    }

    "be not found if the user has no GetPreferences with utr only" in new Fixture {
      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesNotFound
      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get
      status(response) must be(NOT_FOUND)
      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }
  }

  "Paperless warnings partial for verification pending" should {

    "have a verification warning for the unverified email" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedIn.json", Map("email" -> email))

      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must include(s"Verify your email address for paperless notifications")
      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }

    "have no warning if user then verifies email" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(
        stubForPreferencesWithResponse,
        "PreferenceResponsePaperlessVerifiedEmail.json",
        Map("email" -> email)
      )

      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must be("")

      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }

    "have no warning if user then opts out" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedOut.json")

      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must be("")

      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }

    "have verification warning if user then changes email" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedIn.json", Map("email" -> email))

      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must include("Verify your email address for paperless notifications")

      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }
  }

  "Paperless warnings partial for a bounced unverified email address" should {

    "have a bounced warning" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponsePaperlessBounced.json", Map("email" -> email))

      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must include("There&#x27;s a problem with your paperless notification emails")

      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }

    "have inbox full warning if user resends link and their inbox is full" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseInboxFull.json", Map("email" -> email))

      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must include("Your inbox is full")

      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }
  }

  "Paperless warnings partial for opted out user" should {

    "be empty" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedOut.json")

      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)

      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }
  }

  "Paperless warnings partial for a bounced verified email address" should {

    "have a bounced warning" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponsePaperlessBounced.json", Map("email" -> email))

      val request = FakeRequest(GET, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must include("There&#x27;s a problem with your paperless notification emails")

      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }
  }
}
