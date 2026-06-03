/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import connectors.{ EntityResolverConnector, OptInPage, PreferenceFound, PreferenceNotFound, PreferencesConnector, PreferencesCreated, PreferencesFailure, PreferencesStatus, TermsAccepted, TermsAndConditionsUpdate, TermsType }
import controllers.PageIdentifier.{ OptInConfirmation, OptInMail, OptOutConfirmation, PageIdentifier }
import controllers.{ EMPTY_STRING, LayoutProvider, REGIME_ITSA }
import controllers.auth.AuthenticatedRequest
import controllers.internal.EmailOptInJourney.{ AccountDetails, Journey }
import controllers.internal.*
import model.{ BounceEmailJourney, ConflictJourney, EmailVerificationJourney, Encrypted, HostContext, JourneyDC, JourneyTypeDC, Language, PageType, ReOptInJourney, ReOptInModifiedJourney, SurveyType }
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{ Call, Result }
import play.api.mvc.Results.{ BadRequest, Redirect }
import service.PreCheckService
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ DataCall, DataEvent, EventTypes, MergedDataEvent }
import uk.gov.hmrc.play.bootstrap.config.AppName

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

trait PaperlessCommon extends LayoutProvider with I18nSupport {

  implicit val ec: ExecutionContext
  val auditConnector: AuditConnector
  val configuration: Configuration
  val entityResolverConnector: EntityResolverConnector
  val saPrintingPreference: views.html.sa.prefs.sa_printing_preference
  val precheckService: PreCheckService
  val preferencesConnector: PreferencesConnector

  def hasStoredEmail(hostContext: HostContext)(implicit
    hc: HeaderCarrier
  ): Future[Boolean] = {
    val terms = hostContext.termsAndConditions.getOrElse("generic")
    val f: Any => Boolean = {
      case Right(PreferenceNotFound(Some(_))) | Right(PreferenceFound(_, Some(_), _, _, _, _)) => true
      case _                                                                                   => false
    }
    preferencesConnector.getPreferencesStatus(terms) map f
  }

  // scalastyle:off cyclomatic.complexity
  def cohortToTitle(cohort: OptInCohort, hasError: Boolean = false, regime: String = EMPTY_STRING): String =
    cohort match {
      case IPage7                                       => "i_page7.fg_page.title"
      case IPage8                                       => "i_page8.fg_page.title"
      case IPage53 if hasError                          => "i_page53.fg_page.title.error"
      case IPage53                                      => "i_page53.fg_page.title"
      case IPage56 if hasError && regime == REGIME_ITSA => "i_page56.fg_page.itsa.title.error"
      case IPage56 if hasError                          => "i_page56.fg_page.title.error"
      case IPage56 if regime == REGIME_ITSA             => "i_page56.fg_page.itsa.title"
      case IPage56                                      => "i_page56.fg_page.title"
      case ReOptInPage10                                => "reoptin_page10.fg_page.title"
      case ReOptInPage52                                => "reoptin_page52.fg_page.title"
      case ReOptInPage54 if hasError                    => "reoptin_page54.fg_page.title"
      case ReOptInPage54                                => "reoptin_page54.fg_page.title"
      case ReOptInPage55 if hasError                    => "reoptin_page55.fg_page.title"
      case ReOptInPage55                                => "reoptin_page55.fg_page.title"
      case _                                            => "i_page53.fg_page.title"
    }
  // scalastyle:on

  def titleKeyForPageAndRegime(
    pageIdentifier: PageIdentifier,
    hasError: Boolean = false,
    regime: String = EMPTY_STRING
  ): String =
    (pageIdentifier, regime) match {
      case (OptInConfirmation, "itsa")     => "sa_printing_preference.sps_email_confirm.itsa.title"
      case (OptInConfirmation, _)          => "sa_printing_preference.sps_email_confirm"
      case (OptOutConfirmation, "itsa")    => "sa_printing_preference.sps.itsa.opt_out_by_post_confimation_title"
      case (OptOutConfirmation, _)         => "sa_printing_preference.sps.opt_out_by_post_confimation_title"
      case (OptInMail, "itsa") if hasError => "sa_printing_preference.itsa.sps_opt_in_email_error"
      case (OptInMail, "itsa")             => "sa_printing_preference.itsa.sps_opt_in_email"
      case (OptInMail, _) if hasError      => "sa_printing_preference.sps_opt_in_email_error"
      case (OptInMail, _)                  => "sa_printing_preference.sps_opt_in_email"
    }

  def saveAndAuditPreferences(
    sa: SaveAndAuditPreferences
  )(implicit request: AuthenticatedRequest[?], hostContext: HostContext, hc: HeaderCarrier): Future[Result] = {
    val terms = sa.cohort.terms -> TermsAccepted(sa.digital, Some(OptInPage.from(sa.cohort)), sa.surveyType)
    val jouurney = hostContext.journey

    val ret =
      if (sa.digital)
        preferencesConnector
          .optIn(
            TermsAndConditionsUpdate
              .from(terms, sa.email, sa.languagePreference, jouurney)
          )
      else
        preferencesConnector
          .optOut(
            TermsAndConditionsUpdate
              .from(terms, sa.email, sa.languagePreference, jouurney)
          )
    ret.flatMap { preferencesStatus =>
      auditChoice(AccountDetails, sa.cohort, terms, sa.email, preferencesStatus)
      if (sa.cohort.pageType == PageType.ReOptInPage || sa.surveyType.isDefined)
        Future.successful(
          Redirect(
            controllers.internal.paperless.routes.ChoosePaperlessController
              .displayOptOutConfirmation(sa.digital, hostContext.copy(journey = None, cohort = Some(sa.cohort)))
          )
        )
      else if (sa.digital && !sa.emailAlreadyStored) {
        val encryptedEmail = sa.email map (emailAddress => Encrypted(EmailAddress(emailAddress)))
        Future.successful(
          Redirect(
            controllers.internal.paperless.routes.ChoosePaperlessController
              .displayNearlyDone(encryptedEmail, hostContext.copy(journey = None))
          )
        )
      } else
        getReturnResult
    }
  }

  def auditChoice(
    journey: Journey,
    cohort: OptInCohort,
    terms: (TermsType, TermsAccepted),
    emailOption: Option[String],
    preferencesStatus: PreferencesStatus,
    regime: Option[String] = None
  )(implicit request: AuthenticatedRequest[?], hc: HeaderCarrier): Future[AuditResult] = {
    val auditDets: Map[String, String] = hc.toAuditDetails(
      "client"                    -> "YTA",
      "utr"                       -> request.saUtr.getOrElse("N/A"),
      "nino"                      -> request.nino.getOrElse("N/A"),
      "journey"                   -> journey.toString,
      "digital"                   -> terms._2.accepted.toString,
      "cohort"                    -> cohort.toString,
      "TandCsScope"               -> terms._1.toString.toLowerCase,
      "userConfirmedReadTandCs"   -> terms._2.accepted.toString,
      "email"                     -> emailOption.getOrElse(""),
      "newUserPreferencesCreated" -> (preferencesStatus == PreferencesCreated).toString
    )
    val detail = regime.fold(auditDets)(r => auditDets + ("regime" -> r))
    val data = DataCall(
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = detail,
      generatedAt = Instant.now()
    )

    auditConnector.sendMergedEvent(
      MergedDataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = if (preferencesStatus == PreferencesFailure) EventTypes.Failed else EventTypes.Succeeded,
        request = data,
        response = data
      )
    )
  }

  def getReturnResult(implicit hc: HeaderCarrier, hostContext: HostContext): Future[Result] =
    if (hostContext.isItsa)
      preferencesConnector.getPreferences() map {
        case Some(pref) =>
          Redirect(hostContext.returnUrl + "?" + unbind(hostContext.copy(entityId = pref.entityId)))
        case None =>
          throw new Exception(s"Unhandled None scenario in getReturnResult(...)")
      }
    else
      Future.successful(Redirect(hostContext.returnUrl))

  private def unbind(hc: HostContext): String = HostContext.hostContextBinder.unbind("anyValName", hc)

  def returnToFormWithErrors(submitPrefsFormAction: Call, cohort: OptInCohort, request: AuthenticatedRequest[?])(
    form: Form[?]
  )(implicit hostContext: HostContext): Future[Result] = {
    implicit val req: AuthenticatedRequest[?] = request
    Future.successful(
      BadRequest(
        layoutProvider(
          saPrintingPreference(form, submitPrefsFormAction, cohort),
          title = cohortToTitle(cohort, hasError = true, hostContext.regime.getOrElse(EMPTY_STRING))
        )
      )
    )
  }

  def getJourney[A](
    hostContext: HostContext,
    f: Option[JourneyDC] => A,
    auditEnabled: Boolean = false
  )(implicit request: AuthenticatedRequest[?], hc: HeaderCarrier): Future[A] = {
    val maybeJourney = precheckService.determineJourney().map { journey =>
      if (auditEnabled && hostContext.isItsa)
        auditPageShownFor(journey, hostContext.regime, captureAuditTypeForJourney(journey))
      Some(journey)
    }
    maybeJourney map f
  }

  private def auditPageShownFor(journey: JourneyDC, regime: Option[String], auditType: String)(implicit
    request: AuthenticatedRequest[?],
    hc: HeaderCarrier
  ): Future[AuditResult] =
    auditConnector.sendEvent(
      DataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = auditType,
        tags = hc.toAuditTags(transactionNameOf(journey.journeyType), request.path),
        detail = hc.toAuditDetails(
          "utr"    -> request.saUtr.getOrElse("N/A"),
          "nino"   -> request.nino.getOrElse("N/A"),
          "regime" -> regime.getOrElse("N/A"),
          "email"  -> emailOf(journey).getOrElse("N/A"),
          "reason" -> journey.reason,
          "type"   -> journey.journeyType.toString
        ),
        generatedAt = Instant.now()
      )
    )

  private def captureAuditTypeForJourney(journey: JourneyDC): String =
    journey match {
      case _: ConflictJourney => "CustomerEnrolmentCaptureError"
      case _                  => "CustomerEnrolmentCapture"
    }

  private val transactionNameOf: Map[JourneyTypeDC, String] = {
    import JourneyTypeDC._
    Map(
      SilentRedirect    -> "Silent redirect user journey",
      OptIn             -> "Show print preferences",
      BounceEmail       -> "Show bounce page",
      EmailVerification -> "Show verify your email address",
      ReOptIn           -> "Show print preferences",
      ReOptInModified   -> "Show print preferences"
    )
  }

  // It extracts the email address from the given jouney object (depending on the concrete type)
  private def emailOf(journey: JourneyDC): Option[String] =
    journey match {
      case ReOptInJourney(_, email, _)           => email.map(_.email)
      case ReOptInModifiedJourney(_, email, _)   => email.map(_.email)
      case EmailVerificationJourney(_, email, _) => Some(email)
      case BounceEmailJourney(_, email, _)       => Some(email)
      case _                                     => None
    }

}

case class SaveAndAuditPreferences(
  digital: Boolean,
  email: Option[String],
  cohort: OptInCohort,
  emailAlreadyStored: Boolean,
  languagePreference: Some[Language],
  surveyType: Option[SurveyType] = None
)
