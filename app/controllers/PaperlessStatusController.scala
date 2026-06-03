/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers

import connectors.{ PreferenceResponse, PreferencesConnector, StatusNameResponse }
import controllers.auth.WithAuthRetrievals
import controllers.internal.CohortCurrent
import model.StatusName.{ Alright, BouncedEmail, EmailNotVerified, NewCustomer, NoEmail, Paper, ReOptIn, ReOptInModified }
import model.{ PaperlessStatus, _ }
import play.api.i18n.{ I18nSupport, Lang }
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Results }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaperlessStatusController @Inject() (
  preferencesConnector: PreferencesConnector,
  val authConnector: AuthConnector,
  mcc: MessagesControllerComponents,
  externalUrlPrefixes: ExternalUrlPrefixes
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with WithAuthRetrievals {

  def getPaperlessStatus(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      authorised() {
        preferencesConnector
          .getPreferences()
          .map { pref =>
            determinePaperlessStatus(pref)(request.lang, hostContext) match {
              case Some(result) => Ok(Json.toJson(result))
              case None         =>
                // Should not happen as long as the BE provide a status
                InternalServerError("Couldn't get status for preference")
            }
          }
      }.recover { case _ =>
        Results.Unauthorized
      }
    }

  private def readPreferenceResponse(preference: Option[PreferenceResponse]): Option[StatusName] =
    preference match {
      case None => Some(NewCustomer)
      case Some(p) =>
        p.status.map(s => StatusNameResponse.toStatusName(s.name))
    }

  private def determinePaperlessStatus(
    preferenceResponse: Option[PreferenceResponse]
  )(implicit language: Lang, hostContext: HostContext): Option[StatusWithUrl] = {

    val email = preferenceResponse match {
      case Some(PreferenceResponse(_, Some(emailPreference), _, _, _)) => Some(emailPreference.email)
      case _                                                           => None
    }
    val checkSettingsUrl = externalUrlPrefixes.pfUrlPrefix + controllers.internal.routes.ManagePaperlessController
      .checkSettings(hostContext)

    val getEmail = hostContext.email match {
      case Some(email) => Some(Encrypted(EmailAddress(email)))
      case None        => None
    }

    val status = readPreferenceResponse(preferenceResponse)
    processStatus(status, checkSettingsUrl, email, getEmail)
  }

  private def processStatus(
    status: Option[StatusName],
    checkSettingsUrl: String,
    email: Option[String],
    getEmail: Option[Encrypted[EmailAddress]]
  )(implicit language: Lang, hostContext: HostContext): Option[StatusWithUrl] =
    status
      .map {
        case Paper =>
          StatusWithUrl(
            PaperlessStatus(
              Paper,
              Category(Paper),
              messagesApi("paperless.status.text1.paper"),
              messagesApi("paperless.status.text2.paper"),
              Some(CohortCurrent.ipage.majorVersion)
            ),
            Url(checkSettingsUrl, messagesApi("paperless.status.url.text.paper"))
          )
        case EmailNotVerified =>
          val emailReVerifyUrl =
            externalUrlPrefixes.pfUrlPrefix + controllers.internal.paperless.routes.EmailController
              .processEmailReVerificationJourney(hostContext.copy(email = email))
          StatusWithUrl(
            PaperlessStatus(
              EmailNotVerified,
              Category(EmailNotVerified),
              messagesApi("paperless.status.text1.email_not_verified"),
              messagesApi("paperless.status.text2.email_not_verified")
            ),
            Url(emailReVerifyUrl, messagesApi("paperless.status.url.text.email_not_verified"))
          )
        case BouncedEmail =>
          val bounceUrl =
            externalUrlPrefixes.pfUrlPrefix + controllers.internal.paperless.routes.EmailController
              .processEmailBounceJourney(hostContext.copy(email = email))
          StatusWithUrl(
            PaperlessStatus(
              BouncedEmail,
              Category(BouncedEmail),
              messagesApi("paperless.status.text1.bounced"),
              messagesApi("paperless.status.text2.bounced")
            ),
            Url(bounceUrl, messagesApi("paperless.status.url.text.bounced"))
          )
        case NewCustomer =>
          val optInRedirect =
            externalUrlPrefixes.pfUrlPrefix + controllers.internal.paperless.routes.ChoosePaperlessController
              .redirectToDisplayFormWithCohort(getEmail, hostContext)
          StatusWithUrl(
            PaperlessStatus(
              NewCustomer,
              Category(NewCustomer),
              messagesApi("paperless.status.text1.new_customer"),
              messagesApi("paperless.status.text2.new_customer"),
              Some(CohortCurrent.ipage.majorVersion)
            ),
            Url(optInRedirect, messagesApi("paperless.status.url.text.new_customer"))
          )
        case Alright =>
          StatusWithUrl(
            PaperlessStatus(
              Alright,
              Category(Alright),
              messagesApi("paperless.status.text1.alright"),
              messagesApi("paperless.status.text2.alright")
            ),
            Url(checkSettingsUrl, messagesApi("paperless.status.url.text.alright"))
          )
        case NoEmail =>
          StatusWithUrl(
            PaperlessStatus(
              NoEmail,
              Category(NoEmail),
              messagesApi("paperless.status.text1.no_email"),
              messagesApi("paperless.status.text2.no_email")
            ),
            Url(checkSettingsUrl, messagesApi("paperless.status.url.text.no_email.new"))
          )
        case ReOptIn =>
          val reOptInRedirect =
            externalUrlPrefixes.pfUrlPrefix + controllers.internal.paperless.routes.ChoosePaperlessController
              .displayForm(
                Some(CohortCurrent.reoptinpage),
                getEmail,
                hostContext.copy(cohort = Some(CohortCurrent.reoptinpage), email = email)
              )
          StatusWithUrl(
            PaperlessStatus(
              ReOptIn,
              Category(ReOptIn),
              messagesApi("paperless.status.text1.re_opt_in"),
              messagesApi("paperless.status.text2.re_opt_in"),
              Some(CohortCurrent.reoptinpage.majorVersion)
            ),
            Url(reOptInRedirect, messagesApi("paperless.status.url.text.re_opt_in"))
          )
        case ReOptInModified =>
          val reOptInModifiedJourney =
            externalUrlPrefixes.pfUrlPrefix + controllers.internal.paperless.routes.ChoosePaperlessController
              .displayForm(
                Some(CohortCurrent.reoptinpage),
                getEmail,
                hostContext.copy(cohort = Some(CohortCurrent.reoptinpage), email = email)
              )

          StatusWithUrl(
            PaperlessStatus(
              ReOptInModified,
              Category(ReOptInModified),
              messagesApi("paperless.status.text1.re_opt_in_modified"),
              messagesApi("paperless.status.text2.re_opt_in_modified"),
              Some(CohortCurrent.reoptinpage.majorVersion)
            ),
            Url(reOptInModifiedJourney, messagesApi("paperless.status.url.text.re_opt_in_modified"))
          )
      }
}
