/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import play.api.test.FakeRequest
import stubs.{ WireMockStubs, WireMockUtil }
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto
import util.DateTimeUtils
import utils.FileLoader

import java.net.URLEncoder
import java.time.{ LocalDate, ZoneOffset }
import java.time.format.DateTimeFormatter
import java.util.UUID

class CheckSettingsISpec
    extends PlaySpec with TestUser with GuiceOneServerPerSuite with ScalaFutures with BeforeAndAfterAll
    with IntegrationPatience with WireMockUtil with WireMockStubs {
  spec =>

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "controllers.controllers.internal.ActivationController.needsAuth" -> true,
        "play.http.router"                                                -> "legacy.Routes",
        "metrics.enabled"                                                 -> false,
        "auditing.enabled"                                                -> false,
        "metrics.graphite.enabled"                                        -> false,
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter"),
        "microservice.services.auth.port"        -> wireMockServer.port(),
        "microservice.services.preferences.port" -> wireMockServer.port()
      )
      .build()

  trait Fixture {
    self =>

    val todaysDate = LocalDate.ofInstant(DateTimeUtils.now, ZoneOffset.UTC)
    val todaysDateISO = DateTimeFormatter.ISO_LOCAL_DATE.format(todaysDate)

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

  "Check Settings" should {
    "return not authorised when no credentials supplied" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url)
      val result = route(app, request).get

      status(result) must be(UNAUTHORIZED)
    }

    "return rendered digital_false_full" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withNino = Some(nino))

      val stubResponse = FileLoader.read("PreferenceResponseOptedOut.json")
      stubForPreferencesWithResponse(stubResponse)

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(OK)
      val body = contentAsString(result)
      val document = Jsoup.parse(body)

      document.getElementById("saCheckSettings").text() mustBe "Check your settings"
      document
        .getElementsByClass("govuk-summary-list__actions")
        .first()
        .getElementsByTag("a")
        .first()
        .attr("href") must include("/paperless/choose")
    }
  }

  "Check Settings for pending verification" should {

    "contain pending email verification details" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withNino = Some(nino))

      val stubResponse = FileLoader.readAndSubstitute("PreferenceResponsePaperlessEmail.json", Map("email" -> email))
      stubForPreferencesWithResponse(stubResponse)

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(OK)
      val body = contentAsString(result)
      body must include("UNVERIFIED")
      body must include(email)
      body must include("Fix this")
    }

    "contain new email details for a subsequent change email" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))

      val stubResponse =
        FileLoader.readAndSubstitute(
          "PreferenceResponsePendingEmail.json",
          Map("pendingEmail" -> newEmail, "linkSent" -> todaysDateISO)
        )
      stubForPreferencesWithResponse(stubResponse)

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(OK)

      val body = contentAsString(result)
      body must (include("UNVERIFIED") and
        include(newEmail) and
        not include email and
        include("Fix this"))
    }

    "contain sign up details for a subsequent opt out" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      val email = s"${UUID.randomUUID().toString}@email.com"

      val stubResponse = FileLoader.read("PreferenceResponseOptedOut.json")
      stubForPreferencesWithResponse(stubResponse)

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (
        not include email
          and
          include(s"Post")
      )
    }
  }

  "Check settings for verified user" should {

    "contain tax document message and verified email address" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      val stubResponse =
        FileLoader.readAndSubstitute("PreferenceResponsePaperlessVerifiedEmail.json", Map("email" -> email))
      stubForPreferencesWithResponse(stubResponse)

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must include(s"Online messages, or post when not available")
      body must include(email)
    }

    "contain new email details for a subsequent change email" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))

      val stubResponse = FileLoader.readAndSubstitute("PreferenceResponsePaperlessEmail.json", Map("email" -> newEmail))
      stubForPreferencesWithResponse(stubResponse)

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (include("UNVERIFIED") and
        include(newEmail) and
        not include email and
        include("Fix this"))
    }

    "contain sign up details for a subsequent opt out" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withNino = Some(nino))

      val stubResponse = FileLoader.read("PreferenceResponseOptedOut.json")
      stubForPreferencesWithResponse(stubResponse)

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (
        not include email
          and
          include(s"Post")
      )
    }
  }

  "Check settings for a bounced email" should {

    "contain warning message and email address" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      val stubResponse = FileLoader.readAndSubstitute("PreferenceResponsePaperlessBounced.json", Map("email" -> email))
      stubForPreferencesWithResponse(stubResponse)

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must include("FAILING")
      body must include(s"Fix this")
      body must include(email)
    }

    "contain new email details for a subsequent change email" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      val stubResponse =
        FileLoader.readAndSubstitute(
          "PreferenceResponsePendingEmail.json",
          Map("pendingEmail" -> email, "linkSent" -> todaysDateISO)
        )
      stubForPreferencesWithResponse(stubResponse)

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (include("UNVERIFIED") and
        include(email) and
        include("Fix this"))
    }

    "contain sign up details for a subsequent opt out" in new Fixture {
      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/check-settings?returnUrl=$returnUrl&returnLinkText=$text"

      val stubResponse = FileLoader.read("PreferenceResponseOptedOut.json")
      stubForPreferencesWithResponse(stubResponse)

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) must be(OK)
      val body = contentAsString(response)
      body must (
        not include email
          and
          include(s"Post")
      )
    }
  }
}
