/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.filing

import java.time.Instant
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.crypto.{ Crypted, PlainText }
import uk.gov.hmrc.domain.SaUtr

import java.net.{ URLDecoder, URLEncoder }
import java.time.temporal.ChronoUnit

class TokenEncryptionSpec extends PlaySpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "sso.encryption.key"          -> "P5xsJ9Nt+quxGZzB4DeLfw==",
        "sso.encryption.previousKeys" -> Seq.empty
      )
      .build()
  val crypto: TokenEncryption = app.injector.instanceOf[TokenEncryption]
  "Token decryption" should {
    "decrypt a valid token" in {
      val validToken = s"utr:${Instant.now().toEpochMilli}"
      val encryptedToken = URLEncoder.encode(crypto.crypto.encrypt(PlainText(validToken)).value, "UTF-8")

      crypto.decryptToken(encryptedToken, 5).utr must be(SaUtr("utr"))
    }

    "decrypt a valid unencoded token" in {
      val validToken = s"cjsajjdajdas:${Instant.now().toEpochMilli}"
      val encryptedToken = crypto.crypto.encrypt(PlainText(validToken))

      crypto.decryptToken(encryptedToken.value, 5).utr must be(SaUtr("cjsajjdajdas"))
    }

    "decrypt token with slashes and plus chars" in {
      val encoded = "vK%2Bps%2FoV3CYFc0fgzd1ZiBIUu%2FQ%2FVmAeDNcUkgRs%2BTE%3D"
      val token = URLDecoder.decode(encoded, "UTF-8")
      crypto.crypto.decrypt(Crypted(token)).value mustBe "cjsajjdajdas:1379068252455"
    }

    "fail with expired token" in {
      val expiredToken = s"utr:${Instant.now().minus(6, ChronoUnit.MINUTES).toEpochMilli}"
      val encryptedToken = URLEncoder.encode(crypto.crypto.encrypt(PlainText(expiredToken)).value, "UTF-8")

      intercept[TokenExpiredException] {
        crypto.decryptToken(encryptedToken, 5)
      }
    }

    "fail with corrupted token" in {
      intercept[SecurityException] {
        crypto.decryptToken("invalid", 5)
      }
    }
  }

}
