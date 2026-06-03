/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import config.AppConfig
import connectors.{ EntityResolverConnector, PreferencesConnector }
import controllers.LayoutProvider
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import controllers.internal.{ LanguageHelper, OptInCohortCalculator }
import model.{ HostContext, JourneyType }
import model.JourneyType.{ Bounce, EmailReVerify }
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import service.PreCheckService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ DataCall, EventTypes, MergedDataEvent }
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.sca.services.WrapperService

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class EmailController @Inject() (
  val entityResolverConnector: EntityResolverConnector,
  val preferencesConnector: PreferencesConnector,
  val precheckService: PreCheckService,
  val authConnector: AuthConnector,
  val auditConnector: AuditConnector,
  val configuration: Configuration,
  val wrapperService: WrapperService,
  val appConfig: AppConfig,
  val saPrintingPreference: views.html.sa.prefs.sa_printing_preference,
  saEmailResentConfirmation: views.html.sa.prefs.sa_printing_preference_email_resent_confirmation,
  saPrintingPreferenceReOptinConfirmation: views.html.sa.prefs.sa_printing_preference_reoptin_confirmation,
  saEmailReVerification: views.html.sa.prefs.sa_printing_preference_email_re_verification,
  saEmailBounceForm: views.html.sa.prefs.sa_printing_preference_email_bounce_page,
  mcc: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) with OptInCohortCalculator with LayoutProvider with I18nSupport
    with WithAuthRetrievals with LanguageHelper with PaperlessCommon {

  def processEmailReVerificationJourney(hc: model.HostContext): Action[AnyContent] =
    processEmailJourney(hc, EmailReVerify)

  def processEmailBounceJourney(hc: model.HostContext): Action[AnyContent] =
    processEmailJourney(hc, Bounce)

  def resendVerificationEmail(hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => implicit hc =>
        val jouurney = hostContext.journey
        implicit val hostContextImpl: HostContext = hostContext.copy(journey = None)
        hostContext.email match {
          case Some(email) =>
            preferencesConnector
              .changeEmailAddress(email, jouurney)
              .map(_ =>
                Ok(
                  layoutProvider(
                    content = saEmailResentConfirmation(email),
                    title = "sa_printing_preference.sps_email_confirm"
                  )
                )
              )
          case _ => Future.successful(BadRequest("Email address doesn't exist"))
        }
      }
    }

  def displayEmailConfirmation(hostContext: model.HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => _ =>
        implicit val hostContextImpl: HostContext = hostContext
        Future.successful(hostContext.email match {
          case Some(email) =>
            Ok(
              layoutProvider(
                content = saPrintingPreferenceReOptinConfirmation(email),
                title = "sa_printing_preference.sps_email_confirm"
              )
            )
          case None => BadRequest("Email address doesnt't exist")
        })
      }
    }

  def updateEmailVerificationStatus(
    journey: String,
    hostContext: model.HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => implicit hc =>
        implicit val hostContextImpl: HostContext = hostContext
        hostContext.email.map(auditEmailVerificationJourney(_, journey))
        getReturnResult
      }
    }

  private def processEmailJourney(hostContext: model.HostContext, journey: JourneyType): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[?] => _ =>
        implicit val hostContextImpl: HostContext = hostContext
        Future.successful((hostContext.email, journey) match {
          case (Some(email), EmailReVerify) =>
            Ok(
              layoutProvider(
                content = saEmailReVerification(email),
                title = "sa_printing_preference.sps_email_confirm"
              )
            )
          case (Some(email), Bounce) =>
            Ok(
              layoutProvider(
                content = saEmailBounceForm(email),
                title = "sa_printing_preference.sps_email_confirm"
              )
            )
          case _ => BadRequest("Email address doesnt't exist")
        })
      }
    }

  private def auditEmailVerificationJourney(email: String, journey: String)(implicit
    request: AuthenticatedRequest[?],
    hc: HeaderCarrier
  ): Future[AuditResult] =
    auditConnector.sendMergedEvent(
      MergedDataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = EventTypes.Succeeded,
        request = DataCall(
          tags = hc.toAuditTags("Email Verification", request.path),
          detail = hc.toAuditDetails(
            "utr"     -> request.saUtr.getOrElse("N/A"),
            "nino"    -> request.nino.getOrElse("N/A"),
            "journey" -> journey,
            "email"   -> email,
            "status"  -> "Close Email Confirmation Page"
          ),
          generatedAt = Instant.now()
        ),
        response = DataCall(
          tags = hc.toAuditTags("Email Verification", request.path),
          detail = hc.toAuditDetails(
            "utr"     -> request.saUtr.getOrElse("N/A"),
            "nino"    -> request.nino.getOrElse("N/A"),
            "journey" -> journey,
            "email"   -> email,
            "status"  -> "Close Email Confirmation Page"
          ),
          generatedAt = Instant.now()
        )
      )
    )

}
