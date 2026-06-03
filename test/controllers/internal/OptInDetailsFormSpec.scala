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

import controllers.internal.OptInDetailsForm.Data
import controllers.internal.PaperlessChoice.OptedIn
import play.api.data.Form
import uk.gov.hmrc.emailaddress.EmailAddress
import utils.SpecBase
import utils.TestData.TEST_EMAIL_VALUE

class OptInDetailsFormSpec extends SpecBase {

  "OptInEmailForm" should {
    "bind the form successfully for valid values" in {
      val form: Form[String] = OptInEmailForm().bind(Map("sps-opt-in-email" -> "test@gmail.com"))

      form.hasErrors must be(false)
    }

    "throw error for invalid values" in {
      val formWithInvalidField: Form[String] = OptInEmailForm().bind(Map("sps" -> "test@gmail.com"))
      val formWithInvalidDomain: Form[String] = OptInEmailForm().bind(Map("sps-opt-in-email" -> "test@test"))

      val emailValue: String =
        "thisemailvaluehasbeencreatedtotesttheinvalidemailvaluethatismorethan319characters"

      val formWithInvalidEmailLength: Form[String] =
        OptInEmailForm().bind(
          Map(
            "sps-opt-in-email" ->
              s"$emailValue$emailValue$emailValue$emailValue@gmail.com"
          )
        )

      formWithInvalidField.hasErrors must be(true)
      assert(formWithInvalidField.errors("sps-opt-in-email").head.message == "error.required")

      formWithInvalidDomain.hasErrors must be(true)
      assert(formWithInvalidDomain.errors("sps-opt-in-email").head.message == "error.email")

      formWithInvalidEmailLength.hasErrors must be(true)
      assert(formWithInvalidEmailLength.errors("sps-opt-in-email").head.message == "error.email_too_long")
    }
  }

  "OptInDetailsForm.Data" should {
    val data = Data(
      email = (Some(TEST_EMAIL_VALUE), Some(TEST_EMAIL_VALUE)),
      emailVerified = Some("true"),
      choice = Some(OptedIn),
      acceptedTCs = Some(true),
      emailAlreadyStored = Some(false)
    )

    "return correct value for isEmailVerified" in {
      data.isEmailVerified must be(true)
    }

    "return correct value for isEmailAlreadyStored" in {
      data.isEmailAlreadyStored must be(false)
    }

    "return correct value for mainEmail" in {
      data.mainEmail mustBe Some(TEST_EMAIL_VALUE)
    }

    "return correct value for apply method" in {
      Data(
        emailAddress = Some(EmailAddress(TEST_EMAIL_VALUE)),
        preference = Some(OptedIn),
        acceptedTcs = Some(true),
        emailAlreadyStored = Some(false)
      ) mustBe data.copy(emailVerified = None)

    }
  }

  "ReOptInDetailsForm.Data" should {
    val data = ReOptInDetailsForm.Data(
      email = (Some(TEST_EMAIL_VALUE), Some(TEST_EMAIL_VALUE)),
      emailVerified = Some("true"),
      choice = Some(OptedIn),
      acceptedTCs = Some(true),
      emailAlreadyStored = Some(false)
    )

    "return correct value for isEmailVerified" in {
      data.isEmailVerified must be(true)
    }

    "return correct value for isEmailAlreadyStored" in {
      data.isEmailAlreadyStored must be(false)
    }

    "return correct value for mainEmail" in {
      data.mainEmail mustBe Some(TEST_EMAIL_VALUE)
    }

    "return correct value for apply method" in {
      ReOptInDetailsForm.Data(
        emailAddress = Some(EmailAddress(TEST_EMAIL_VALUE)),
        preference = Some(OptedIn),
        acceptedTcs = Some(true),
        emailAlreadyStored = Some(false)
      ) mustBe data.copy(emailVerified = None)

    }
  }

  "OptInStartForm.Data" should {
    "return the correct value for isEmailAlreadyStored" in {
      OptInStartForm.Data(choice = Some(OptedIn), emailAlreadyStored = Some(true)).isEmailAlreadyStored must be(true)
    }
  }

  "PaperlessChoice.toBoolean" should {

    "return the correct value" in {
      PaperlessChoice.OptedIn.toBoolean must be(true)
      PaperlessChoice.OptedOut.toBoolean must be(false)
    }
  }
}
