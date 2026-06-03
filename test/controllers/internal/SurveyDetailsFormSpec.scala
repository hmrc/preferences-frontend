/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.{ JsResultException, Json }
import utils.SpecBase
import utils.TestData.{ TEST_LANG_VALUE, TEST_NINO, TEST_REASON, TEST_UTR }

class SurveyDetailsFormSpec extends SpecBase {

  "QuestionAnswer.formats" should {
    import QuestionAnswer.formats

    "read the json correctly" in new Setup {
      Json.parse(questionAnswerJsonString).as[QuestionAnswer] mustBe questionAnswer
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(questionAnswerInvalidJsonString).as[QuestionAnswer]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(questionAnswer) mustBe Json.parse(questionAnswerJsonString)
    }
  }

  "EventDetail.formats" should {
    import EventDetail.formats

    "read the json correctly" in new Setup {
      Json.parse(eventDetailJsonString).as[EventDetail] mustBe eventDetail
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(eventDetailInvalidJsonString).as[EventDetail]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(eventDetail) mustBe Json.parse(eventDetailJsonString)
    }
  }

  trait Setup {
    val questionAnswer: QuestionAnswer = QuestionAnswer(question = "test", answer = "test")
    val eventDetail: EventDetail =
      EventDetail(
        submissionType = "submitted",
        utr = TEST_UTR,
        nino = TEST_NINO,
        language = TEST_LANG_VALUE,
        choices = Map(),
        reason = TEST_REASON
      )

    val questionAnswerJsonString: String = """{"question":"test","answer":"test"}"""
    val questionAnswerInvalidJsonString: String = """{"question":"test"}"""

    val eventDetailJsonString: String =
      """{
        |"submissionType":"submitted",
        |"utr":"UTR-456",
        |"nino":"NA000914D",
        |"language":"en",
        |"choices":{},
        |"reason":"test_reason"
        |}""".stripMargin

    val eventDetailInvalidJsonString: String =
      """{
        |"utr":"UTR-456",
        |"nino":"NA000914D",
        |"language":"en",
        |"choices":{},
        |"reason":"test_reason"
        |}""".stripMargin
  }
}
