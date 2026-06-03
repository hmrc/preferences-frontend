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
import controllers.LayoutProvider
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import model.HostContext
import play.api.data.FormBinding
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ EventTypes, ExtendedDataEvent }
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.sca.services.WrapperService

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class SurveyController @Inject() (
  auditConnector: AuditConnector,
  val authConnector: AuthConnector,
  val wrapperService: WrapperService,
  val appConfig: AppConfig,
  reOptinDeclinedSurvey: views.html.sa.prefs.surveys.reoptin_declined_survey,
  optoutPaperlessSurvey: views.html.sa.prefs.surveys.optout_paperless_survey,
  optinDeclinedSurvey: views.html.sa.prefs.surveys.optin_declined_survey,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with LayoutProvider with I18nSupport with WithAuthRetrievals with LanguageHelper {

  def displayReOptInDeclinedSurveyForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => _ =>
        Future.successful(
          Ok(
            layoutProvider(
              content = reOptinDeclinedSurvey(
                surveyForm = SurveyReOptInDeclinedDetailsForm(),
                submitSurveyFormAction =
                  controllers.internal.routes.SurveyController.submitReOptInDeclinedSurveyForm(hostContext)
              ),
              title = "paperless.survey.reoptin_declined.title"
            )
          )
        )
      }
    }

  def submitReOptInDeclinedSurveyForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => _ =>
        val form = SurveyReOptInDeclinedDetailsForm().bindFromRequest()(request, FormBinding.Implicits.formBinding)
        (form.hasErrors, form.data.get("submissionType")) match {
          case (true, Some("submitted")) =>
            Future.successful(
              BadRequest(
                layoutProvider(
                  content =
                    reOptinDeclinedSurvey(form, routes.SurveyController.submitReOptInDeclinedSurveyForm(hostContext)),
                  title = "paperless.survey.reoptin_declined.title"
                )
              )
            )
          case (_, _) =>
            auditSurvey(
              "Re-OptIn Declined Survey Answered",
              languageType(request.lang.code).toString,
              SurveyReOptInDeclinedDetailsForm.choices,
              form.data,
              "paperless.survey.reoptin_declined.choice.",
              SurveyReOptInDeclinedDetailsForm.reasonMaxLength
            )
            Future.successful(Redirect(hostContext.returnUrl))
        }
      }
    }

  def displayOptinDeclinedSurveyForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => _ =>
        Future.successful(
          Ok(
            layoutProvider(
              content = optinDeclinedSurvey(
                surveyForm = SurveyOptinDeclinedDetailsForm(),
                submitSurveyFormAction = routes.SurveyController.submitOptinDeclinedSurveyForm(hostContext)
              ),
              title = "paperless.survey.optin_declined.title"
            )
          )
        )
      }
    }

  def submitOptinDeclinedSurveyForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => _ =>
        val form = SurveyOptinDeclinedDetailsForm().bindFromRequest()(request, FormBinding.Implicits.formBinding)
        (form.hasErrors, form.data.get("submissionType")) match {
          case (true, Some("submitted")) =>
            Future.successful(
              BadRequest(
                layoutProvider(
                  content =
                    optinDeclinedSurvey(form, routes.SurveyController.submitOptinDeclinedSurveyForm(hostContext)),
                  title = "paperless.survey.optin_declined.title"
                )
              )
            )
          case (_, _) =>
            auditSurvey(
              "OptIn Declined Survey Answered",
              languageType(request.lang.code).toString,
              SurveyOptinDeclinedDetailsForm.choices,
              form.data,
              "paperless.survey.optin_declined.choice.",
              SurveyOptinDeclinedDetailsForm.reasonMaxLength
            )
            Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
        }
      }
    }

  def displayOptoutSurvey(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => _ =>
        Future.successful(
          Ok(
            layoutProvider(
              content = optoutPaperlessSurvey(
                SurveyOptoutDetailsForm(),
                routes.SurveyController.submitOptoutSurvey(hostContext)
              ),
              title = "paperless.survey.optout.title"
            )
          )
        )
      }
    }

  def submitOptoutSurvey(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[?] => _ =>
        val form = SurveyOptoutDetailsForm().bindFromRequest()(request, FormBinding.Implicits.formBinding)
        (form.hasErrors, form.data.get("submissionType")) match {
          case (true, Some("submitted")) =>
            Future.successful(
              BadRequest(
                layoutProvider(
                  content = optoutPaperlessSurvey(form, routes.SurveyController.submitOptoutSurvey(hostContext)),
                  title = "paperless.survey.optout.title"
                )
              )
            )
          case (_, subType) =>
            auditSurvey(
              s"""Manual OptOut Survey ${subType
                  .map(s => if (s == "submitted") "Answered" else "Not Answered")
                  .getOrElse("Answered")}""",
              languageType(request.lang.code).toString,
              SurveyOptoutDetailsForm.choices,
              form.data,
              "paperless.survey.optout.choice.",
              SurveyOptoutDetailsForm.reasonMaxLength
            )
            Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
        }
      }
    }

  def auditSurvey(
    transactionName: String,
    languagePreference: String,
    choices: List[String],
    data: Map[String, String],
    msgPrefix: String,
    reasonLength: Int
  )(implicit request: AuthenticatedRequest[?]): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = AppName.fromConfiguration(appConfig.configuration),
        auditType = EventTypes.Succeeded,
        tags = Map(EventKeys.TransactionName -> transactionName),
        detail = Json.toJson(
          EventDetail(
            data.getOrElse("submissionType", "N/A"),
            request.saUtr.getOrElse("N/A"),
            request.nino.getOrElse("N/A"),
            languagePreference,
            (choices map { c =>
              s"choice-$c" -> QuestionAnswer(
                question = messagesApi(s"$msgPrefix$c", "N/A")(
                  request.lang
                ),
                answer = data.getOrElse(s"choice-$c", "false")
              )
            }).toMap,
            data.get("reason").fold("N/A")(r => if (r.length > reasonLength) r.substring(0, reasonLength) else r)
          )
        )
      )
    )
}
