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

package config

import utils.SpecBase

class AppConfigSpec extends SpecBase {

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "betaFeedbackUrl" should {
    "return correct value" in {
      appConfig.betaFeedbackUrl mustBe "http://localhost:9025/contact/beta-feedback"
    }
  }

  "betaFeedbackUnauthenticatedUrl" should {
    "return correct value" in {
      appConfig.betaFeedbackUnauthenticatedUrl mustBe "http://localhost:9025/contact/beta-feedback-unauthenticated"
    }
  }

  "homeUrl" should {
    "return correct value" in {
      appConfig.homeUrl mustBe "http://localhost:9020/account"
    }
  }

  "signOutUrl" should {
    "return correct value" when {
      "returnUrl is None" in {
        appConfig.signOutUrl(None) mustBe "http://localhost:9553/bas-gateway/sign-out-without-state"
      }

      "returnUrl has some value" in {
        appConfig.signOutUrl(Some("business-account")) mustBe "http://localhost:9020/business-account/sso-sign-out"
      }
    }
  }

}
