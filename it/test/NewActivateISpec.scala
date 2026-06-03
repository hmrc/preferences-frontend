/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

import org.jsoup.Jsoup
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.libs.json
import play.api.test.FakeRequest
import play.api.mvc.Cookie
import stubs.{ WireMockStubs, WireMockUtil }
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto

import java.net.URLEncoder
import java.util.UUID

class NewActivateGraceOutISpec
    extends PlaySpec with TestUser with GuiceOneServerPerSuite with ScalaFutures with BeforeAndAfterAll
    with IntegrationPatience with WireMockUtil with WireMockStubs {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "controllers.controllers.internal.ActivationController.needsAuth" -> true,
        "activation.gracePeriodInMin"                                     -> 0,
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

  "activate with grace period already passed" should {

    "return PRECONDITION_FAILED for existing PTA Customer who had previously opted out and has no email held in GetPreferences" in new Fixture {

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkOptedOut()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get
      val json = contentAsJson(result)

      status(result) must be(PRECONDITION_FAILED)
      withClue("no survey in the redirectUserTo") {
        (json \ "redirectUserTo").as[String] must be(
          s"http://localhost:9024/paperless/choose?returnUrl=$returnUrl&returnLinkText=$text&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"
        )
      }
    }

    "return PRECONDITION_FAILED for existing PTA Customer who had previously opted out and has no email held in GetPreferences with survey" in new Fixture {

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkOptedOutWithSurvey()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      val json = contentAsJson(result)
      status(result) must be(PRECONDITION_FAILED)

      withClue("survey should not be requested in the redirectUserTo") {
        (json \ "redirectUserTo").as[String] must be(
          s"http://localhost:9024/paperless/choose?returnUrl=$returnUrl&returnLinkText=$text"
        )
      }
    }

    "return OK for existing Opted-in customer with unverified email" in new Fixture {

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOk()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(OK)
    }

    "return OK for Existing Opted-in customer with verified email" in new Fixture {

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkVerified()
      stubForPreferencesEmailLanguage
      stubForPreferencesPutEmail(OK, "success")

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(OK)

      val verificationToken = UUID.randomUUID().toString
      val verificationRequest = FakeRequest(GET, s"/sa/print-preferences/verification/$verificationToken")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val verificationResponse = route(app, verificationRequest).get

      status(verificationResponse) must be(OK)
      val verificationBody = contentAsString(verificationResponse)

      verificationBody must (
        include("Email address verified") and include(
          "To read your online tax letters, sign in to HMRC services and select &#x27;Messages&#x27;."
        ) and
          include("Continue to your HMRC online account")
      )

      Jsoup.parse(verificationBody).getElementById("link-to-home").toString() must include("/account")
    }

    "set language preference based on cookie language value for Existing Opted-in customer which has no existing language preference" in new Fixture {

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text&language=cy"
      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkVerified()
      stubForPreferencesEmailLanguage
      stubForPreferencesPutEmail(OK, "success")

      val request = FakeRequest(PUT, url)
        .withSession(SessionKeys.authToken -> "Bearer testToken")
        .withCookies(Cookie("PLAY_LANG", "cy"))
      val result = route(app, request).get

      status(result) must be(OK)

      val verificationToken = UUID.randomUUID().toString
      val verificationRequest = FakeRequest(GET, s"/sa/print-preferences/verification/$verificationToken")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val verificationResponse = route(app, verificationRequest).get

      status(verificationResponse) must be(OK)
      val verificationBody = contentAsString(verificationResponse)

      verificationBody must (
        include("Email address verified") and include(
          "To read your online tax letters, sign in to HMRC services and select &#x27;Messages&#x27;."
        ) and
          include("Continue to your HMRC online account")
      )

      Jsoup.parse(verificationBody).getElementById("link-to-home").toString() must include("/account")

    }

    "not silently overwrite a language preference based on cookie value for Existing Opted-in customer which has an existing language preference" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      val email = s"${UUID.randomUUID().toString}@email.com"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkVerified()
      stubForPreferencesEmailLanguage
      stubForPreferencesPutEmail(OK, "success")

      val requestEnglish = FakeRequest(PUT, url)
        .withSession(SessionKeys.authToken -> "Bearer testToken")
        .withCookies(Cookie("PLAY_LANG", "en"))
      val resultEnglish = route(app, requestEnglish).get
      status(resultEnglish) must be(OK)

      val verificationToken1 = UUID.randomUUID().toString
      val verificationRequest1 = FakeRequest(GET, s"/sa/print-preferences/verification/$verificationToken1")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val verificationResponse1 = route(app, verificationRequest1).get

      status(verificationResponse1) must be(OK)
      val verificationBody1 = contentAsString(verificationResponse1)

      verificationBody1 must (
        include("Email address verified") and include(
          "To read your online tax letters, sign in to HMRC services and select &#x27;Messages&#x27;."
        ) and
          include("Continue to your HMRC online account")
      )

      Jsoup.parse(verificationBody1).getElementById("link-to-home").toString() must include("/account")

      wireMockServer.resetMappings()
      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkVerifiedWithLanguage("en")
      stubForPreferencesEmailLanguage
      stubForPreferencesPutEmail(OK, "success")

      val requestWelsh = FakeRequest(PUT, url)
        .withSession(SessionKeys.authToken -> "Bearer testToken")
        .withCookies(Cookie("PLAY_LANG", "cy"))
      val resultWelsh = route(app, requestWelsh).get
      status(resultWelsh) must be(OK)

      val verificationToken2 = UUID.randomUUID().toString
      val verificationRequest2 = FakeRequest(GET, s"/sa/print-preferences/verification/$verificationToken2")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val verificationResponse2 = route(app, verificationRequest2).get

      status(verificationResponse2) must be(OK)
      val verificationBody2 = contentAsString(verificationResponse2)

      verificationBody2 must (
        include("Email address verified") and include(
          "To read your online tax letters, sign in to HMRC services and select &#x27;Messages&#x27;."
        ) and
          include("Continue to your HMRC online account")
      )

      Jsoup.parse(verificationBody2).getElementById("link-to-home").toString() must include("/account")
    }

    "return PRECONDITION_FAILED for Existing Opted-out customer who was previously Opted-in with verified email" in new Fixture {

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkOptedOutAfterVerification()
      stubForPreferencesEmailLanguage
      stubForPreferencesPutEmail(OK, "success")

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(PRECONDITION_FAILED)

      val verificationToken = UUID.randomUUID().toString
      val verificationRequest = FakeRequest(GET, s"/sa/print-preferences/verification/$verificationToken")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val verificationResponse = route(app, verificationRequest).get

      status(verificationResponse) must be(OK)
      val verificationBody = contentAsString(verificationResponse)

      verificationBody must (
        include("Email address verified") and include(
          "To read your online tax letters, sign in to HMRC services and select &#x27;Messages&#x27;."
        ) and
          include("Continue to your HMRC online account")
      )

      Jsoup.parse(verificationBody).getElementById("link-to-home").toString() must include("/account")

    }

    "return PRECONDITION_FAILED for Existing Opted-out customer who was previously Opted-in with unverified email" in new Fixture {

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkOptedOut()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(PRECONDITION_FAILED)
    }
  }
}

class NewActivateGraceInISpec
    extends PlaySpec with TestUser with GuiceOneServerPerSuite with ScalaFutures with BeforeAndAfterAll
    with IntegrationPatience with WireMockUtil with WireMockStubs {
  spec =>

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "controllers.controllers.internal.ActivationController.needsAuth" -> true,
        "play.http.router"                                                -> "legacy.Routes",
        "Test.activation.gracePeriodInMin"                                -> 10,
        "metrics.enabled"                                                 -> false,
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

  "activate within grace period" should {

    "return OK for Existing Opted-out customer who was previously Opted-in with verified email" in new Fixture {

      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      val email = s"${UUID.randomUUID().toString}@email.com"
      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkOptedOutAfterVerificationWithinGracePeriod()
      stubForPreferencesEmailLanguage
      stubForPreferencesPutEmail(OK, "success")

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(OK)

      val verificationToken = UUID.randomUUID().toString
      val verificationRequest = FakeRequest(GET, s"/sa/print-preferences/verification/$verificationToken")
        .withSession(SessionKeys.authToken -> "Bearer testToken")
      val verificationResponse = route(app, verificationRequest).get

      status(verificationResponse) must be(OK)
      val verificationBody = contentAsString(verificationResponse)

      verificationBody must (
        include("Email address verified") and include(
          "To read your online tax letters, sign in to HMRC services and select &#x27;Messages&#x27;."
        ) and
          include("Continue to your HMRC online account")
      )

      Jsoup.parse(verificationBody).getElementById("link-to-home").toString() must include("/account")
    }

    "return OK for Existing Opted-out customer who was previously Opted-in with unverified email" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatusOkOptedOutUnverifiedWithinGracePeriod()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      status(result) must be(OK)
    }
  }
}

class NewActivateNewUserISpec
    extends PlaySpec with TestUser with GuiceOneServerPerSuite with ScalaFutures with BeforeAndAfterAll
    with IntegrationPatience with WireMockUtil with WireMockStubs {
  spec =>

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "controllers.controllers.internal.ActivationController.needsAuth" -> true,
        "activation.gracePeriodInMin"                                     -> 0,
        "play.http.router"                                                -> "legacy.Routes",
        "metrics.enabled"                                                 -> false,
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

  "activate new customer" should {
    "survey should not be in the redirectUserTo" in new Fixture {
      val (returnUrl, text) = buildReturnUrlAndLink
      val url = s"/paperless/activate?returnUrl=$returnUrl&returnLinkText=$text"

      buildAuthStub(withUtr = Some(utr))
      stubForPreferencesStatus()
      stubForPreferencesEmailLanguage

      val request = FakeRequest(PUT, url).withSession(SessionKeys.authToken -> "Bearer testToken")
      val result = route(app, request).get

      val json = contentAsJson(result)
      status(result) must be(PRECONDITION_FAILED)

      withClue("new user survey in the redirectUserTo") {
        (json \ "redirectUserTo").as[String] must be(
          s"http://localhost:9024/paperless/choose?returnUrl=$returnUrl&returnLinkText=$text&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"
        )
      }
    }
  }
}
