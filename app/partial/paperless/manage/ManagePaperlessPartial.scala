/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package partial.paperless.manage

import connectors.PreferenceResponse
import model.{ Encrypted, HostContext }
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.emailaddress.EmailAddress

import javax.inject.{ Inject, Singleton }

@Singleton
class ManagePaperlessPartial @Inject() (
  digitalFalse: html.digital_false,
  digitalTrueBounced: html.digital_true_bounced,
  digitalTrueVerified: html.digital_true_verified,
  digitalTruePending: html.digital_true_pending
) {

  def apply(
    prefs: Option[PreferenceResponse]
  )(implicit request: Request[?], hostContext: HostContext, messages: Messages): HtmlFormat.Appendable =
    prefs match {
      case p @ Some(PreferenceResponse(_, Some(email), _, _, _)) if p.exists(_.genericTermsAccepted) =>
        (email.hasBounces, email.isVerified) match {
          case (true, _) => digitalTrueBounced(email)
          case (_, true) => digitalTrueVerified(email)
          case _         => digitalTruePending(email)
        }
      case Some(PreferenceResponse(_, email, _, _, _)) =>
        val encryptedEmail = email map (emailPreference => Encrypted(EmailAddress(emailPreference.email)))
        digitalFalse(encryptedEmail)
      case _ => digitalFalse(None)
    }
}
