/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.auth.AuthenticatedRequest
import model.HostContext
import play.api.Configuration
import play.api.i18n.{ Messages, MessagesApi }
import play.twirl.api.{ Html, HtmlFormat }
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukBackLink, GovukPhaseBanner, TwoThirdsMainContent, TwoThirdsOneThirdMainContent }
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.hmrcfrontend.views.config.{ StandardAlphaBanner, StandardBetaBanner }
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{ HmrcReportTechnicalIssueHelper, HmrcServiceNavigationLanguageSelectHelper, HmrcStandardPage, HmrcTimeoutDialogHelper }
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.hmrcstandardpage.ServiceURLs
import uk.gov.hmrc.sca.config.BackLinkConfig
import uk.gov.hmrc.sca.services.WrapperService
import uk.gov.hmrc.sca.utils.WebchatUtil
import uk.gov.hmrc.sca.views.html.components.FullWidthMainContent
import uk.gov.hmrc.sca.views.html.{ BespokeUserResearchBannerView, PtaAttorneyBanner, PtaHead, PtaMenuBar, PtaScripts, StandardScaLayout }
import util.PageTitle
import views.ViewHelper

class LayoutProvider @Inject() (
  govukBackLink: GovukBackLink,
  hmrcStandardPage: HmrcStandardPage,
  hmrcReportTechnicalIssueHelper: HmrcReportTechnicalIssueHelper,
  alphaBanner: StandardAlphaBanner,
  betaBanner: StandardBetaBanner,
  hmrcLanguageSelectHelper: HmrcServiceNavigationLanguageSelectHelper,
  hmrcTimeoutDialogHelper: HmrcTimeoutDialogHelper,
  ptaScripts: PtaScripts,
  ptaHead: PtaHead,
  govukPhaseBanner: GovukPhaseBanner,
  ptaAttorneyBanner: PtaAttorneyBanner,
  twoThirdsOneThirdMainContent: TwoThirdsOneThirdMainContent,
  fullWidthMainContent: FullWidthMainContent,
  twoThirdsMainContent: TwoThirdsMainContent,
  bespokeUserResearchBannerView: BespokeUserResearchBannerView,
  ptaMenuBar: PtaMenuBar,
  webchatUtil: WebchatUtil,
  configuration: Configuration,
  messagesApi: MessagesApi,
  appConfig: config.AppConfig
)(implicit contactFrontendConfig: ContactFrontendConfig) {

  def provideLayout(
    content: Html,
    title: String,
    showBackLinkJS: Boolean = true,
    displaySignOutLink: Boolean = true,
    appendServiceName: Boolean = false,
    disableSessionExpired: Boolean = false
  )(implicit
    messages: Messages,
    request: AuthenticatedRequest[?],
    hostContext: model.HostContext
  ): HtmlFormat.Appendable = {
    val serviceNameKey = ViewHelper.serviceName(Some("service.name"), hostContext.regime)

    val pageTitle =
      if (appendServiceName)
        PageTitle(Messages(title), serviceNameKey.map(Messages(_)))
      else
        Messages(title)

    val wrapperService = createDynamicSignInWrapperService(hostContext)

    wrapperService
      .standardScaLayout(
        content = content,
        pageTitle = Some(pageTitle),
        serviceURLs = ServiceURLs(
          serviceUrl = hostContext.serviceUrl,
          signOutUrl = if (displaySignOutLink) Some(appConfig.signOutUrl(Option(hostContext.returnUrl))) else None
        ),
        hideMenuBar = true,
        backLinkConfig = Option.when(showBackLinkJS)(BackLinkConfig.JsBack),
        fullWidth = false,
        serviceNameKey = serviceNameKey,
        disableSessionExpired = disableSessionExpired
      )
  }

  private def createDynamicSignInWrapperService(hostContext: HostContext): WrapperService = {
    val scaAppConfig = new uk.gov.hmrc.sca.config.AppConfig(
      configuration,
      messagesApi
    ) {
      private val maybeConfigUrl: Option[String] =
        configuration.getOptional[String]("sca-wrapper.signin.url")

      private val params = HostContext.hostContextBinder
        .unbind("anyValName", hostContext) match {
        case ""    => ""
        case value => s"?$value"
      }

      override val timeOutUrl: Option[String] = maybeConfigUrl.map(_ + params)
    }

    val layout = new StandardScaLayout(
      scaAppConfig,
      govukBackLink,
      hmrcStandardPage,
      hmrcReportTechnicalIssueHelper,
      alphaBanner,
      betaBanner,
      hmrcLanguageSelectHelper,
      hmrcTimeoutDialogHelper,
      ptaScripts,
      ptaHead,
      govukPhaseBanner,
      ptaAttorneyBanner,
      twoThirdsOneThirdMainContent,
      fullWidthMainContent,
      twoThirdsMainContent,
      bespokeUserResearchBannerView
    )

    new WrapperService(
      ptaMenuBar,
      layout,
      webchatUtil,
      scaAppConfig
    )
  }
}
