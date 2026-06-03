/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import controllers.auth.AuthenticatedRequest
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.Future
import views.html.sessionTimeout
import play.api.i18n.I18nSupport
import model.HostContext
import uk.gov.hmrc.sca.services.WrapperService

class TimeoutController @Inject() (
  mcc: MessagesControllerComponents,
  sessionTimeOutView: sessionTimeout,
  val wrapperService: WrapperService,
  val appConfig: AppConfig
) extends FrontendController(mcc) with LayoutProvider with I18nSupport {

  implicit def toFuture(r: Result): Future[Result] = Future.successful(r)

  def keepAliveSession(): Action[AnyContent] = Action(NoContent)

  def timeout: Action[AnyContent] =
    Action.async { implicit request =>
      implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")
      implicit val authRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(request, None, None, None, None)
      Ok(
        layoutProvider(
          content = sessionTimeOutView(),
          title = "account.details.update.email.title"
        )
      )
    }
}
