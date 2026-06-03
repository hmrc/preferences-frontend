/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model

import java.time.{ ZoneOffset, ZonedDateTime }
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsObject, JsResultException, JsString, Json }
import model.SurveyType.StandardInterruptOptOut
import model.Survey.*

class SurveySpec extends PlaySpec {

  "SurveyType" should {
    """deserialize SandardInterruptOptOut from string "StandardInterrupOptOut" """ in {
      JsString("StandardInterruptOptOut").as[SurveyType] must be(SurveyType.StandardInterruptOptOut)
    }

    """serialize StandardInterruptOptOut to JsString("StandardInterruptOptOut")""" in {
      Json.toJson(StandardInterruptOptOut) must be(JsString("StandardInterruptOptOut"))
    }

    "serialize and deserialize Survey" in {
      val date = ZonedDateTime.of(2015, 5, 13, 0, 0, 0, 0, ZoneOffset.UTC).toInstant
      val fixture = Json
        .parse(s"""
                  |{
                  |  "surveyType": "StandardInterruptOptOut",
                  |  "completedAt": {"$$date": ${date.toEpochMilli}}
                  |}""".stripMargin)
        .as[JsObject]

      fixture.as[Survey] must be(Survey(StandardInterruptOptOut, date))
      Json.toJson(Survey(StandardInterruptOptOut, date)) must be(fixture)
    }

    "throw exception when json is invalid for Survey" in {
      import Survey.surveyFormat

      val invalidJson = """{"surveyType":"StandardInterruptOptOut"}"""

      intercept[JsResultException] {
        Json.parse(invalidJson).as[Survey]
      }
    }
  }
}
