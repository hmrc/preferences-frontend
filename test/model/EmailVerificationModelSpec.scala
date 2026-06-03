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
