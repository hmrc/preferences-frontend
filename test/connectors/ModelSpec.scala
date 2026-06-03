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

package connectors

import connectors.SaEmailPreference.Status.{ Pending, Verified }
import connectors.StatusNameResponse.Paper
import model.Language.English
import model.Survey
import model.SurveyType.StandardInterruptOptOut
import play.api.libs.json.{ JsResultException, Json }
import utils.SpecBase
import utils.TestData.*

class ModelSpec extends SpecBase {

  "UpdateEmail.formats" should {
    import UpdateEmail.formats

    "read the json correctly" in new Setup {
      Json.parse(updateEmailJsonString).as[UpdateEmail] mustBe updateEmail
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(updateEmailInvalidJsonString).as[UpdateEmail]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(updateEmail) mustBe Json.parse(updateEmailJsonString)
    }
  }

  "SaPreference.formats" should {
    import SaPreference.formats

    "read the json correctly" in new Setup {
      Json.parse(saPreferenceJsonString).as[SaPreference] mustBe saPreference
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(saPreferenceInvalidJsonString).as[SaPreference]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(saPreference) mustBe Json.parse(saPreferenceJsonString)
    }
  }

  "SaEmailPreference.formats" should {
    import SaEmailPreference.Status.formats
    import SaEmailPreference.formats

    "read the json correctly" in new Setup {
      Json.parse(saEmailPreferenceJsonString).as[SaEmailPreference] mustBe saEmailPreference
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(saEmailPreferenceInvalidJsonString).as[SaEmailPreference]
      }
    }
  }

  "SaEmailPreference.emailPreferenceOps.toEmailPreference" should {
    "return the correct output" in new Setup {
      saEmailPreference.emailPreferenceOps(saEmailPreference).toEmailPreference mustBe EmailPreference(
        email = TEST_EMAIL_VALUE,
        isVerified = false,
        hasBounces = false,
        mailboxFull = false,
        linkSent = Some(TEST_LOCAL_DATE)
      )
    }
  }

  "SaPreferenceSimplified.formats" should {
    import SaPreferenceSimplified.formats

    "read the json correctly" in new Setup {
      Json.parse(saPreferenceSimplifiedJsonString).as[SaPreferenceSimplified] mustBe saPreferenceSimplified
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(saPreferenceSimplifiedInvalidJsonString).as[SaPreferenceSimplified]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(saPreferenceSimplified) mustBe Json.parse(saPreferenceSimplifiedJsonString)
    }
  }

  "ValidateEmail.formats" should {
    import ValidateEmail.formats

    "read the json correctly" in new Setup {
      Json.parse(validateEmailJsonString).as[ValidateEmail] mustBe validateEmail
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(validateEmailInvalidJsonString).as[ValidateEmail]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(validateEmail) mustBe Json.parse(validateEmailJsonString)
    }
  }

  "PaperlessStatusResponse.formats" should {
    import PaperlessStatusResponse.formats

    "read the json correctly" in new Setup {
      Json.parse(paperlessStatusResponseJsonString).as[PaperlessStatusResponse] mustBe paperlessStatusResponse
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(paperlessStatusResponseInvalidJsonString).as[PaperlessStatusResponse]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(paperlessStatusResponse) mustBe Json.parse(paperlessStatusResponseJsonString)
    }
  }

  "TermsAndConditionsAcceptance.formats" should {
    import TermsAndConditionsAcceptance.formats

    "read the json correctly" in new Setup {
      Json
        .parse(termsAndConditionsAcceptanceJsonString)
        .as[TermsAndConditionsAcceptance] mustBe termsAndConditionsAcceptance
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(termsAndConditionsAcceptanceInvalidJsonString).as[TermsAndConditionsAcceptance]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(termsAndConditionsAcceptance) mustBe Json.parse(termsAndConditionsAcceptanceJsonString)
    }
  }

  "PreferenceResponse.formats" should {
    import PreferenceResponse.formats

    "read the json correctly" in new Setup {
      Json
        .parse(preferenceResponseJsonString)
        .as[PreferenceResponse] mustBe preferenceResponse
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(preferenceResponseInvalidJsonString).as[PreferenceResponse]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(preferenceResponse) mustBe Json.parse(preferenceResponseJsonString)
    }
  }

  trait Setup {
    val updateEmail: UpdateEmail = UpdateEmail(email = TEST_EMAIL_VALUE, journey = Some(TEST_JOURNEY_VALUE))
    val saEmailPreference: SaEmailPreference =
      SaEmailPreference(
        email = TEST_EMAIL_VALUE,
        status = Pending,
        message = Some(TEST_MSG),
        linkSent = Some(TEST_LOCAL_DATE)
      )

    val survey: Survey = Survey(surveyType = StandardInterruptOptOut, completedAt = TEST_TIME_INSTANT)

    val saPreference: SaPreference = SaPreference(digital = true, email = None, surveys = Some(List(survey)))

    val saPreferenceSimplified: SaPreferenceSimplified =
      SaPreferenceSimplified(digital = true, email = Some(TEST_EMAIL_VALUE))

    val validateEmail: ValidateEmail = ValidateEmail(TEST_TOKEN)

    val paperlessStatusResponse: PaperlessStatusResponse = PaperlessStatusResponse(Paper)

    val termsAndConditionsAcceptance: TermsAndConditionsAcceptance =
      TermsAndConditionsAcceptance(
        accepted = true,
        updatedAt = Some(TEST_TIME_INSTANT),
        majorVersion = Some(2),
        paperless = Some(true)
      )

    val emailPreference: EmailPreference = EmailPreference(
      email = TEST_EMAIL_VALUE,
      isVerified = true,
      hasBounces = false,
      mailboxFull = false,
      linkSent = Some(TEST_LOCAL_DATE),
      language = Some(English),
      pendingEmail = Some(TEST_EMAIL_VALUE)
    )

    val preferenceResponse: PreferenceResponse =
      PreferenceResponse(
        termsAndConditions = Map(TEST_KEY -> termsAndConditionsAcceptance),
        email = Some(emailPreference)
      )

    val updateEmailJsonString: String = """{"email":"test@test.com","journey":"OPT_IN"}""".stripMargin
    val updateEmailInvalidJsonString: String = """{"journey":"OPT_IN"}""".stripMargin

    val saEmailPreferenceJsonString: String =
      """{
        |"email":"test@test.com",
        |"status":"pending",
        |"mailboxFull":false,
        |"message":"test_msg",
        |"linkSent":"2026-02-22"
        |}""".stripMargin

    val saEmailPreferenceInvalidJsonString: String = """{}""".stripMargin

    val saPreferenceJsonString: String =
      """{
        |"digital":true,
        |"surveys":[{"surveyType":"StandardInterruptOptOut","completedAt":{"$date":3467288}}]
        |}""".stripMargin

    val saPreferenceInvalidJsonString: String =
      """{
        |"surveys":[{"surveyType":"StandardInterruptOptOut","completedAt":{"$date":3467288}}]
        |}""".stripMargin

    val saPreferenceSimplifiedJsonString: String =
      """{
        |"digital":true,
        |"email":"test@test.com"
        |}""".stripMargin

    val saPreferenceSimplifiedInvalidJsonString: String =
      """{
        |"surveys":[{"surveyType":"StandardInterruptOptOut","completedAt":{"$date":3467288}}]
        |}""".stripMargin

    val validateEmailJsonString: String = """{"token":"14578hggdss908"}""".stripMargin
    val validateEmailInvalidJsonString: String = """{}""".stripMargin

    val paperlessStatusResponseJsonString: String = """{"name":"PAPER"}""".stripMargin
    val paperlessStatusResponseInvalidJsonString: String = """{}""".stripMargin

    val termsAndConditionsAcceptanceJsonString: String =
      """{"accepted":true,"updatedAt":"1970-01-01T00:57:47.288Z","majorVersion":2,"paperless":true}""".stripMargin

    val termsAndConditionsAcceptanceInvalidJsonString: String =
      """{"updatedAt":"1970-01-01T00:57:47.288Z","majorVersion":2,"paperless":true}""".stripMargin

    val preferenceResponseJsonString: String =
      """{
        |"termsAndConditions":{
        |"test_key":{"accepted":true,"updatedAt":"1970-01-01T00:57:47.288Z","majorVersion":2,"paperless":true}
        |},
        |"email":{
        |"email":"test@test.com",
        |"isVerified":true,
        |"hasBounces":false,
        |"mailboxFull":false,
        |"linkSent":"2026-02-22",
        |"language":"en",
        |"pendingEmail":"test@test.com"
        |}
        |}""".stripMargin

    val preferenceResponseInvalidJsonString: String =
      """{
        |"email":{
        |"email":"test@test.com",
        |"isVerified":true,
        |"hasBounces":false,
        |"mailboxFull":false,
        |"linkSent":"2026-02-22",
        |"language":"en",
        |"pendingEmail":"test@test.com"
        |}
        |}""".stripMargin
  }
}
