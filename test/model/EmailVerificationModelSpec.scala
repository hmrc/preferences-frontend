/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package model

import model.VerifyStatus.Success
import play.api.libs.json.{ JsResult, JsResultException, Json }
import utils.SpecBase
import utils.TestData.{ TEST_DESCRIPTION, TEST_LINK_TEXT, TEST_URL }

class EmailVerificationModelSpec extends SpecBase {

  "EmailVerification format" should {
    import EmailVerification.given_Format_EmailVerification

    "read the json correctly" in new Setup {
      Json.parse(emailVerificationJsonString).as[EmailVerification] mustBe emailVerification
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(emailVerificationInvalidJsonString).as[EmailVerification]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(emailVerification) mustBe Json.parse(emailVerificationJsonString)
    }
  }

  trait Setup {
    val emailVerification: EmailVerification =
      EmailVerification(
        verifyStatus = Success,
        description = TEST_DESCRIPTION,
        returnLinkText = Some(TEST_LINK_TEXT),
        returnUrl = Some(TEST_URL)
      )

    val emailVerificationJsonString: String =
      """{
        |"verifyStatus":"success",
        |"description":"test_desc",
        |"returnLinkText":"test_link",
        |"returnUrl":"http://localhost:9088/test"
        |}""".stripMargin

    val emailVerificationInvalidJsonString: String =
      """{
        |"description":"test_desc",
        |"returnLinkText":"test_link",
        |"returnUrl":"http://localhost:9088/test"
        |}""".stripMargin
  }
}
