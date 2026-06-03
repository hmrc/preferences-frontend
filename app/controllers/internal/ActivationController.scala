/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors._
import controllers.ExternalUrlPrefixes
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import model.{ FormType, HostContext, Language }
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ ExecutionContext, Future }

class ActivationController @Inject() (
  preferencesConnector: PreferencesConnector,
  val authConnector: AuthConnector,
  externalUrlPrefixes: ExternalUrlPrefixes,
  mcc: MessagesControllerComponents,
  config: Configuration
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with WithAuthRetrievals with I18nSupport with LanguageHelper
    with ActivationResponseTrait {

  val hostUrl = externalUrlPrefixes.pfUrlPrefix

  private lazy val gracePeriod =
    config
      .getOptional[Long](s"activation.gracePeriodInMin")
      .getOrElse(throw new RuntimeException(s"missing activation.gracePeriodInMin"))

  def preferences(): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { (_: AuthenticatedRequest[?]) => implicit hc: HeaderCarrier =>
        preferencesConnector.getPreferences().map {
          case Some(preference) => Ok(Json.toJson(preference))
          case _                => NotFound
        }
      }
    }

  def activate(hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { (_: AuthenticatedRequest[?]) => implicit hc: HeaderCarrier =>
        val terms = hostContext.termsAndConditions.getOrElse("generic")
        for {
          preferenceStatus <- preferencesConnector.getPreferencesStatus(terms)
          lang = languageType(request.lang.code)
          _ <- storeLanguagePreference(hostContext, lang, preferenceStatus)
        } yield preferencesStatusResult(hostUrl, gracePeriod: Long, hostContext, preferenceStatus)
      }
    }

  def activateLegacyFromTaxIdentifier(
    @unused formType: FormType,
    @unused taxIdentifier: String,
    hostContext: HostContext
  ): Action[AnyContent] = activate(hostContext)

  private def storeLanguagePreference(
    hostContext: HostContext,
    lang: Language,
    preferenceStatus: Either[Int, PreferenceStatus]
  )(implicit hc: HeaderCarrier): Future[Unit] =
    preferenceStatus match {
      case Right(PreferenceFound(true, emailPreference, _, _, _, _)) if emailPreference.exists(_.language.isEmpty) =>
        preferencesConnector
          .changeEmailLanguage(TermsAndConditionsUpdate.fromLanguage(Some(lang)))(hc, hostContext, ec)
          .map(_ => ())
      case _ => Future.successful(())
    }
}
