/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package partial.paperless

import connectors.PreferencesConnector
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import model.HostContext
import partial.paperless.manage.ManagePaperlessPartial
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaperlessPartialController @Inject() (
  preferencesConnector: PreferencesConnector,
  val authConnector: AuthConnector,
  managePaperlessPartial: ManagePaperlessPartial,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with WithAuthRetrievals {

  def displayManagePaperlessPartial(implicit returnUrl: HostContext): Action[AnyContent] =
    Action.async { request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[?] => implicit hc: HeaderCarrier =>
        preferencesConnector.getPreferences().map { pref =>
          Ok(managePaperlessPartial(pref))
        }
      }(request, ec)
    }

  def displayPaperlessWarningsPartial(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[?] => implicit hc: HeaderCarrier =>
        preferencesConnector.getPreferences().map {
          case None => NotFound
          case Some(prefs) =>
            Ok(PaperlessWarningPartial.apply(prefs, hostContext))
              .withHeaders("X-Opted-In-Email" -> prefs.genericTermsAccepted.toString)
        }
      }(request, ec)
    }
}
