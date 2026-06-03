/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.crypto.{ Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory }
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.{ ApplicationCrypto, ApplicationCryptoProvider, CryptoImplicits }
import uk.gov.hmrc.emailaddress.EmailAddress

import java.net.{ URLDecoder, URLEncoder }

case class Encrypted[T](decryptedValue: T)
object Encrypted {
  val applicationCrypto = ApplicationCryptoProvider(Configuration.apply(ConfigFactory.load())).get()

  lazy val secondaryCrypto: Encrypter & Decrypter =
    SymmetricCryptoFactory.aesCryptoFromConfig(
      baseConfigKey = "queryParams.encryption.secondary",
      ConfigFactory.load()
    )

  implicit def encryptedStringToDecryptedEmail(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[Encrypted[EmailAddress]] =
    new EncryptedQueryBinder[EmailAddress](
      applicationCrypto.QueryParameterCrypto,
      secondaryCrypto,
      EmailAddress.apply,
      _.value
    )

  implicit def encryptedStringToDecryptedString(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[Encrypted[String]] =
    new EncryptedQueryBinder[String](
      applicationCrypto.QueryParameterCrypto,
      secondaryCrypto,
      s => s,
      s => s
    )
}

private[model] class EncryptedQueryBinder[T](
  primaryCrypto: Encrypter & Decrypter,
  secondaryCrypto: Encrypter & Decrypter,
  fromString: String => T,
  toString: T => String
)(implicit stringBinder: QueryStringBindable[String])
    extends QueryStringBindable[Encrypted[T]] {
  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Encrypted[T]]] =
    stringBinder.bind(key, params).map {
      case Right(encryptedString) =>
        def tryDecrypt(crypto: Encrypter & Decrypter): Either[String, Encrypted[T]] =
          try {
            val decrypted = crypto.decrypt(Crypted(encryptedString))
            try Right(Encrypted(fromString(decrypted.value)))
            catch {
              case _: IllegalArgumentException => Left(s"$key is not valid")
            }
          } catch {
            case _: Exception => Left(s"Could not decrypt value for $key")
          }

        // Try first key, then fallback if needed
        tryDecrypt(primaryCrypto) match {
          case Right(res) => Right(res)
          case Left(_)    => tryDecrypt(secondaryCrypto)
        }
      case Left(f) => Left(f)
    }

  override def unbind(key: String, enc: Encrypted[T]): String =
    stringBinder.unbind(key, primaryCrypto.encrypt(PlainText(toString(enc.decryptedValue))).value)
}

object EntityIdCrypto {
  lazy val currentCrypto: Encrypter & Decrypter =
    SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = "entityId.encryption", ConfigFactory.load())

  def encryptEntityId(entityId: String): Option[String] =
    try Some(encryptAndEncodeString(entityId))
    catch {
      case e: Throwable =>
        LoggerFactory.getLogger("EntityIdCrypto").warn(s"Unable to encrypt $entityId : ${e.getMessage}")
        None
    }

  private def encryptAndEncodeString(s: String): String =
    URLEncoder.encode(currentCrypto.encrypt(PlainText(s)).value, "UTF-8")

  private def decodeAndDecryptString(s: String): String =
    currentCrypto.decrypt(Crypted(URLDecoder.decode(s, "UTF-8"))).value

  def decryptEntityId(entityId: String): Option[String] =
    try Some(decodeAndDecryptString(entityId))
    catch {
      case e: Throwable =>
        LoggerFactory.getLogger("EntityIdCrypto").warn(s"Unable to decrypt $entityId : ${e.getMessage}")
        None
    }
}
