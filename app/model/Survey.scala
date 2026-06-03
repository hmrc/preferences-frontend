/*
 * Copyright 2023 HM Revenue & Customs
 *
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
