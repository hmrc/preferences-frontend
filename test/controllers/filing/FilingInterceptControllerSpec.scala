/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.filing

import connectors.PreferencesConnector
import model.Encrypted

import java.time.Instant
import org.mockito.ArgumentMatchers.{ eq => meq, _ }
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, OptionValues }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ AnyContent, Request }
import play.api.test.FakeRequest
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import java.net.URLEncoder.{ encode => urlEncode }
import java.time.temporal.ChronoUnit
import scala.concurrent.{ ExecutionContext, Future }

class FilingInterceptControllerSpec
    extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ScalaFutures with OptionValues
    with GuiceOneAppPerSuite {

  import play.api.test.Helpers._

  val mockPreferencesConnector = mock[PreferencesConnector]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "sso.encryption.key"          -> "P5xsJ9Nt+quxGZzB4DeLfw==",
        "sso.encryption.previousKeys" -> Seq.empty
      )
      .configure("metrics.enabled" -> false)
      .overrides(bind[PreferencesConnector].toInstance(mockPreferencesConnector))
      .build()

  val crypto = app.injector.instanceOf[TokenEncryption]
  val controller = app.injector.instanceOf[FilingInterceptController]

  override def beforeEach() = reset(mockPreferencesConnector)

  "Preferences pages" should {
    "redirect to the portal when no preference exists for a specific utr" in new TestCase {
      when(mockPreferencesConnector.getEmailAddress(meq(validUtr))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrlRedirectUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrl)
      verify(mockPreferencesConnector, times(1))
        .getEmailAddress(meq(validUtr))(any[HeaderCarrier], any[ExecutionContext])
    }

    "redirect to the portal when a preference for email already exists for a specific utr" in new TestCase {
      when(mockPreferencesConnector.getEmailAddress(meq(validUtr))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(emailAddress)))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrlRedirectUrl, None)(FakeRequest())
      status(page) shouldBe 303
      private val value = header("Location", page).value
      value should be(decodedReturnUrlWithEmailAddress)
      verify(mockPreferencesConnector, times(1))
        .getEmailAddress(meq(validUtr))(any[HeaderCarrier], any[ExecutionContext])
    }

    "redirect to the portal when a preference for paper already exists for a specific utr" in new TestCase {
      when(mockPreferencesConnector.getEmailAddress(meq(validUtr))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrlRedirectUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrl)
      verify(mockPreferencesConnector, times(1))
        .getEmailAddress(meq(validUtr))(any[HeaderCarrier], any[ExecutionContext])
    }

    "redirect to the portal when preferences already exist for a specific utr and an email address was passed to the platform" in new TestCase {
      when(mockPreferencesConnector.getEmailAddress(meq(validUtr))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(emailAddress)))

      val page = controller.redirectWithEmailAddress(
        validToken,
        encodedReturnUrlRedirectUrl,
        Some(Encrypted(EmailAddress("other@me.com")))
      )(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrlWithEmailAddress)
    }

    "redirect to portal if the token is expired on the landing page" in new TestCase {
      val page = controller.redirectWithEmailAddress(expiredToken, encodedReturnUrlRedirectUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "redirect to portal if the token is not valid on the landing page" in new TestCase {
      val page = controller.redirectWithEmailAddress(incorrectToken, encodedReturnUrlRedirectUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "return bad request if redirect_url is not in the allowlist" in new TestCase {

      val page =
        controller.redirectWithEmailAddress(validToken, encodedUrlNotOnAllowlistRedirectUrl, None)(FakeRequest())
      status(page) shouldBe 400
    }
  }

  trait TestCase {
    //   val crypto = CryptoWithKeysFromConfig(baseConfigKey = "sso.encryption")
    val emailAddress = "foo@bar.com"
    val validUtr = SaUtr("1234567")
    lazy val validToken =
      urlEncode(
        crypto.crypto.encrypt(PlainText(s"$validUtr:${Instant.now().toEpochMilli}")).value,
        "UTF-8"
      )
    lazy val expiredToken = urlEncode(
      crypto.crypto
        .encrypt(PlainText(s"$validUtr:${Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli}"))
        .value,
      "UTF-8"
    )
    lazy val incorrectToken = "this is an incorrect token khdskjfhasduiy3784y37yriuuiyr3i7rurkfdsfhjkdskh"
    val decodedReturnUrl = "http://localhost:8080/portal?exampleQuery=exampleValue"
    val encodedReturnUrl = urlEncode(decodedReturnUrl, "UTF-8")
    lazy val decodedReturnUrlWithEmailAddress =
      s"$decodedReturnUrl&email=${urlEncode(crypto.crypto.encrypt(PlainText(emailAddress)).value, "UTF-8")}"
    val encodedUrlNotOnAllowlist = urlEncode("http://notOnAllowlist/something", "UTF-8")

    val encodedReturnUrlRedirectUrl: RedirectUrl = RedirectUrl(urlEncode(decodedReturnUrl, "UTF-8"))
    val encodedUrlNotOnAllowlistRedirectUrl: RedirectUrl = RedirectUrl(
      urlEncode("http://notOnAllowlist/something", "UTF-8")
    )

    val request = FakeRequest()

    implicit def hc: HeaderCarrier = any()

    def request(
      optIn: Option[Boolean],
      mainEmail: Option[String] = None,
      mainEmailConfirmation: Option[String] = None
    ): Request[AnyContent] = {

      val params = (
        Seq(mainEmail.map { v =>
          "email.main" -> v
        })
          ++ Seq(mainEmailConfirmation.map { v =>
            ("email.confirm", v)
          })
          ++ Seq(optIn.map { v =>
            ("opt-in", v.toString)
          })
      ).flatten

      FakeRequest().withFormUrlEncodedBody(params*)

    }

  }

}
