/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

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
import util.DateTimeUtils

import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.time.{ LocalDate, ZoneOffset }
import java.util.UUID

class ManagePaperlessPartialISpec
    extends PlaySpec with TestUser with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience
    with WireMockUtil with WireMockStubs {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "play.http.router"         -> "legacy.Routes",
        "metrics.enabled"          -> false,
        "auditing.enabled"         -> false,
        "metrics.graphite.enabled" -> false,
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter"),
        "microservice.services.auth.port"        -> wireMockServer.port(),
        "microservice.services.preferences.port" -> wireMockServer.port()
      )
      .build()

  trait Fixture {
    self =>

    private val longDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

    val todaysDate = LocalDate.ofInstant(DateTimeUtils.now, ZoneOffset.UTC)
    val todaysDateISO = DateTimeFormatter.ISO_LOCAL_DATE.format(todaysDate)
    val todaysDateLong = longDateFormatter.format(todaysDate)

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
  }

  "Manage Paperless partial" should {

    "return not authorised when no credentials supplied" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url)
      val response = route(app, request).get

      status(response) must be(UNAUTHORIZED)
    }

    "return opted out details when no preference is set" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withNino = Some(nino))

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (
        include("Sign up for paperless notifications") and
          not include "You need to verify"
      )
    }
  }

  "Manage Paperless partial for pending verification" should {

    "contain pending email verification details" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withNino = Some(nino))

      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedIn.json", Map("email" -> email))

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      contentAsString(response) must include(s"You need to verify")
    }

    "contain new email details for a subsequent change email" in new Fixture {
      buildAuthStub(withUtr = Some(utr))

      val newEmail = s"${UUID.randomUUID().toString}@email.com"

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      stubWithResponse(
        stubForPreferencesWithResponse,
        "PreferenceResponsePendingEmail.json",
        Map("pendingEmail" -> newEmail, "linkSent" -> todaysDateISO)
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (include(s"You need to verify your email address.") and
        include(newEmail) and
        include(s"on $todaysDateLong. Click on the link in the email to verify your email address."))
    }

    "contain sign up details for a subsequent opt out" in new Fixture {
      buildAuthStub(withUtr = Some(utr))
      val email = s"${UUID.randomUUID().toString}@email.com"

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedOut.json", Map())

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (
        not include email and
          include(s"Sign up for paperless notifications")
      )
    }
  }

  "Manage Paperless partial for verified user" should {

    "contain new email details for a subsequent change email" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(
        stubForPreferencesWithResponse,
        "PreferenceResponsePendingEmail.json",
        Map("pendingEmail" -> email, "linkSent" -> todaysDateISO)
      )

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (include(s"You need to verify your email address.") and
        include(email) and
        include(s"on $todaysDateLong. Click on the link in the email to verify your email address."))
    }

    "contain sign up details for a subsequent opt out" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withNino = Some(nino))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedOut.json")

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (not include email and
        include(s"Sign up for paperless notifications"))
    }
  }

  "Manage Paperless partial for a bounced verification email" should {

    "contain new email details for a subsequent change email" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(
        stubForPreferencesWithResponse,
        "PreferenceResponsePendingEmail.json",
        Map("pendingEmail" -> email, "linkSent" -> todaysDateISO)
      )

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (include(s"You need to verify your email address.") and
        include(email) and
        include(s"on $todaysDateLong. Click on the link in the email to verify your email address."))
    }

    "contain sign up details for a subsequent opt out" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))
      stubWithResponse(stubForPreferencesWithResponse, "PreferenceResponseOptedOut.json")

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/manage?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (not include email and
        include(s"Sign up for paperless notifications"))
    }
  }
}
