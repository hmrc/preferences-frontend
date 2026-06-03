/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import cats.instances.list._
import cats.syntax.apply._
import config.AppConfig
import connectors.{ EntityResolverConnector, OptInPage, PreferencesConnector, TermsAccepted, TermsAndConditionsUpdate }
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import controllers.internal.EmailOptInJourney.AccountDetails
import controllers.internal.PaperlessChoice.OptedOut
import controllers.internal._
import controllers.{ LayoutProvider, internal }
import model.{ HostContext, Language, ReOptInModifiedJourney, SurveyType }
import play.api.Configuration
import play.api.data.{ Form, FormBinding }
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, Call, MessagesControllerComponents, MessagesRequest, Result }
import service.PreCheckService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.sca.services.WrapperService
import views.ViewHelper

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

class ReOptInController @Inject() (
  val entityResolverConnector: EntityResolverConnector,
  val preferencesConnector: PreferencesConnector,
  val precheckService: PreCheckService,
  val authConnector: AuthConnector,
  val auditConnector: AuditConnector,
  val configuration: Configuration,
  val wrapperService: WrapperService,
  val appConfig: AppConfig,
  val saPrintingPreference: views.html.sa.prefs.sa_printing_preference,
  saPrintingPreferenceReOptinBounceEmail: views.html.sa.prefs.sa_printing_preference_reoptin_bounce_email,
  saPrintingPreferenceReOptinEmail: views.html.sa.prefs.sa_printing_preference_reoptin_email,
  mcc: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) with OptInCohortCalculator with LayoutProvider with I18nSupport
    with WithAuthRetrievals with LanguageHelper with PaperlessCommon {

  def displayMultiPageReOptIn(hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request: MessagesRequest[AnyContent] =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => implicit hc =>
        val cohort = hostContext.cohort.getOrElse(CohortCurrent.reoptinpage)

        val call = internal.paperless.routes.ReOptInController.submitMultiPageReOptIn(hostContext)
        implicit val hostContextImpl: HostContext = hostContext

        hasStoredEmail(hostContext).map { _ =>
          Ok(makeLayoutForDisplayMultiPageReoptIn(cohort, call))
        }
      }
    }

  def submitMultiPageReOptIn(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => implicit hc =>
        val cohort = hostContext.cohort.getOrElse(CohortCurrent.reoptinpage)
        ReOptInStartForm()
          .bindFromRequest()(authRequest, FormBinding.Implicits.formBinding)
          .fold[Future[Result]](
            formwithErrors =>
              Future.successful(
                BadRequest(
                  layoutProvider(
                    content = saPrintingPreference(
                      formwithErrors,
                      paperless.routes.ReOptInController.submitMultiPageReOptIn(hostContext),
                      cohort
                    ),
                    title = cohortToTitle(cohort, hasError = true)
                  )
                )
              ),
            happyForm =>
              if (happyForm.choice.contains(OptedOut))
                saveAndAuditPreferences(
                  SaveAndAuditPreferences(
                    digital = false,
                    email = None,
                    hostContext.cohort.getOrElse(CohortCurrent.reoptinpage),
                    emailAlreadyStored = false,
                    languagePreference = Some(languageType(request.lang.code)),
                    if (hostContext.survey)
                      Some(SurveyType.StandardInterruptOptOut)
                    else None
                  )
                )(authRequest, hostContext, hc)
              else
                getJourney(
                  hostContext,
                  {
                    case Some(_: ReOptInModifiedJourney) =>
                      Redirect(
                        paperless.routes.ReOptInController.displayMultiPageReOptInBounceEmail(hostContext)
                      )
                    case _ =>
                      Redirect(
                        paperless.routes.ReOptInController.displayMultiPageReOptInEmail(hostContext)
                      )
                  }
                )(authRequest, hc)
          )
      }
    }

  def displayMultiPageReOptInBounceEmail(hostContext: model.HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => _ =>
        implicit val hostContextImpl: HostContext = hostContext
        Future.successful(Ok(makeLayoutForDisplayMultiPageReOptInBounceEmail()))
      }
    }

  def displayMultiPageReOptInEmail(
    hostContext: model.HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => _ =>
        implicit val hostContextImpl: HostContext = hostContext
        Future.successful(Ok(makeLayoutForDisplayMultiPageReOptInEmail()))
      }
    }

  def submitMultiPageReOptInEmail(
    hasEmailBounce: Boolean,
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => implicit hc =>
        implicit val hostContextImpl: HostContext = hostContext

        ReOptInEmailForm()
          .bindFromRequest()(request, FormBinding.Implicits.formBinding)
          .fold(
            formwithErrors => makeSubmitMultiPageReOptInEmailErrorResponse(hasEmailBounce, request, formwithErrors),
            happyForm =>
              saveMultiPageReOptinPreference(
                happyForm.email,
                Some(languageType(request.lang.code))
              )
          )
      }
    }

  private def makeSubmitMultiPageReOptInEmailErrorResponse(
    hasEmailBounce: Boolean,
    request: MessagesRequest[AnyContent],
    formwithErrors: Form[ReOptInEmailForm.Data]
  )(implicit
    authReq: AuthenticatedRequest[?],
    hostContext: HostContext
  ) = {
    // hack to satisfy DC-3301 error handling requirements
    val useExistingEmail = formwithErrors.data
      .get("sps-re-opt-in")
      .contains("false")
    if (useExistingEmail)
      saveMultiPageReOptinPreference(
        None,
        Some(languageType(request.lang.code))
      )
    else
      Future.successful(
        BadRequest(
          makeLayoutForSubmitMultiPageReOptInEmail(hasEmailBounce, formwithErrors)
        )
      )
  }

  private def saveMultiPageReOptinPreference(
    newEmailAddress: Option[String],
    languagePreference: Some[Language]
  )(implicit request: AuthenticatedRequest[?], hc: HeaderCarrier, hostContext: HostContext): Future[Result] =
    (
      Validated.fromOption(hostContext.email, List("Missing original email in multi-page re-opt in")),
      Validated.fromOption(hostContext.cohort, List("Missing cohort in multi-page re-opt in"))
    ).tupled
      .bimap(
        errors => Future.successful(BadRequest(errors.mkString(", "))),
        emailAndCohort => updateTermsAndAudit(newEmailAddress, languagePreference, emailAndCohort._1, emailAndCohort._2)
      ) match {
      case Valid(a)   => a
      case Invalid(e) => e
    }

  private def updateTermsAndAudit(
    newEmailAddress: Option[String],
    languagePreference: Some[Language],
    email: String,
    cohort: OptInCohort
  )(implicit authReq: AuthenticatedRequest[?], hostContext: HostContext, hc: HeaderCarrier) = {
    val terms = cohort.terms -> TermsAccepted(accepted = true, Some(OptInPage.from(cohort)))
    preferencesConnector
      .optIn(
        TermsAndConditionsUpdate
          .from(terms, Some(email), languagePreference, hostContext.journey)
      )
      .andThen { case Success(preferencesStatus) =>
        auditChoice(AccountDetails, cohort, terms, Some(email), preferencesStatus, hostContext.regime)
      }
      .flatMap { _ =>
        if (newEmailAddress.isEmpty)
          getReturnResult
        else
          changeEmailAddress(newEmailAddress)
      }
      .recover { case _ =>
        BadRequest("Unable to change email while re-opt-in")
      }
  }

  private def changeEmailAddress(
    newEmailAddress: Option[String]
  )(implicit hc: HeaderCarrier, hostContext: HostContext) =
    preferencesConnector
      .changeEmailAddress(newEmailAddress.get)
      .map(_ =>
        Redirect(
          paperless.routes.EmailController.displayEmailConfirmation(
            hostContext
              .copy(cohort = Some(CohortCurrent.reoptinpage), email = newEmailAddress, journey = None)
          )
        )
      )

  private def makeLayoutForDisplayMultiPageReoptIn(cohort: OptInCohort, call: Call)(implicit
    authRequest: AuthenticatedRequest[?],
    hc: HostContext
  ) =
    layoutProvider(
      content = saPrintingPreference(
        emailForm = ReOptInStartForm().fill(
          ReOptInStartForm.Data(
            choice = None
          )
        ),
        submitPrefsFormAction = call,
        cohort = cohort
      ),
      title = cohortToTitle(cohort)
    )

  private def makeLayoutForDisplayMultiPageReOptInBounceEmail()(implicit
    authenticatedRequest: AuthenticatedRequest[?],
    hc: HostContext
  ) =
    layoutProvider(
      content = saPrintingPreferenceReOptinBounceEmail(
        ReOptInEmailForm(),
        internal.paperless.routes.ReOptInController
          .submitMultiPageReOptInEmail(
            hasEmailBounce = true,
            hc.copy(journey = Some(ViewHelper.RE_OPT_IN_MODIFY))
          )
      ),
      title = "sa_printing_preference.sps_opt_in_email"
    )

  private def makeLayoutForDisplayMultiPageReOptInEmail()(implicit
    authenticatedRequest: AuthenticatedRequest[?],
    hc: HostContext
  ) =
    layoutProvider(
      content = saPrintingPreferenceReOptinEmail(
        ReOptInEmailForm(),
        internal.paperless.routes.ReOptInController.submitMultiPageReOptInEmail(hasEmailBounce = false, hc)
      ),
      title = "sa_printing_preference.sps_re_opt_in_email.title"
    )

  private def makeLayoutForSubmitMultiPageReOptInEmail(
    hasEmailBounce: Boolean,
    formwithErrors: Form[ReOptInEmailForm.Data]
  )(implicit authenticatedRequest: AuthenticatedRequest[?], hc: HostContext) = {

    val updatedErrors =
      if (formwithErrors.error("sps-re-opt-in").isDefined)
        formwithErrors.errors.filterNot(e => e.key == "sps-re-opt-in-email")
      else formwithErrors.errors
    val templateFn =
      if (hasEmailBounce)
        saPrintingPreferenceReOptinBounceEmail.apply
      else
        saPrintingPreferenceReOptinEmail.apply

    layoutProvider(
      content = templateFn(
        formwithErrors.copy(errors = updatedErrors),
        internal.paperless.routes.ReOptInController
          .submitMultiPageReOptInEmail(hasEmailBounce, hc)
      ),
      title = "sa_printing_preference.sps_re_opt_in_email.title"
    )
  }

}
