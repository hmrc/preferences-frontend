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
import play.api.i18n.Messages
import play.twirl.api.{ Html, HtmlFormat }
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.hmrcstandardpage.ServiceURLs
import uk.gov.hmrc.sca.services.WrapperService
import views.ViewHelper

trait LayoutProvider {
  def wrapperService: WrapperService
  def appConfig: AppConfig
  def layoutProvider(content: Html, title: String, showBackLinkJS: Boolean = true)(implicit
    messages: Messages,
    request: AuthenticatedRequest[?],
    hostContext: model.HostContext
  ): HtmlFormat.Appendable =
    wrapperService.standardScaLayout(
      content = content,
      pageTitle = Some(Messages(title)),
      serviceURLs = ServiceURLs(
        serviceUrl = hostContext.serviceUrl,
        signOutUrl = Some(appConfig.signOutUrl(Option(hostContext.returnUrl)))
      ),
      hideMenuBar = true,
      showBackLinkJS = showBackLinkJS,
      fullWidth = false,
      serviceNameKey = ViewHelper.serviceName(Some("service.name"), hostContext.regime)
    )
}
