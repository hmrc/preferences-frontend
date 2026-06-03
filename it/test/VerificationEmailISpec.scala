/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

import com.github.tomakehurst.wiremock.client.WireMock.{ getRequestedFor, postRequestedFor, putRequestedFor, urlEqualTo }
import org.jsoup.Jsoup
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

class VerificationEmailISpec
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
      s"/paperless/resend-verification-email?returnUrl=$returnUrl&returnLinkText=$text"
    }
  }

  "Verification email confirmation" should {

    "confirm email has been sent to the users verification email address" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedIn.json", Map("email" -> email))
      stubForPreferencesPendingEmail

      val request = FakeRequest(POST, buildUrl).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must (
        include("Verification email sent") and include(s"A new email has been sent to $email")
      )

      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      wireMockServer.verify(1, getRequestedFor(urlEqualTo("/preferences")))
    }
  }

  "Attempt to verify an email" should {

    "display success message if the email link is valid" in new Fixture {
      stubForPreferencesPutEmail(OK, "success")

      val token = UUID.randomUUID().toString

      val request = FakeRequest(GET, s"/sa/print-preferences/verification/$token")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (
        include("Email address verified") and
          include("To read your online tax letters, sign in to HMRC services and select &#x27;Messages&#x27;.") and
          include("Continue to your HMRC online account")
      )

      Jsoup.parse(body).getElementById("link-to-home").toString() must include("/account")

      wireMockServer.verify(1, putRequestedFor(urlEqualTo("/preferences/email")))
    }

    "display expiry message if the link has expired" in new Fixture {
      stubForPreferencesPutEmail(GONE, "expired_token")

      val token = UUID.randomUUID().toString

      val request = FakeRequest(GET, s"/sa/print-preferences/verification/$token")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (
        include("This link has expired") and
          include("Continue to your HMRC online account") and
          include("request a new verification link")
      )

      Jsoup.parse(body).getElementById("link-to-home").toString() must include("/account")
      wireMockServer.verify(1, putRequestedFor(urlEqualTo("/preferences/email")))
    }

    "display already verified message if the email has been verified already" in {
      stubForPreferencesPutEmail(OK, "already_verified")

      val token = UUID.randomUUID().toString

      val request = FakeRequest(GET, s"/sa/print-preferences/verification/$token")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(BAD_REQUEST)

      val body = contentAsString(response)
      body must
        (include("Email address already verified") and
          include("You will start getting emails letting you know about your online tax letters.") and
          include("Continue to your HMRC online account"))

      Jsoup.parse(body).getElementById("link-to-home").toString() must include("/account")
      wireMockServer.verify(1, putRequestedFor(urlEqualTo("/preferences/email")))
    }

    "display expired old email address message if verification link is not valid due to opt out" in {
      stubForPreferencesPutEmail(CONFLICT, "error")

      val token = UUID.randomUUID().toString

      val request = FakeRequest(GET, s"/sa/print-preferences/verification/$token")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (
        include("You&#x27;ve used a link that has now expired") and
          include("It may have been sent to an old or alternative email address.") and
          include("Please use the link in the latest verification email sent to your specified email address.")
      )
      wireMockServer.verify(1, putRequestedFor(urlEqualTo("/preferences/email")))
    }
  }
}
