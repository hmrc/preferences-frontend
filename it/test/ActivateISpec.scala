/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import stubs.{ WireMockStubs, WireMockUtil }
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto

import java.net.URLEncoder

class ActivateISpec
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
      val returnUrl = "/test/return/url"
      val returnLinkText = "Continue"

      (
        encryptAndEncode(ac, returnUrl),
        encryptAndEncode(ac, returnLinkText)
      )
    }
  }

  "activate" should {

    "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with utr only" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatus()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      val json = contentAsJson(result)

      status(result) must be(PRECONDITION_FAILED)

      (json \ "redirectUserTo").as[String] must be(
        s"http://localhost:9024/paperless/choose?returnUrl=$returnUrl&returnLinkText=$text&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"
      )
      (json \ "optedIn").asOpt[Boolean] mustBe empty
      (json \ "verifiedEmail").asOpt[Boolean] mustBe empty
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with given utr and nino" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr), withNino = Some(nino))
      stubForPreferencesStatus()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      val json = contentAsJson(result)

      status(result) must be(PRECONDITION_FAILED)

      (json \ "redirectUserTo").as[String] must be(
        s"http://localhost:9024/paperless/choose?returnUrl=$returnUrl&returnLinkText=$text&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"
      )
      (json \ "optedIn").asOpt[Boolean] mustBe empty
      (json \ "verifiedEmail").asOpt[Boolean] mustBe empty
    }

    "return UNAUTHORIZED if activating for a user with no nino or utr" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      stubForUnauthorised

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(UNAUTHORIZED)
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with nino only" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withNino = Some(nino))
      stubForPreferencesStatus()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      val json = contentAsJson(result)

      status(result) must be(PRECONDITION_FAILED)

      (json \ "redirectUserTo").as[String] must be(
        s"http://localhost:9024/paperless/choose?returnUrl=$returnUrl&returnLinkText=$text&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"
      )
      (json \ "optedIn").asOpt[Boolean] mustBe empty
      (json \ "verifiedEmail").asOpt[Boolean] mustBe empty
    }

    "return OK with the optedIn attribute set to true and verifiedEmail set to false if the user has opted in and not verified" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOk()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      val json = contentAsJson(result)
      status(result) must be(OK)

      (json \ "optedIn").as[Boolean] mustBe true
      (json \ "verifiedEmail").as[Boolean] mustBe false
      (json \ "redirectUserTo").asOpt[String] mustBe empty
    }

    "return OK with the optedIn attribute set to true and verifiedEmail set to true if the user has opted in and verified" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkVerified()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      val json = contentAsJson(result)
      status(result) must be(OK)

      (json \ "optedIn").as[Boolean] mustBe true
      (json \ "verifiedEmail").as[Boolean] mustBe true
      (json \ "redirectUserTo").asOpt[String] mustBe empty
    }

    "return OK with the optedId attribute set to false if the user has opted out" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkOptedOut()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      val json = contentAsJson(result)
      status(result) must be(OK)

      (json \ "optedIn").as[Boolean] mustBe false
      (json \ "verifiedEmail").asOpt[Boolean] mustBe empty
      (json \ "redirectUserTo").asOpt[String] mustBe empty
    }

    "return CONFLICT if trying to activate providing an email different than the stored one" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      private val encEmail = encryptAndEncode(ac, "generic2@test.com") // Emails mismatch

      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text" +
        s"&termsAndConditions=generic2&email=$encEmail"

      buildAuthStub(withNino = Some(nino))
      stubForPreferencesStatusNotFound()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get
      status(result) must be(CONFLICT)
    }
  }

}
