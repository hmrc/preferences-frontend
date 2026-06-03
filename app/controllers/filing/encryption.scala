/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.filing

import java.time.Instant
import play.api.{ Configuration, Logger }
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.domain.SaUtr

import java.net.URLDecoder
import java.time.temporal.ChronoUnit
import javax.inject.{ Inject, Singleton }

private[filing] case class TokenExpiredException(token: String, time: Long)
    extends Exception(
      s"Token expired: $token. Timestamp: $time, Now: ${Instant.now().toEpochMilli}"
    )

private[filing] case class Token(utr: SaUtr, timestamp: Long, encryptedToken: String)

@Singleton
private[filing] class TokenEncryption @Inject() (config: Configuration) extends KeysFromConfig {
  override def configuration: Configuration = config

  val base64 = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$"

  val crypto: Encrypter & Decrypter = SymmetricCryptoFactory.composeCrypto(currentCrypto, previousCryptos)

  def decryptToken(encryptedToken: String, timeout: Long): Token = {
    val tokenAsString =
      if (encryptedToken.matches(base64)) crypto.decrypt(Crypted(encryptedToken))
      else crypto.decrypt(Crypted(URLDecoder.decode(encryptedToken, "UTF-8")))

    val (utr, time) = tokenAsString.value.split(":") match {
      case Array(u, t) => (u.trim, t.trim.toLong)
      case _           => throw new IllegalArgumentException("Could not parse decrypted token")
    }
    if (currentTime.minus(timeout, ChronoUnit.MINUTES).isAfter(Instant.ofEpochMilli(time)))
      throw TokenExpiredException(encryptedToken, time)
    else Token(SaUtr(utr.trim), time, encryptedToken)
  }

  def currentTime: Instant = Instant.now()
}

trait KeysFromConfig {

  val baseConfigKey: String = "sso.encryption"

  def configuration: Configuration

  protected val currentCrypto: AesCrypto = {
    val configKey = baseConfigKey + ".key"
    val logger: Logger = Logger(this.getClass)
    val currentEncryptionKey = configuration.getOptional[String](configKey).getOrElse {
      logger.error(s"Missing required 1 configuration entry: $configKey")
      throw new SecurityException(s"Missing required 2 configuration entry: $configKey")
    }
    aesCrypto(currentEncryptionKey)
  }

  protected val previousCryptos: Seq[AesCrypto] = {
    val configKey = baseConfigKey + ".previousKeys"
    val previousEncryptionKeys = configuration.getOptional[Seq[String]](configKey).getOrElse(Seq.empty)
    previousEncryptionKeys.map(aesCrypto)
  }

  private def aesCrypto(key: String) =
    try {
      val crypto = new AesCrypto {
        override val encryptionKey: String = key
      }
      crypto.decrypt(crypto.encrypt(PlainText("assert-valid-key")))
      crypto
    } catch {
      case e: Exception =>
        val logger: Logger = Logger(this.getClass)
        logger.error(s"Invalid encryption key: $key", e); throw new SecurityException("Invalid encryption key", e)
    }
}
