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

package model

import connectors.EmailPreference
import model.JourneyTypeDC.{ BounceEmail, Conflict, EmailVerification, OptIn, ReOptIn, ReOptInModified, SilentRedirect }
import play.api.libs.json._

enum JourneyTypeDC(val entryName: String) {
  case SilentRedirect extends JourneyTypeDC("SILENT_REDIRECT")
  case Conflict extends JourneyTypeDC("CONFLICT")
  case OptIn extends JourneyTypeDC("OPT_IN")
  case EmailVerification extends JourneyTypeDC("EMAIL_VERIFICATION")
  case BounceEmail extends JourneyTypeDC("BOUNCE_EMAIL")
  case ReOptIn extends JourneyTypeDC("RE_OPT_IN")
  case ReOptInModified extends JourneyTypeDC("RE_OPT_IN_MODIFIED")
}

object JourneyTypeDC {
  implicit val format: Format[JourneyTypeDC] = new Format[JourneyTypeDC] {
    def reads(json: JsValue): JsResult[JourneyTypeDC] = json match {
      case JsString("SILENT_REDIRECT")    => JsSuccess(SilentRedirect)
      case JsString("CONFLICT")           => JsSuccess(Conflict)
      case JsString("OPT_IN")             => JsSuccess(OptIn)
      case JsString("EMAIL_VERIFICATION") => JsSuccess(EmailVerification)
      case JsString("BOUNCE_EMAIL")       => JsSuccess(BounceEmail)
      case JsString("RE_OPT_IN")          => JsSuccess(ReOptIn)
      case JsString("RE_OPT_IN_MODIFIED") => JsSuccess(ReOptInModified)
      case _                              => JsError("Invalid Journey Type")
    }

    def writes(journeyTypeDC: JourneyTypeDC): JsValue = JsString(journeyTypeDC.entryName)
  }
}

sealed trait JourneyDC {
  def journeyType: JourneyTypeDC
  def reason: String
}

case class SilentRedirectJourney(entityId: String, reason: String = "", journeyType: JourneyTypeDC = SilentRedirect)
    extends JourneyDC

case class ConflictJourney(reason: String, journeyType: JourneyTypeDC = Conflict) extends JourneyDC

case class OptInJourney(reason: String, journeyType: JourneyTypeDC = OptIn) extends JourneyDC

case class EmailVerificationJourney(
  reason: String,
  email: String,
  journeyType: JourneyTypeDC = EmailVerification
) extends JourneyDC

case class BounceEmailJourney(
  reason: String,
  email: String,
  journeyType: JourneyTypeDC = BounceEmail
) extends JourneyDC

case class ReOptInJourney(
  reason: String,
  email: Option[EmailPreference],
  journeyType: JourneyTypeDC = ReOptIn
) extends JourneyDC

case class ReOptInModifiedJourney(
  reason: String,
  email: Option[EmailPreference],
  journeyType: JourneyTypeDC = ReOptInModified
) extends JourneyDC

object JourneyDC {
  implicit val silentRedirectFormat: OFormat[SilentRedirectJourney] = Json.format[SilentRedirectJourney]
  implicit val silentRedirectWrites: Writes[SilentRedirectJourney] = Json.writes[SilentRedirectJourney]
  implicit val conflictFormat: OFormat[ConflictJourney] = Json.format[ConflictJourney]
  implicit val conflictWrites: Writes[ConflictJourney] = Json.writes[ConflictJourney]
  implicit val optInFormat: OFormat[OptInJourney] = Json.format[OptInJourney]
  implicit val optIntWrites: Writes[OptInJourney] = Json.writes[OptInJourney]
  implicit val emailVerificationFormat: OFormat[EmailVerificationJourney] = Json.format[EmailVerificationJourney]
  implicit val emailVerificationWrites: Writes[EmailVerificationJourney] = Json.writes[EmailVerificationJourney]
  implicit val bounceEmailFormat: OFormat[BounceEmailJourney] = Json.format[BounceEmailJourney]
  implicit val bounceEmailWrites: Writes[BounceEmailJourney] = Json.writes[BounceEmailJourney]
  implicit val reOptInModifiedFormat: OFormat[ReOptInModifiedJourney] = Json.format[ReOptInModifiedJourney]
  implicit val reOptInModifiedWrites: Writes[ReOptInModifiedJourney] = Json.writes[ReOptInModifiedJourney]
  implicit val reOptInFormat: OFormat[ReOptInJourney] = Json.format[ReOptInJourney]
  implicit val reOptInWrites: Writes[ReOptInJourney] = Json.writes[ReOptInJourney]
  implicit val formats: OFormat[JourneyDC] = Json.format[JourneyDC]
}
