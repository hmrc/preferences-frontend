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
import model.HostContext
import play.api.Configuration
import play.api.i18n.MessagesApi
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukBackLink, GovukPhaseBanner, TwoThirdsMainContent, TwoThirdsOneThirdMainContent }
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.hmrcfrontend.views.config.{ StandardAlphaBanner, StandardBetaBanner }
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{ HmrcReportTechnicalIssueHelper, HmrcServiceNavigationLanguageSelectHelper, HmrcStandardPage, HmrcTimeoutDialogHelper }
import uk.gov.hmrc.sca.config.AppConfig
import uk.gov.hmrc.sca.services.WrapperService
import uk.gov.hmrc.sca.utils.WebchatUtil
import uk.gov.hmrc.sca.views.html.components.FullWidthMainContent
import uk.gov.hmrc.sca.views.html.{ BespokeUserResearchBannerView, PtaAttorneyBanner, PtaHead, PtaMenuBar, PtaScripts, StandardScaLayout }

class DynamicSigninWrapperServiceFactory @Inject() (
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
  messagesApi: MessagesApi
)(implicit contactFrontendConfig: ContactFrontendConfig) {

  def createDynamicSignInWrapperService(hostContext: model.HostContext): WrapperService = {
    val appConfig = new AppConfig(
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
      appConfig,
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
      appConfig
    )
  }
}
