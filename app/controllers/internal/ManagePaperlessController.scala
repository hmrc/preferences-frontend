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

package controllers.internal

import config.AppConfig
import connectors._
import controllers.LayoutProvider
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }

import javax.inject.Inject
import model.{ Encrypted, HostContext, Language }
import play.api.data.FormBinding
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Result }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailaddress.{ EmailAddress, EmailAddressValidation }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.sca.services.WrapperService
import views.html.manage._

import scala.concurrent.{ ExecutionContext, Future }

class ManagePaperlessController @Inject() (
  preferencesConnector: PreferencesConnector,
  emailValidation: EmailAddressValidation,
  val authConnector: AuthConnector,
  val wrapperService: WrapperService,
  val appConfig: AppConfig,
  optedBackIntoPaperThankYou: views.html.opted_back_into_paper_thank_you,
  accountDetailsVerificationEmailResentConfirmation: views.html.account_details_verification_email_resent_confirmation,
  confirmOptBackIntoPaper: views.html.confirm_opt_back_into_paper,
  accountDetailsUpdateEmailAddress: views.html.account_details_update_email_address,
  accountDetailsUpdateEmailAddressVerifyEmail: views.html.account_details_update_email_address_verify_email,
  accountDetailsUpdateEmailAddressThankYou: views.html.account_details_update_email_address_thank_you,
  digitalFalseFull: digital_false_full,
  digitalTrueBouncedFull: digital_true_bounced_full,
  digitalTrueVerifiedFull: digital_true_verified_full,
  digitalTruePendingFull: digital_true_pending_full,
  typesOfTaxLetters: views.html.sa.prefs.cohorts.types_of_online_tax_letters,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with OptInCohortCalculator with LayoutProvider with I18nSupport
    with WithAuthRetrievals with LanguageHelper {

  private[controllers] def displayStopPaperlessConfirmed(implicit
    request: AuthenticatedRequest[?],
    hostContext: HostContext
  ): Result =
    Ok(
      layoutProvider(
        content = optedBackIntoPaperThankYou(),
        title = "opted.back.into.paperless.title"
      )
    )

  private[controllers] def submitStopPaperless(
    lang: Some[Language]
  )(implicit hostContext: HostContext, hc: HeaderCarrier): Future[Result] =
    preferencesConnector
      .optOut(
        TermsAndConditionsUpdate.from(
          (GenericTerms, TermsAccepted(false, Some(OptInPage.from(CohortCurrent.cysConfirmPage)))),
          email = None,
          lang
        )
      )
      .map(_ => Redirect(routes.SurveyController.displayOptoutSurvey(hostContext)))

  private[controllers] def resendVerificationEmail(implicit
    request: AuthenticatedRequest[?],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Future[Result] =
    lookupCurrentEmail { email =>
      preferencesConnector
        .changeEmailAddress(email)
        .map(_ =>
          Ok(
            layoutProvider(
              content = accountDetailsVerificationEmailResentConfirmation(email),
              title = "account.details.verification.email.title"
            )
          )
        )
    }

  private[controllers] def displayStopPaperless(implicit
    request: AuthenticatedRequest[?],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Future[Result] =
    lookupCurrentEmail { email =>
      val updatedHostContext = hostContext.copy(email = Some(email))
      Future.successful(
        Ok(
          layoutProvider(
            content = confirmOptBackIntoPaper(email.obfuscated)(request, request2Messages(request), updatedHostContext),
            title = "account.opt.back.out.confirmation.title"
          )
        )
      )
    }

  private[controllers] def displayChangeEmailAddress(
    emailAddress: Option[Encrypted[EmailAddress]]
  )(implicit request: AuthenticatedRequest[?], hostContext: HostContext, hc: HeaderCarrier): Future[Result] =
    lookupCurrentEmail { email =>
      val updatedHostContext = hostContext.copy(email = Some(email))
      Future.successful(
        Ok(
          layoutProvider(
            content = accountDetailsUpdateEmailAddress(
              email,
              EmailForm().fill(EmailForm.Data(emailAddress.map(_.decryptedValue)))
            )(request, request2Messages(request), updatedHostContext),
            title = "account.details.update.email.title"
          )
        )
      )
    }

  private def lookupCurrentEmail(
    func: EmailAddress => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    preferencesConnector.getPreferences().flatMap {
      case p @ Some(PreferenceResponse(_, Some(email), _, _, _)) if p.exists(_.genericTermsAccepted) =>
        func(EmailAddress(email.email))
      case _ =>
        Future.successful(BadRequest("Could not find existing preferences."))
    }

  private[controllers] def submitChangeEmailAddress(implicit
    request: AuthenticatedRequest[?],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Future[Result] =
    lookupCurrentEmail { email =>
      EmailForm()
        .bindFromRequest()(request, FormBinding.Implicits.formBinding)
        .fold(
          errors =>
            Future.successful(
              BadRequest(
                layoutProvider(
                  content = accountDetailsUpdateEmailAddress(email, errors),
                  title = "account.details.update.email.title"
                )
              )
            ),
          emailForm => {
            val emailVerificationStatus =
              if (emailForm.isEmailVerified) true
              else emailValidation.isValid(emailForm.mainEmail)

            if (emailVerificationStatus) {
              val updatedHostContext = hostContext.copy(email = Some(emailForm.mainEmail))
              preferencesConnector
                .changeEmailAddress(emailForm.mainEmail, None)
                .map(_ =>
                  Redirect(routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(updatedHostContext))
                )
            } else
              Future.successful(
                Ok(
                  layoutProvider(
                    content = accountDetailsUpdateEmailAddressVerifyEmail(emailForm.mainEmail),
                    title = "account.details.update.email.verify.title"
                  )
                )
              )
          }
        )
    }

  private[controllers] def displayChangeEmailAddressConfirmed(implicit
    request: AuthenticatedRequest[?],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Future[Result] =
    lookupCurrentEmail(email =>
      Future.successful(
        Ok(
          layoutProvider(
            content = accountDetailsUpdateEmailAddressThankYou(email),
            title = "account.details.update.email.thank_you.title"
          )
        )
      )
    )

  def displayChangeEmailAddress(implicit
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[?] => implicit hc =>
        displayChangeEmailAddress(emailAddress)
      }
    }

  def submitChangeEmailAddress(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => implicit hc =>
        submitChangeEmailAddress
      }
    }

  def displayChangeEmailAddressConfirmed(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => implicit hc =>
        displayChangeEmailAddressConfirmed
      }
    }

  def displayStopPaperless(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => implicit hc =>
        displayStopPaperless
      }
    }

  def submitStopPaperless(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest {
        val lang = languageType(request.lang.code)
        (_: AuthenticatedRequest[?]) => implicit hc => submitStopPaperless(Some(lang))
      }
    }

  def displayStopPaperlessConfirmed(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => _ =>
        Future.successful(displayStopPaperlessConfirmed)
      }
    }

  def resendVerificationEmail(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => implicit hc =>
        resendVerificationEmail
      }
    }

  def checkSettings(hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => implicit hc =>
        preferencesConnector.getPreferences() map { pref =>
          Ok(pref match {
            case p @ Some(PreferenceResponse(_, Some(email), _, _, _)) if p.exists(_.genericTermsAccepted) =>
              implicit val updatedHostContext: HostContext = hostContext.copy(email = Some(email.email))
              (email.hasBounces, email.isVerified) match {
                case (true, _) =>
                  layoutProvider(
                    content = digitalTrueBouncedFull(email, returnLinkTextToMessagesKey())(
                      withAuthenticatedRequest,
                      Messages.implicitMessagesProviderToMessages(request),
                      updatedHostContext
                    ),
                    title = "sa_printing_preferences.confirm_correct_email.title"
                  )
                case (_, true) =>
                  layoutProvider(
                    content = digitalTrueVerifiedFull(email, returnLinkTextToMessagesKey())(
                      withAuthenticatedRequest,
                      Messages.implicitMessagesProviderToMessages(request),
                      updatedHostContext
                    ),
                    title = "sa_printing_preferences.confirm_correct_email.title"
                  )

                case _ =>
                  layoutProvider(
                    content = digitalTruePendingFull(email, returnLinkTextToMessagesKey())(
                      withAuthenticatedRequest,
                      Messages.implicitMessagesProviderToMessages(request),
                      updatedHostContext
                    ),
                    title = "sa_printing_preferences.confirm_correct_email.title"
                  )
              }
            case _ =>
              implicit val updatedHostContext: HostContext = hostContext.copy(email = None)
              layoutProvider(
                content = digitalFalseFull(returnLinkTextToMessagesKey()),
                title = "sa_printing_preferences.confirm_correct_email.title"
              )
          })
        }
      }
    }

  def displayTypesOfDigitalTaxLetters(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => _ =>
        Future.successful(
          Ok(
            layoutProvider(
              content = typesOfTaxLetters(),
              title = "types_of_online_tax_letters_h1"
            )
          )
        )
      }
    }

  private def returnLinkTextToMessagesKey()(implicit hostContext: HostContext): String =
    hostContext.returnLinkText match {
      case "Continue"                                    => "pta"
      case "Return to your business tax account details" => "bta"
      case "Continue to your Itsa Account"               => "itsa"
      case _                                             => "pta"
    }
}
