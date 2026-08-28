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

class PaperlessStatusControllerISpec
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

  "/paperless/status" should {
    "return 401 when no auth is found" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/status?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url)
      val response = route(app, request).get
      status(response) mustBe UNAUTHORIZED
    }

    "return 401 when a request doesn't meet the correct confidence level" in new Fixture {
      buildAuthStub(withUtr = Some(utr), confidenceLevel = 50)

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/status?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) mustBe OK
    }

    "return 401 when a request doesn't contain the correct affinityGroup type" in new Fixture {
      stubForUnauthorised

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/status?returnUrl=$returnUrl&returnLinkText=$text"

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) mustBe UNAUTHORIZED
    }

    "return 200 when a request is authenticated" in new Fixture {
      buildAuthStub(withUtr = Some(utr))

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/status?returnUrl=$returnUrl&returnLinkText=$text"

      stubForPreferencesNotFound

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val response = route(app, request).get

      status(response) mustBe OK
    }

  }
}
