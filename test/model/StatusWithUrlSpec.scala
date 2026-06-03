/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package model

import model.Category.ActionRequired
import model.StatusName.ReOptIn
import utils.SpecBase
import utils.TestData.{ TEST_LINK_TEXT, TEST_URL }
import play.api.libs.json.{ JsResultException, Json }

class StatusWithUrlSpec extends SpecBase {

  "StatusWithUrl.formats" should {
    import StatusWithUrl.formats

    "read the json correctly" in new Setup {
      Json.parse(statusWithUrlJsonString).as[StatusWithUrl] mustBe statusWithUrl
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(statusWithUrlInvalidJsonString).as[StatusWithUrl]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(statusWithUrl) mustBe Json.parse(statusWithUrlJsonString)
    }
  }

  "Url.formats" should {
    import Url.formats

    "read the json correctly" in new Setup {
      Json.parse(urlJsonString).as[Url] mustBe url
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(urlJsonInvalidString).as[Url]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(url) mustBe Json.parse(urlJsonString)
    }
  }

  "PaperlessStatus.formats" should {
    import PaperlessStatus.formats

    "read the json correctly" in new Setup {
      Json.parse(paperlessStatusJsonString).as[PaperlessStatus] mustBe paperlessStatus
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(paperlessStatusInvalidJsonString).as[PaperlessStatus]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(paperlessStatus) mustBe Json.parse(paperlessStatusJsonString)
    }
  }

  trait Setup {
    val url: Url = Url(link = TEST_URL, text = TEST_LINK_TEXT)
    val paperlessStatus: PaperlessStatus =
      PaperlessStatus(name = ReOptIn, category = ActionRequired, text1 = TEST_LINK_TEXT, text2 = TEST_LINK_TEXT)

    val statusWithUrl: StatusWithUrl = StatusWithUrl(status = paperlessStatus, url = url)

    val urlJsonString: String = """{"link":"http://localhost:9088/test","text":"test_link"}""".stripMargin
    val urlJsonInvalidString: String = """{"text":"test_link"}""".stripMargin

    val statusWithUrlJsonString: String =
      """{
        |"status":{"name":"RE_OPT_IN","category":"ACTION_REQUIRED","text1":"test_link","text2":"test_link"},
        |"url":{"link":"http://localhost:9088/test","text":"test_link"}
        |}""".stripMargin

    val statusWithUrlInvalidJsonString: String =
      """{
        |"url":{"link":"http://localhost:9088/test","text":"test_link"}
        |}""".stripMargin

    val paperlessStatusJsonString: String =
      """{
        |"name":"RE_OPT_IN",
        |"category":"ACTION_REQUIRED",
        |"text1":"test_link",
        |"text2":"test_link"
        |}""".stripMargin

    val paperlessStatusInvalidJsonString: String =
      """{
        |"category":"ACTION_REQUIRED",
        |"text1":"test_link",
        |"text2":"test_link"
        |}""".stripMargin
  }
}
