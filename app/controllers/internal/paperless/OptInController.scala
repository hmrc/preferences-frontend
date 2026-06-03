/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import config.AppConfig
import connectors.{ EntityResolverConnector, OptInPage, PreferencesConnector, TermsAccepted, TermsAndConditionsUpdate }
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import controllers.internal.EmailOptInJourney.AccountDetails
import controllers.internal.PaperlessChoice.OptedOut
import controllers.internal.*
import controllers.{ EMPTY_STRING, LayoutProvider, PageIdentifier, REGIME_ITSA, internal }
import model.{ HostContext, Language, SurveyType }
import play.api.Configuration
import play.api.data.FormBinding
import play.api.i18n.I18nSupport
import play.api.mvc.*
import service.PreCheckService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.sca.services.WrapperService
import PageIdentifier.{ OptInConfirmation, OptInMail }

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class OptInController @Inject() (
  val entityResolverConnector: EntityResolverConnector,
  val preferencesConnector: PreferencesConnector,
  val authConnector: AuthConnector,
  val auditConnector: AuditConnector,
  val configuration: Configuration,
  val wrapperService: WrapperService,
  val appConfig: AppConfig,
  val saPrintingPreference: views.html.sa.prefs.sa_printing_preference,
  val precheckService: PreCheckService,
  saPrintingPreferenceOptinEmail: views.html.sa.prefs.sa_printing_preference_optin_email,
  saPrintingPreferenceOptinConfirmation: views.html.sa.prefs.sa_printing_preference_optin_confirmation,
  mcc: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) with WithAuthRetrievals with LayoutProvider with LanguageHelper with I18nSupport
    with PaperlessCommon {

  def displayOptIn(hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request: MessagesRequest[AnyContent] =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => implicit hc =>
        val call = internal.paperless.routes.OptInController.submitOptIn(hostContext)
        implicit val hostContextImpl: HostContext = hostContext
        hasStoredEmail(hostContext).map { emailAlreadyStored =>
          Ok(
            layoutProvider(
              content = saPrintingPreference(
                emailForm = OptInStartForm().fill(
                  OptInStartForm.Data(
                    choice = None,
                    emailAlreadyStored = Some(emailAlreadyStored)
                  )
                ),
                submitPrefsFormAction = call,
                cohort = CohortCurrent.ipage
              ),
              title = cohortToTitle(CohortCurrent.ipage, false, hostContext.regime.getOrElse(EMPTY_STRING))
            )
          )
        }
      }
    }

  def submitOptIn(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request: MessagesRequest[AnyContent] =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => implicit hc =>
        handleOptInChoice()
      }
    }

  def submitOptInEmail(
    changeEmail: Option[Boolean],
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => implicit hc =>
        implicit val hostContextImpl: HostContext = hostContext
        OptInEmailForm()
          .bindFromRequest()(request, FormBinding.Implicits.formBinding)
          .fold(
            formwithErrors =>
              Future.successful(
                BadRequest(
                  layoutProvider(
                    content = saPrintingPreferenceOptinEmail(
                      formwithErrors,
                      internal.paperless.routes.OptInController.submitOptInEmail(changeEmail, hostContext)
                    ),
                    title = titleKeyForPageAndRegime(
                      pageIdentifier = OptInMail,
                      hasError = true,
                      regime = hostContext.regime.getOrElse(EMPTY_STRING)
                    )
                  )
                )
              ),
            happyForm =>
              saveOptinPreference(
                changeEmail.getOrElse(false),
                happyForm,
                Some(languageType(request.lang.code)),
                hostContext
              )
          )
      }
    }

  def displayOptInEmail(
    changeEmail: Option[Boolean],
    hostContext: model.HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => _ =>
        implicit val hostContextImpl: HostContext = hostContext
        Future.successful(
          Ok(
            layoutProvider(
              content = saPrintingPreferenceOptinEmail(
                OptInEmailForm().fill(hostContext.email.getOrElse(EMPTY_STRING)),
                internal.paperless.routes.OptInController.submitOptInEmail(changeEmail, hostContext)
              ),
              title = titleKeyForPageAndRegime(
                pageIdentifier = OptInMail,
                regime = hostContext.regime.getOrElse(EMPTY_STRING)
              )
            )
          )
        )
      }
    }

  def displayOptInConfirmation(
    hostContext: model.HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => _ =>
        implicit val hostContextImpl: HostContext = hostContext
        Future.successful(
          Ok(
            layoutProvider(
              content = saPrintingPreferenceOptinConfirmation(hostContext.email.getOrElse(EMPTY_STRING)),
              title = titleKeyForPageAndRegime(
                pageIdentifier = OptInConfirmation,
                regime = hostContext.regime.getOrElse(EMPTY_STRING)
              )
            )
          )
        )
      }
    }

  private def saveOptinPreference(
    changeEmail: Boolean,
    emailAddress: String,
    languagePreference: Some[Language],
    hostContext: HostContext
  )(implicit request: AuthenticatedRequest[?], hc: HeaderCarrier): Future[Result] = {
    val terms =
      CohortCurrent.ipage.terms -> TermsAccepted(accepted = true, Some(OptInPage.from(CohortCurrent.ipage)), None)
    val journey = hostContext.journey
    val updtTandCRes =
      if (changeEmail) preferencesConnector.changeEmailAddress(emailAddress, journey)
      else
        preferencesConnector
          .optIn(
            TermsAndConditionsUpdate
              .from(terms, Some(emailAddress), languagePreference, journey)
          )(hc, hostContext = hostContext, ec)
          .map(prefStatus => auditChoice(AccountDetails, CohortCurrent.ipage, terms, Some(emailAddress), prefStatus))

    updtTandCRes.map { _ =>
      val _hc = hostContext.copy(email = Some(emailAddress))
      Redirect(
        paperless.routes.OptInController.displayOptInConfirmation(_hc)
      )
    }
  }

  private def handleOptInChoice()(implicit
    request: AuthenticatedRequest[?],
    hc: HeaderCarrier,
    hostContext: HostContext
  ): Future[Result] = {
    val call = paperless.routes.OptInController.submitOptIn(hostContext)
    val lang = languageType(request.lang.code)
    OptInStartForm()
      .bindFromRequest()(request, FormBinding.Implicits.formBinding)
      .fold(
        returnToFormWithErrors(call, CohortCurrent.ipage, request),
        happyForm =>
          if (happyForm.choice.contains(OptedOut))
            saveAndAuditPreferences(
              SaveAndAuditPreferences(
                digital = false,
                email = None,
                CohortCurrent.ipage,
                emailAlreadyStored = false,
                languagePreference = Some(lang),
                // always set this so the confirmation page is shown
                Some(SurveyType.StandardInterruptOptOut)
              )
            )(request, hostContext, hc)
          else {
            val _hc = hostContext.copy(email = Some(EMPTY_STRING))
            val changeEmail = happyForm.isEmailAlreadyStored
            Future.successful(
              Redirect(
                paperless.routes.OptInController.displayOptInEmail(Some(changeEmail), _hc)
              )
            )
          }
      )
  }
}
