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

package service

import com.google.inject.ImplementedBy
import connectors.{ EmailPreference, MultiplePreferenceFound, PreferenceNotFound, PreferenceResponse, PreferencesConnector }
import model._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

@ImplementedBy(classOf[PreferencesPreCheckService])
trait PreCheckService {
  def determineJourney()(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[JourneyDC]
}

class PreferencesPreCheckService @Inject() (connector: PreferencesConnector) extends PreCheckService {

  def determineJourney()(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[JourneyDC] =
    connector.getPreferencesUnresolved() map {
      case Right(p)                        => processPreferences(p)
      case Left(PreferenceNotFound(_))     => OptInJourney("New customer with no preference record")
      case Left(MultiplePreferenceFound()) => ConflictJourney("Multiple Preferences Found")
      case other                           => throw new Exception(s"Unhandled case in determineJourney(): $other")
    }

  private def processPreferences(preference: PreferenceResponse): JourneyDC =
    preference match {
      case p @ PreferenceResponse(_, _, _, _, _) if !p.genericTermsAccepted =>
        OptInJourney("Existing customer with T&C not accepted")
      case p @ PreferenceResponse(_, None, _, _, _) if p.genericTermsAccepted =>
        OptInJourney("T&Cs are accepted but no valid email address provided")
      case p @ PreferenceResponse(_, Some(EmailPreference(email, false, false, _, _, _, _)), _, _, _)
          if p.genericTermsAccepted =>
        EmailVerificationJourney("T&C accepted, but email is not verified yet", email)
      case p @ PreferenceResponse(_, Some(EmailPreference(email, _, true, _, _, _, _)), _, _, _)
          if p.genericTermsAccepted && p.isOnNewTerms =>
        BounceEmailJourney("T&C accepted, but email bounced", email)
      case p @ PreferenceResponse(_, Some(EmailPreference(_, true, false, _, _, _, _)), _, Some(entityId), _)
          if p.genericTermsAccepted && p.isOnNewTerms =>
        SilentRedirectJourney(entityId = entityId, "HappyPath")
      case p @ PreferenceResponse(_, Some(EmailPreference(_, _, false, _, _, _, _)), _, _, _)
          if p.genericTermsAccepted && !p.isOnNewTerms =>
        ReOptInJourney("New T&Cs are need to be accepted", preference.email)
      case p @ PreferenceResponse(_, Some(EmailPreference(email, _, true, _, _, _, Some(_))), _, _, _)
          if p.genericTermsAccepted && !p.isOnNewTerms =>
        BounceEmailJourney("Pending Email bounced, so new T&Cs are not accepted", email)
      case p @ PreferenceResponse(_, Some(EmailPreference(_, _, true, _, _, _, _)), _, _, _)
          if p.genericTermsAccepted && !p.isOnNewTerms =>
        ReOptInModifiedJourney("Email bounced, so new T&Cs are not accepted", preference.email)
      case _ =>
        throw new Exception(s"Unhandled case in processPreferences(...): $preference")
    }
}
