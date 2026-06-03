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

package controllers.internal.paperless

import config.AppConfig
import connectors.*
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import controllers.internal.EmailOptInJourney.*
import controllers.internal.PaperlessChoice.OptedIn
import controllers.internal.*
import controllers.{ EMPTY_STRING, LayoutProvider, PageIdentifier, internal }
import model.JourneyType.{ MultiPage1, MultiPage2 }
import model.*
import play.api.data.{ Form, FormBinding }
import play.api.i18n.I18nSupport
import play.api.libs.json.*
import play.api.mvc.*
import play.api.{ Configuration, Logger }
import service.PreCheckService
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.emailaddress.{ EmailAddress, EmailAddressValidation }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.*
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ DataCall, EventTypes, MergedDataEvent }
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.sca.services.WrapperService
import PageIdentifier.OptOutConfirmation

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class ChoosePaperlessController @Inject() (
  val entityResolverConnector: EntityResolverConnector,
  val preferencesConnector: PreferencesConnector,
  val precheckService: PreCheckService,
  val wrapperService: WrapperService,
  val appConfig: AppConfig,
  emailValidation: EmailAddressValidation,
  val auditConnector: AuditConnector,
  val authConnector: AuthConnector,
  val configuration: Configuration,
  val saPrintingPreference: views.html.sa.prefs.sa_printing_preference,
  saPrintingPreferenceVerifyEmail: views.html.sa_printing_preference_verify_email,
  accountDetailsPrintingPreferenceConfirm: views.html.account_details_printing_preference_confirm,
  saOptOutConfirmation: views.html.sa.prefs.sa_printing_preference_sa_opt_out_confirmation,
  changeLanguage: views.html.change_language,
  mcc: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) with OptInCohortCalculator with LayoutProvider with I18nSupport
    with WithAuthRetrievals with LanguageHelper with PaperlessCommon {

  val logger: Logger = Logger(this.getClass)
  def redirectToDisplayFormWithCohort(
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { (_: AuthenticatedRequest[AnyContent]) => _ =>
        Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))
      }
    }

  private def createRedirectToDisplayFormWithCohort(
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ) =
    Redirect(
      paperless.routes.ChoosePaperlessController
        .displayForm(Some(calculateCohort(hostContext)), emailAddress, hostContext)
    )

  def displayForm(
    cohort: Option[OptInCohort],
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        cohort.fold(ifEmpty = Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))) {
          cohort =>
            auditPageShown(AccountDetails, cohort)
            val email: Option[EmailAddress] = emailAddress.map(_.decryptedValue)

            cohort.journeyType match {
              case Some(MultiPage1) =>
                Future.successful(
                  Redirect(
                    paperless.routes.OptInController.displayOptIn(hostContext)
                  )
                )
              case Some(MultiPage2) =>
                Future.successful(
                  Redirect(
                    paperless.routes.ReOptInController.displayMultiPageReOptIn(hostContext)
                  )
                )
              case _ =>
                implicit val hostContextImpl: HostContext = hostContext
                hasStoredEmail(hostContext).map { emailAlreadyStored =>
                  Ok(
                    layoutProvider(
                      content = saPrintingPreference(
                        emailForm = form(emailAlreadyStored, cohort, email),
                        submitPrefsFormAction =
                          internal.paperless.routes.ChoosePaperlessController.submitForm(hostContext),
                        cohort = cohort
                      ),
                      title = cohortToTitle(cohort)
                    )
                  )
                }
            }

        }
      }
    }

  private def form(emailAlreadyStored: Boolean, cohort: OptInCohort, email: Option[EmailAddress])(implicit
    hc: HostContext
  ): Form[?] =
    if (cohort.pageType == PageType.ReOptInPage)
      ReOptInDetailsForm().fill(
        ReOptInDetailsForm.Data(
          emailAddress = hc.email.map(EmailAddress.apply),
          preference = None,
          acceptedTcs = None,
          emailAlreadyStored = Some(emailAlreadyStored)
        )
      )
    else
      OptInDetailsForm().fill(
        OptInDetailsForm.Data(
          emailAddress = email,
          preference = None,
          acceptedTcs = None,
          emailAlreadyStored = Some(emailAlreadyStored)
        )
      )

  def displayCohort(cohort: Option[OptInCohort]): Action[AnyContent] =
    Action.async { implicit request =>
      cohort.fold(Future.successful(BadRequest("Invalid cohort"))) { cohort =>
        val form =
          cohort.pageType match {
            case PageType.IPage       => OptInDetailsForm()
            case PageType.ReOptInPage => ReOptInDetailsForm()
            case _                    => throw new Exception("Invalid cohort")
          }
        implicit val authRequest: AuthenticatedRequest[AnyContent] =
          AuthenticatedRequest[AnyContent](request, None, None, None, None)
        implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")
        Future.successful(
          Ok(
            layoutProvider(
              content = saPrintingPreference(
                emailForm = form,
                submitPrefsFormAction = internal.paperless.routes.ChoosePaperlessController.cohortNop(),
                cohort = cohort
              ),
              title = cohortToTitle(cohort)
            )
          )
        )
      }
    }

  def cohortNop(): Action[AnyContent] =
    Action.async {
      Future.successful(Ok(""))
    }

  def cohortList(): Action[AnyContent] =
    Action.async { _ =>
      Future.successful(Ok(OptInCohort.listCohortWrites.writes(OptInCohortConfigurationValues.availableValues)))
    }

  def submitForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => _ =>
        val cohort = calculateCohort(hostContext)
        val call = paperless.routes.ChoosePaperlessController.submitForm(hostContext)
        val lang = languageType(request.lang.code)
        val formwithErrors = returnToFormWithErrors(call, cohort, authRequest)

        hostContext.cohort match {
          case Some(c) if c.pageType == PageType.ReOptInPage => handleReOptInGeneric(formwithErrors, cohort, lang)
          case Some(c) if c.pageType == PageType.IPage       => handleGeneric(formwithErrors, cohort, lang)
          case None                                          => handleGeneric(formwithErrors, cohort, lang)
          case _ =>
            throw new Exception(s"Unhandled cohort in submitForm(): ${hostContext.cohort}")
        }
      }
    }

  private def handleGeneric(formwithErrors: Form[?] => Future[Result], cohort: OptInCohort, lang: Language)(implicit
    request: AuthenticatedRequest[?],
    hostContext: HostContext
  ): Future[Result] =
    OptInOrOutForm()
      .bindFromRequest()(request, FormBinding.Implicits.formBinding)
      .fold[Future[Result]](
        formwithErrors,
        happyForm =>
          if (happyForm.optedIn.contains(false))
            saveAndAuditPreferences(
              SaveAndAuditPreferences(
                digital = false,
                email = None,
                cohort,
                emailAlreadyStored = false,
                languagePreference = Some(lang),
                if (hostContext.survey) Some(SurveyType.StandardInterruptOptOut)
                else None
              )
            )
          else
            OptInDetailsForm()
              .bindFromRequest()(request, FormBinding.Implicits.formBinding)
              .fold[Future[Result]](
                hasErrors = formwithErrors,
                success = {
                  case emailForm @ OptInDetailsForm
                        .Data((Some(emailAddress), _), _, Some(OptedIn), Some(true), _) =>
                    validateEmailAndSavePreference(
                      EmailAndSavePreference(
                        emailAddress,
                        emailForm.isEmailVerified,
                        emailForm.isEmailAlreadyStored,
                        cohort,
                        languagePreference = Some(lang)
                      )
                    )
                  case _ =>
                    formwithErrors(OptInDetailsForm().bindFromRequest())
                }
              )
      )

  private def handleReOptInGeneric(
    formwithErrors: Form[?] => Future[Result],
    cohort: OptInCohort,
    lang: Language
  )(implicit request: AuthenticatedRequest[?], hostContext: HostContext): Future[Result] =
    ReOptInOrOutForm()
      .bindFromRequest()(request, FormBinding.Implicits.formBinding)
      .fold[Future[Result]](
        hasErrors = formwithErrors,
        happyForm =>
          if (happyForm.optedIn.contains(false))
            saveAndAuditPreferences(
              SaveAndAuditPreferences(
                digital = false,
                email = None,
                cohort,
                emailAlreadyStored = false,
                languagePreference = Some(lang)
              )
            )
          else
            ReOptInDetailsForm()
              .bindFromRequest()(request, FormBinding.Implicits.formBinding)
              .fold[Future[Result]](
                hasErrors = formwithErrors,
                success = {
                  case _ @ReOptInDetailsForm
                        .Data((Some(emailAddress), _), _, Some(OptedIn), Some(true), _) =>
                    validateEmailAndSavePreference(
                      EmailAndSavePreference(
                        emailAddress = emailAddress,
                        isEmailVerified = true,
                        isEmailAlreadyStored = true,
                        cohort = cohort,
                        languagePreference = Some(lang)
                      )
                    )
                  case _ =>
                    formwithErrors(ReOptInDetailsForm().bindFromRequest())
                }
              )
      )

  def displayOptOutConfirmation(digital: Boolean, hostContext: model.HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => _ =>
        implicit val hostContextImpl: HostContext = hostContext

        Future.successful(
          Ok(
            layoutProvider(
              content = saOptOutConfirmation(
                hostContext.isItsa,
                internal.paperless.routes.ChoosePaperlessController.submitOptOutConfirmation(digital, hostContext)
              ),
              title = titleKeyForPageAndRegime(
                pageIdentifier = OptOutConfirmation,
                regime = hostContext.regime.getOrElse(EMPTY_STRING)
              )
            )
          )
        )
      }
    }

  def submitOptOutConfirmation(
    digital: Boolean,
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { (_: AuthenticatedRequest[?]) => implicit hc =>
        implicit val hostContextImpl: HostContext = hostContext
        val cohort = hostContext.cohort.getOrElse(CohortCurrent.reoptinpage)
        val surveyType =
          if (hostContext.survey)
            Some(SurveyType.StandardInterruptOptOut)
          else None

        if (cohort.pageType == PageType.ReOptInPage)
          if (!digital && !hostContext.isItsa)
            Future.successful(Redirect(routes.SurveyController.displayReOptInDeclinedSurveyForm(hostContext)))
          else
            getReturnResult
        else if (surveyType.isDefined && !hostContext.isItsa)
          Future.successful(Redirect(routes.SurveyController.displayOptinDeclinedSurveyForm(hostContext)))
        else
          getReturnResult
      }
    }

  private def validateEmailAndSavePreference(
    es: EmailAndSavePreference
  )(implicit request: AuthenticatedRequest[?], hostContext: HostContext, hc: HeaderCarrier): Future[Result] = {
    val emailVerificationStatus =
      if (es.isEmailVerified) true
      else emailValidation.isValid(es.emailAddress)

    if (emailVerificationStatus)
      saveAndAuditPreferences(
        SaveAndAuditPreferences(
          digital = true,
          email = Some(es.emailAddress),
          cohort = es.cohort,
          emailAlreadyStored = es.isEmailAlreadyStored,
          languagePreference = es.languagePreference
        )
      )
    else
      Future.successful(
        Ok(
          makeDefinedLayoutProvider(es.emailAddress, es.cohort)
        )
      )
  }

  private def makeDefinedLayoutProvider(emailAddress: String, cohort: OptInCohort)(implicit
    request: AuthenticatedRequest[?],
    hostContext: HostContext
  ) =
    layoutProvider(
      content = saPrintingPreferenceVerifyEmail(
        emailAddress,
        cohort,
        controllers.internal.paperless.routes.ChoosePaperlessController.submitForm(hostContext),
        controllers.internal.paperless.routes.ChoosePaperlessController
          .redirectToDisplayFormWithCohort(Some(Encrypted(EmailAddress(emailAddress))), hostContext)
          .url
      ),
      title = "sa_printing_preferences.confirm_correct_email.title"
    )

  private def auditPageShown(
    journey: Journey,
    cohort: OptInCohort
  )(implicit request: AuthenticatedRequest[?], hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendMergedEvent(
      MergedDataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = EventTypes.Succeeded,
        request = DataCall(
          tags = hc.toAuditTags("Show Print Preference Option", request.path),
          detail = hc.toAuditDetails(
            "utr"     -> request.saUtr.getOrElse("N/A"),
            "nino"    -> request.nino.getOrElse("N/A"),
            "journey" -> journey.toString,
            "cohort"  -> cohort.toString
          ),
          generatedAt = Instant.now()
        ),
        response = DataCall(
          tags = hc.toAuditTags("Show Print Preference Option", request.path),
          detail = hc.toAuditDetails(
            "utr"     -> request.saUtr.getOrElse("N/A"),
            "nino"    -> request.nino.getOrElse("N/A"),
            "journey" -> journey.toString,
            "cohort"  -> cohort.toString
          ),
          generatedAt = Instant.now()
        )
      )
    )

  def displayNearlyDone(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => _ =>
        implicit val hostContextImplicit: HostContext = hostContext
        Future.successful(
          Ok(
            layoutProvider(
              content = accountDetailsPrintingPreferenceConfirm(
                calculateCohort(hostContext),
                emailAddress.map(_.decryptedValue)
              ),
              title = "sa_printing_preference_confirm.title"
            )
          )
        )
      }
    }

  def displayLanguageForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        preferencesConnector.getPreferences() map { pref =>
          Ok(
            layoutProvider(
              content = changeLanguage(
                languageForm =
                  LanguageForm().fill(LanguageForm.Data(language = pref.map(_.lang().exists(_ == Language.Welsh)))),
                submitLanguageFormAction =
                  internal.paperless.routes.ChoosePaperlessController.submitLanguageForm(hostContext)
              ),
              title = "account.details.change.language.title"
            )
          )
        }
      }
    }

  def submitLanguageForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { (authRequest: AuthenticatedRequest[?]) => implicit hc =>
        LanguageForm()
          .bindFromRequest()(authRequest, FormBinding.Implicits.formBinding)
          .fold[Future[Result]](
            _ => Future.successful(BadRequest("Unable to submit the form.")),
            happyForm => {
              val lang = happyForm.language.fold[Language](Language.English)(isWelsh =>
                if (isWelsh) Language.Welsh else Language.English
              )
              preferencesConnector
                .changeEmailLanguage(TermsAndConditionsUpdate.fromLanguage(Some(lang)))
                .map { _ =>
                  Redirect(routes.ManagePaperlessController.checkSettings(hostContext))
                }
            }
          )
      }
    }

  private def unbind(hc: HostContext): String = HostContext.hostContextBinder.unbind("anyValName", hc)

  def capture(hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[?] => implicit hc: HeaderCarrier =>
        logger.warn(s"Received host context $hostContext")

        def processSilentRedirect(entityId: String) = {
          val queryParams = unbind(hostContext.copy(entityId = Some(entityId)))
          logger.warn(s"Silently redirecting to ${hostContext.returnUrl + "?" + queryParams} for entityId: $entityId")
          Redirect(hostContext.returnUrl + "?" + queryParams)
        }

        def processEmailVerification(email: String) = {
          val _hc = hostContext.copy(email = Some(email))
          Redirect(paperless.routes.EmailController.processEmailReVerificationJourney(_hc))
        }

        def processEmailBounce(email: String) = {
          val _hc = hostContext.copy(email = Some(email))
          Redirect(paperless.routes.EmailController.processEmailBounceJourney(_hc))
        }

        def processReOptin(email: Option[EmailPreference]) =
          Redirect(
            paperless.routes.ChoosePaperlessController.displayForm(
              Some(CohortCurrent.reoptinpage),
              None,
              hostContext.copy(cohort = Some(CohortCurrent.reoptinpage), email = email.map(_.email))
            )
          )

        getJourney(
          hostContext,
          {
            case Some(SilentRedirectJourney(entityId, _, _)) => processSilentRedirect(entityId)
            case Some(c: ConflictJourney)                    => Conflict(Json.toJson(c))
            case Some(_: OptInJourney) =>
              Redirect(paperless.routes.OptInController.displayOptIn(hostContext))
            case Some(ReOptInJourney(_, email, _))           => processReOptin(email)
            case Some(ReOptInModifiedJourney(_, email, _))   => processReOptin(email)
            case Some(EmailVerificationJourney(_, email, _)) => processEmailVerification(email)
            case Some(BounceEmailJourney(_, email, _))       => processEmailBounce(email)
            case _ =>
              val queryParams = unbind(hostContext.copy(regime = Some("itsa")))
              logger.warn(s"test helper itsa encrypted: $queryParams")
              BadRequest("This feature is disabled")
          },
          auditEnabled = true
        )
      }
    }

  // --------------------------

  def preCheck(): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { (_: AuthenticatedRequest[?]) => implicit hc: HeaderCarrier =>
        precheckService.determineJourney() map {
          case silent: SilentRedirectJourney => Ok(Json.toJson(silent))
          case e: EmailVerificationJourney   => Ok(Json.toJson(e))
          case conflict: ConflictJourney     => Conflict(Json.toJson(conflict))
          case o: OptInJourney               => Ok(Json.toJson(o))
          case anyOther                      => BadRequest(Json.toJson(anyOther))
        }
      }
    }
}

private case class EmailAndSavePreference(
  emailAddress: String,
  isEmailVerified: Boolean,
  isEmailAlreadyStored: Boolean,
  cohort: OptInCohort,
  languagePreference: Some[Language]
)
