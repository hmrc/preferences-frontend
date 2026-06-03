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

package controllers

import utils.SpecBase
import utils.TestData.TEST_URL

class ExternalUrlsSpec extends SpecBase {

  "ExternalUrlPrefixes.pfUrlPrefix" should {
    "return correct value" in new Setup {
      externalUrlPrefixes.pfUrlPrefix must be("http://localhost:9024")
    }
  }

  "ExternalUrlPrefixes.ytaUrlPrefix" should {
    "return correct value" in new Setup {
      externalUrlPrefixes.ytaUrlPrefix must be("http://localhost:9020")
    }
  }

  "ExternalUrlPrefixes.caUrlPrefix" should {
    "return correct value" in new Setup {
      externalUrlPrefixes.caUrlPrefix must be("http://localhost:9025")
    }
  }

  "ExternalUrlPrefixes.bas_gateway_frontendUrl" should {
    "return correct value" in new Setup {
      externalUrlPrefixes.bas_gateway_frontendUrl must be("http://localhost:9553")
    }
  }

  "ExternalUrls.betaFeedbackUrl" should {
    "return correct value" in new Setup {
      externalUrls.betaFeedbackUrl must be("http://localhost:9025/contact/beta-feedback")
    }
  }

  "ExternalUrls.betaFeedbackUnauthenticatedUrl" should {
    "return correct value" in new Setup {
      externalUrls.betaFeedbackUnauthenticatedUrl must be("http://localhost:9025/contact/beta-feedback-unauthenticated")
    }
  }

  "ExternalUrls.taxAccountRedirect" should {
    "return correct value" in new Setup {
      externalUrls.taxAccountRedirect must be("http://localhost:9020/account")
    }
  }

  "ExternalUrls.bta" should {
    "return correct value" in new Setup {
      externalUrls.bta must be("business-account")
    }
  }

  "ExternalUrls.btaSignoutUrl" should {
    "return correct value" in new Setup {
      externalUrls.betaFeedbackUnauthenticatedUrl must be("http://localhost:9025/contact/beta-feedback-unauthenticated")
    }
  }

  "ExternalUrls.ptaSignoutUrl" should {
    "return correct value" in new Setup {
      externalUrls.ptaSignoutUrl must be("http://localhost:9553/bas-gateway/sign-out-without-state")
    }
  }

  "ExternalUrls.survey" should {
    "return correct value" when {
      "there is some return url" in new Setup {
        externalUrls.survey(Some(TEST_URL)) must be("http://localhost:9553/bas-gateway/sign-out-without-state")
      }

      "there is no return url" in new Setup {
        externalUrls.survey(None) must be("http://localhost:9553/bas-gateway/sign-out-without-state")
      }
    }
  }

  trait Setup {
    lazy val externalUrlPrefixes: ExternalUrlPrefixes = app.injector.instanceOf[ExternalUrlPrefixes]
    lazy val externalUrls: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  }
}
