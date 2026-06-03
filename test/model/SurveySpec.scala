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
