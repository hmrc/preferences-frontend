/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import config.AppConfig
import connectors.{ EntityResolverConnector, PreferencesConnector }
import controllers.PageIdentifier.*
import controllers.{ PageIdentifier, REGIME_ITSA }
import controllers.auth.AuthenticatedRequest
import controllers.internal.{ IPage53, IPage56, IPage7, IPage8, IosReOptOutPage51, ReOptInPage10, ReOptInPage52, ReOptInPage54, ReOptInPage55 }
import model.HostContext
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import service.PreCheckService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.sca.services.WrapperService
import utils.SpecBase
import utils.TestData.EMPTY_STRING
import views.html.sa.prefs.sa_printing_preference

import scala.concurrent.{ ExecutionContext, Future }

class PaperlessCommonSpec extends SpecBase {

  "cohortToTitle" should {

    "return correct title" when {
      "cohort is IPage7" in new Setup {
        paperlessCommon.cohortToTitle(IPage7) must be("i_page7.fg_page.title")
      }

      "cohort is IPage8" in new Setup {
        paperlessCommon.cohortToTitle(IPage8) must be("i_page8.fg_page.title")
      }

      "cohort is IPage53 and page has no error" in new Setup {
        paperlessCommon.cohortToTitle(IPage53) must be("i_page53.fg_page.title")
      }

      "cohort is IPage53 and page has error" in new Setup {
        paperlessCommon.cohortToTitle(IPage53, true) must be("i_page53.fg_page.title.error")
      }

      "cohort is IPage56, regime is itsa and page has no error" in new Setup {
        paperlessCommon.cohortToTitle(IPage56, false, REGIME_ITSA) must be("i_page56.fg_page.itsa.title")
      }

      "cohort is IPage56, regime is itsa and page has error" in new Setup {
        paperlessCommon.cohortToTitle(IPage56, true, REGIME_ITSA) must be("i_page56.fg_page.itsa.title.error")
      }

      "cohort is IPage56, regime is empty and page has no error" in new Setup {
        paperlessCommon.cohortToTitle(IPage56, false, EMPTY_STRING) must be("i_page56.fg_page.title")
      }

      "cohort is IPage56, regime is empty and page has error" in new Setup {
        paperlessCommon.cohortToTitle(IPage56, true, EMPTY_STRING) must be("i_page56.fg_page.title.error")
      }

      "cohort is IPage56, regime is non itsa and page has no error" in new Setup {
        paperlessCommon.cohortToTitle(IPage56, false, "nino") must be("i_page56.fg_page.title")
      }

      "cohort is IPage56, regime is non itsa and page has error" in new Setup {
        paperlessCommon.cohortToTitle(IPage56, true, "nino") must be("i_page56.fg_page.title.error")
      }

      "cohort is ReOptInPage10" in new Setup {
        paperlessCommon.cohortToTitle(ReOptInPage10) must be("reoptin_page10.fg_page.title")
      }

      "cohort is ReOptInPage52" in new Setup {
        paperlessCommon.cohortToTitle(ReOptInPage52) must be("reoptin_page52.fg_page.title")
      }

      "cohort is ReOptInPage54 and page has no error" in new Setup {
        paperlessCommon.cohortToTitle(ReOptInPage54) must be("reoptin_page54.fg_page.title")
      }

      "cohort is ReOptInPage54 and page has error" in new Setup {
        paperlessCommon.cohortToTitle(ReOptInPage54, true) must be("reoptin_page54.fg_page.title")
      }

      "cohort is ReOptInPage55 and page has no error" in new Setup {
        paperlessCommon.cohortToTitle(ReOptInPage55) must be("reoptin_page55.fg_page.title")
      }

      "cohort is ReOptInPage55 and page has error" in new Setup {
        paperlessCommon.cohortToTitle(ReOptInPage55) must be("reoptin_page55.fg_page.title")
      }

      "cohort falls under default case" in new Setup {
        paperlessCommon.cohortToTitle(IosReOptOutPage51) must be("i_page53.fg_page.title")
      }
    }
  }

  "titleKeyForPageAndRegime" should {

    "return correct title key" when {

      "page identifier is OPT_OUT_CONFIRMATION and regime is itsa" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptOutConfirmation,
          regime = REGIME_ITSA
        ) must be("sa_printing_preference.sps.itsa.opt_out_by_post_confimation_title")
      }

      "page identifier is OPT_OUT_CONFIRMATION and regime is other than itsa" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptOutConfirmation,
          regime = "nino"
        ) must be("sa_printing_preference.sps.opt_out_by_post_confimation_title")
      }

      "page identifier is OPT_OUT_CONFIRMATION and regime is empty" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptOutConfirmation,
          regime = EMPTY_STRING
        ) must be("sa_printing_preference.sps.opt_out_by_post_confimation_title")
      }

      "page identifier is OPT_IN_EMAIL, regime is itsa and page has error" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptInMail,
          hasError = true,
          regime = REGIME_ITSA
        ) must be("sa_printing_preference.itsa.sps_opt_in_email_error")
      }

      "page identifier is OPT_IN_EMAIL, regime is itsa and page has no error" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptInMail,
          regime = REGIME_ITSA
        ) must be("sa_printing_preference.itsa.sps_opt_in_email")
      }

      "page identifier is OPT_IN_EMAIL, regime is other than itsa and page has error" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptInMail,
          hasError = true,
          regime = "nino"
        ) must be("sa_printing_preference.sps_opt_in_email_error")
      }

      "page identifier is OPT_IN_EMAIL, regime is other than itsa and page has no error" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptInMail,
          regime = "nino"
        ) must be("sa_printing_preference.sps_opt_in_email")
      }

      "page identifier is OPT_IN_EMAIL and regime is empty" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptInMail,
          regime = EMPTY_STRING
        ) must be("sa_printing_preference.sps_opt_in_email")
      }

      "page identifier is OPT_IN_CONFIRMATION and regime is itsa" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptInConfirmation,
          regime = REGIME_ITSA
        ) must be("sa_printing_preference.sps_email_confirm.itsa.title")
      }

      "page identifier is OPT_IN_CONFIRMATION and regime is other than itsa" in new Setup {
        paperlessCommon.titleKeyForPageAndRegime(
          pageIdentifier = OptInConfirmation,
          regime = "nino"
        ) must be("sa_printing_preference.sps_email_confirm")
      }
    }
  }

  trait Setup {
    val paperlessCommon: PaperlessCommon = new PaperlessCommon {
      override val auditConnector: AuditConnector = mock[AuditConnector]
      override val configuration: Configuration = mock[Configuration]
      override val entityResolverConnector: EntityResolverConnector = mock[EntityResolverConnector]
      override val saPrintingPreference: sa_printing_preference = app.injector.instanceOf[sa_printing_preference]
      override val precheckService: PreCheckService = mock[PreCheckService]
      override val preferencesConnector: PreferencesConnector = mock[PreferencesConnector]
      override def messagesApi: MessagesApi = messageApi
      override def appConfig: AppConfig = mock[AppConfig]
      override def wrapperService: WrapperService = mock[WrapperService]

      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
    }
  }
}
