/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package config

import controllers.LayoutProvider
import controllers.auth.AuthenticatedRequest
import model.HostContext
import play.api.i18n.MessagesApi
import play.api.mvc.{ AnyContent, Request, RequestHeader }
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.hmrc.sca.services.WrapperService
import views.html.errorTemplate

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ErrorHandler @Inject() (
  errorPage: errorTemplate,
  val messagesApi: MessagesApi,
  val wrapperService: WrapperService,
  val appConfig: AppConfig
)(override implicit val ec: ExecutionContext)
    extends FrontendErrorHandler with LayoutProvider {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: RequestHeader
  ): Future[Html] = {
    implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")
    implicit val authRequest = AuthenticatedRequest(Request(request, AnyContent), None, None, None, None)
    Future {
      layoutProvider(
        content = errorPage(heading, message),
        title = pageTitle
      )
    }
  }
}
