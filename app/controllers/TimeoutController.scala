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

package controllers
import config.AppConfig
import controllers.auth.AuthenticatedRequest
import model.HostContext
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.sessionTimeout

import javax.inject.Inject
import scala.concurrent.Future

class TimeoutController @Inject() (
  mcc: MessagesControllerComponents,
  sessionTimeOutView: sessionTimeout,
  appConfig: AppConfig,
  layoutProvider: LayoutProvider
) extends FrontendController(mcc) with I18nSupport {

  implicit def toFuture(r: Result): Future[Result] = Future.successful(r)

  def keepAliveSession(): Action[AnyContent] = Action(NoContent)

  def timeout(implicit timeoutHostContext: model.TimeoutHostContext): Action[AnyContent] =
    Action.async { implicit request =>

      implicit val hostContext: HostContext =
        new HostContext(
          returnUrl = "",
          returnLinkText = "",
          regime = timeoutHostContext.regime,
          serviceUrl = timeoutHostContext.serviceUrl
        )

    implicit val authRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
      request = request,
      previousLoginTime = None,
      nino = None,
      saUtr = None,
      affinityGroup = None
    )

    Ok(
      layoutProvider.provideLayout(
        content = sessionTimeOutView(appConfig.signOutUrl(None)),
        title = "session.timeout.heading",
        showBackLinkJS = false,
        displaySignOutLink = false,
        appendServiceName = true,
        disableSessionExpired = true
      )
    )
    }
}
