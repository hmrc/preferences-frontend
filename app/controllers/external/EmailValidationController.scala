/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.external

import config.AppConfig
import connectors._
import controllers.LayoutProvider
import controllers.auth.AuthenticatedRequest
import model.HostContext
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.sca.services.WrapperService

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class EmailValidationController @Inject() (
  preferencesConnector: PreferencesConnector,
  val wrapperService: WrapperService,
  val appConfig: AppConfig,
  saPrintingPreferenceExpiredEmail: views.html.sa.prefs.sa_printing_preference_expired_email,
  saPrintingPreferenceVerifyEmailFailed: views.html.sa.prefs.sa_printing_preference_verify_email_failed,
  saPrintingPreferenceVerifyEmail: views.html.sa.prefs.sa_printing_preference_verify_email,
  saPrintingPreferenceWrongToken: views.html.sa.prefs.sa_printing_preference_wrong_token,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with LayoutProvider with I18nSupport {

  val regex = "([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12})".r

  def verify(token: String): Action[AnyContent] =
    Action.async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      implicit val authRequest = AuthenticatedRequest(request, None, None, None, None)
      implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")
      token match {
        case regex(_) =>
          preferencesConnector.updateEmailValidationStatusUnsecured(token) map {
            case Validated =>
              Ok(
                layoutProvider(
                  content = saPrintingPreferenceVerifyEmail(None, None),
                  title = "sa_printing_preference.email.verified.title",
                  showBackLinkJS = false
                )
              )
            case ValidatedWithReturn(returnText, returnUrl) =>
              Ok(
                layoutProvider(
                  content = saPrintingPreferenceVerifyEmail(Some(returnUrl), Some(returnText)),
                  title = "sa_printing_preference.email.verified.title",
                  showBackLinkJS = false
                )
              )
            case ValidationExpired =>
              Ok(
                layoutProvider(
                  content = saPrintingPreferenceExpiredEmail(),
                  title = "sa_printing_preference.token.expired.title",
                  showBackLinkJS = false
                )
              )
            case WrongToken =>
              Ok(
                layoutProvider(
                  content = saPrintingPreferenceWrongToken(),
                  title = "sa_printing_preference.token.wrong.title",
                  showBackLinkJS = false
                )
              )
            case ValidationErrorWithReturn(returnLinkText, returnUrl) =>
              BadRequest(
                layoutProvider(
                  content = saPrintingPreferenceVerifyEmailFailed(Some(returnUrl), Some(returnLinkText)),
                  title = "sa_printing_preference.email.failed.verified.title",
                  showBackLinkJS = false
                )
              )
            case ValidationError =>
              BadRequest(
                layoutProvider(
                  content = saPrintingPreferenceVerifyEmailFailed(None, None),
                  title = "sa_printing_preference.email.failed.verified.title",
                  showBackLinkJS = false
                )
              )
          }
        case _ =>
          Future.successful(
            BadRequest(
              layoutProvider(
                content = saPrintingPreferenceVerifyEmailFailed(None, None),
                title = "sa_printing_preference.email.failed.verified.title",
                showBackLinkJS = false
              )
            )
          )
      }
    }
}
