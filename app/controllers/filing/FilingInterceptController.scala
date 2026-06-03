/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package controllers.filing

import connectors.PreferencesConnector
import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.config.UriConfig
import io.lemonlabs.uri.encoding._
import io.lemonlabs.uri.typesafe.dsl._
import model.Encrypted
import play.api.mvc._
import play.api.{ Configuration, Logger }
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{ AbsoluteWithHostnameFromAllowlist, RedirectUrl }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.net.URLDecoder
import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ ExecutionContext, Future }

class FilingInterceptController @Inject() (
  preferencesConnector: PreferencesConnector,
  configuration: Configuration,
  tokenEncryption: TokenEncryption,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  lazy val redirectDomainAllowlist = configuration
    .getOptional[Seq[String]](s"portal.redirectDomainAllowlist")
    .getOrElse(List())
    .toSet
  implicit val wl: Set[String] = redirectDomainAllowlist
  implicit val config: UriConfig = UriConfig(encoder = percentEncode)
  implicit def uriToString(uri: Uri): String = uri.toString()
  val logger: Logger = Logger(getClass.getName)

  def redirectWithEmailAddress(
    encryptedToken: String,
    redirectUrl: RedirectUrl,
    @unused emailAddressToPrefill: Option[Encrypted[EmailAddress]]
  ): Action[AnyContent] =
    Action.async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val decodeReturnUrl = RedirectUrl(URLDecoder.decode(redirectUrl.unsafeValue, "UTF-8"))
      val redirectUrlPolicy = AbsoluteWithHostnameFromAllowlist(wl.toSeq*)

      decodeReturnUrl
        .getEither(redirectUrlPolicy)
        .fold(
          _ => Future.successful(BadRequest),
          safeUrl =>
            try {
              lazy val tokenTimeout = configuration.getOptional[Long](s"portal.tokenTimeout").getOrElse(240L)
              val token = tokenEncryption.decryptToken(encryptedToken, tokenTimeout)
              val utr = token.utr

              for {
                value <- preferencesConnector.getEmailAddress(utr)
              } yield value match {
                case Some(emailAddress) =>
                  Redirect(
                    safeUrl.toString() ? ("email" -> tokenEncryption.crypto.encrypt(PlainText(emailAddress)).value)
                  )
                case _ => Redirect(safeUrl.url)
              }
            } catch {
              case e: TokenExpiredException =>
                logger.error("Unable to validate token", e)
                Future.successful(Redirect(safeUrl.url))
              case e: Exception =>
                logger.error("Exception happened while decrypting the token", e)
                Future.successful(Redirect(safeUrl.url))
            }
        )
    }
}
