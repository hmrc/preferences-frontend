/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.{ EmailPreference, PreferenceFound, PreferenceNotFound, PreferenceStatus }
import model.SurveyType.StandardInterruptOptOut
import model.{ Encrypted, HostContext, Survey }
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{ Conflict, Ok, PreconditionFailed, Redirect, Status }
import uk.gov.hmrc.emailaddress.EmailAddress

import java.time.Instant

trait ActivationResponseTrait {
  def preferencesStatusResult(
    hostUrl: String,
    gracePeriod: Long,
    hostContext: HostContext,
    preferenceStatus: Either[Int, PreferenceStatus]
  ): Result = {
    val createPreferencesResult = preferencesFoundStatusResult orElse preferencesNotFoundStatusResult
    createPreferencesResult((hostUrl, gracePeriod, hostContext, preferenceStatus))
  }

  private def preferencesFoundStatusResult
    : PartialFunction[(String, Long, HostContext, Either[Int, PreferenceStatus]), Result] = {
    case (_, _, hostContext, Right(PreferenceFound(true, _, _, _, _, _))) if isAlreadyOptedIn(hostContext) =>
      Redirect(hostContext.alreadyOptedInUrl.get)

    case (_, _, _, Right(PreferenceFound(true, emailPreference, _, None, _, _))) =>
      Ok(makeOptedInResponseBody(emailPreference))

    case (
          hostUrl,
          _,
          hostContext,
          Right(PreferenceFound(true, Some(emailPreference), _, Some(majorVersion), paperless, _))
        ) =>
      if (isRedirect(emailPreference, majorVersion, paperless))
        PreconditionFailed(Json.obj("redirectUserTo" -> makeRedirectUrl(hostUrl, hostContext, emailPreference)))
      else
        Ok(Json.obj("optedIn" -> true, "verifiedEmail" -> emailPreference.isVerified))

    // [TODO] [DC-3013] for PreferencesFound  add to hostcontext whether survey has to be presented
    case (hostUrl, gracePeriod, hostContext, Right(PreferenceFound(false, None, updatedAt, _, _, surveys))) =>
      constructResponse(hostUrl, gracePeriod, hostContext, updatedAt, surveys)

    case (_, _, _, Right(PreferenceFound(false, _, _, _, _, _))) =>
      Ok(Json.obj("optedIn" -> false))
  }

  private def preferencesNotFoundStatusResult
    : PartialFunction[(String, Long, HostContext, Either[Int, PreferenceStatus]), Result] = {
    case (_, _, hostContext: HostContext, Right(PreferenceNotFound(Some(email))))
        if isEmailMismatch(hostContext, email) =>
      Conflict

    case (hostUrl, _, hostContext, Right(PreferenceNotFound(email))) =>
      PreconditionFailed(Json.obj("redirectUserTo" -> makeNotFoundRedirectUrl(hostUrl, hostContext, email)))

    case (_, _, _, Left(status)) =>
      Status(status)

    case (_, _, _, preferenceStatus) =>
      throw new Exception(s"Unhandled case in preferencesStatusResult(...): $preferenceStatus")
  }

  private def isEmailMismatch(hostContext: HostContext, email: EmailPreference) =
    hostContext.email.exists(_ != email.email)

  private def constructResponse(
    hostUrl: String,
    gracePeriod: Long,
    hostContext: HostContext,
    updatedAt: Option[Instant],
    surveys: Option[List[Survey]]
  ) =
    updatedAt match {
      case Some(u) if withinGracePeriod(gracePeriod, u) =>
        Ok(Json.obj("optedIn" -> false))
      case _ =>
        PreconditionFailed(Json.obj("redirectUserTo" -> makeRedirectUrl(hostUrl, hostContext, surveys)))
    }

  private def makeNotFoundRedirectUrl(hostUrl: String, hostContext: HostContext, email: Option[EmailPreference]) =
    hostUrl + paperless.routes.ChoosePaperlessController
      .redirectToDisplayFormWithCohort(
        email.map(e => Encrypted(EmailAddress(e.email))),
        hostContext.copy(survey = true)
      )
      .url

  private def makeRedirectUrl(hostUrl: String, hostContext: HostContext, surveys: Option[List[Survey]]) =
    hostUrl + paperless.routes.ChoosePaperlessController
      .redirectToDisplayFormWithCohort(
        email = None,
        hostContext.copy(survey = maybeRequiresSurvey(surveys).getOrElse(true))
      )
      .url

  private def makeRedirectUrl(hostUrl: String, hostContext: HostContext, emailPreference: EmailPreference) =
    hostUrl + paperless.routes.ChoosePaperlessController
      .displayForm(
        Some(CohortCurrent.reoptinpage),
        email = None,
        hostContext.copy(cohort = Some(CohortCurrent.reoptinpage), email = Some(emailPreference.email))
      )
      .url

  private def maybeRequiresSurvey(surveys: Option[List[Survey]]) =
    surveys.collect { case ls => !ls.exists(_.surveyType == StandardInterruptOptOut) }

  private def withinGracePeriod(gracePeriod: Long, u: Instant) =
    u.plusSeconds(gracePeriod * 60).isAfter(Instant.now)

  private def isRedirect(emailPreference: EmailPreference, majorVersion: Int, paperless: Option[Boolean]): Boolean =
    isDisplayReOptIn(emailPreference, majorVersion, paperless) || isDoReOptInOptimisation(emailPreference, majorVersion)

  private def isDoReOptInOptimisation(emailPreference: EmailPreference, majorVersion: Int) =
    isVersionBehind(majorVersion) && (emailPreference.hasBounces && hasNoPendingEmail(emailPreference))

  private def isDisplayReOptIn(emailPreference: EmailPreference, majorVersion: Int, paperless: Option[Boolean]) =
    isVersionBehind(majorVersion) && hasNoPendingEmail(emailPreference) && isPaperless(paperless)

  private def isPaperless(paperless: Option[Boolean]) =
    paperless.exists(identity)

  private def hasNoPendingEmail(emailPreference: EmailPreference) =
    emailPreference.pendingEmail.isEmpty

  private def isVersionBehind(majorVersion: Int) =
    majorVersion < CohortCurrent.ipage.majorVersion

  private def makeOptedInResponseBody(emailPreference: Option[EmailPreference]) =
    Json.obj(
      "optedIn"       -> true,
      "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
    )

  private def isAlreadyOptedIn(hostContext: HostContext) =
    hostContext.alreadyOptedInUrl.isDefined
}
