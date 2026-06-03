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

import java.time.Instant
import play.api.libs.json.*

enum SurveyType {
  case StandardInterruptOptOut
}
object SurveyType {
  implicit val format: Format[SurveyType] = new Format[SurveyType] {
    def reads(json: JsValue): JsResult[SurveyType] = json match {
      case JsString("StandardInterruptOptOut") => JsSuccess(StandardInterruptOptOut)
      case _                                   => JsError("Invalid Survey Type")
    }

    def writes(surveyType: SurveyType): JsValue = JsString(surveyType.toString)
  }
}

final case class Survey(surveyType: SurveyType, completedAt: Instant)

object Survey {

  final val dateTimeReads: Reads[Instant] =
    Reads
      .at[Long](__ \ "$date")
      .map(dateTime => Instant.ofEpochMilli(dateTime))

  final val dateTimeWrites: Writes[Instant] =
    Writes
      .at[Long](__ \ "$date")
      .contramap[Instant](_.toEpochMilli)

  implicit val dateTimeFormat: Format[Instant] =
    Format(dateTimeReads, dateTimeWrites)

  implicit val surveyFormat: Format[Survey] = Json.format
}
