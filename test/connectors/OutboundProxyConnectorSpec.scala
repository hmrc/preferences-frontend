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

import play.api.http.HeaderNames.{ AUTHORIZATION, CONNECTION }
import utils.SpecBase

class OutboundProxyConnectorSpec extends SpecBase {

  "outboundHeadersFilter" should {

    "return true when key is not present in outboundHeaderAllowList" in {
      OutboundProxyConnector.outboundHeadersFilter("Unknown", "Unknown") must be(true)
    }

    "return false when key is present in outboundHeaderAllowList" in {
      OutboundProxyConnector.outboundHeadersFilter(CONNECTION, CONNECTION) must be(false)
    }
  }

  "loggedHeaderAllowlist" should {

    "return correct output" in {
      OutboundProxyConnector.loggedHeaderAllowlist must be(Set("Ocp-Apim-Subscription-Key", AUTHORIZATION))
    }
  }

  "loggedHeadersFilter" should {

    "return true when key is not present in loggedHeaderAllowlist" in {
      OutboundProxyConnector.loggedHeadersFilter(CONNECTION, CONNECTION) must be(true)
    }

    "return false when key is present in loggedHeaderAllowlist" in {
      OutboundProxyConnector.loggedHeadersFilter("Ocp-Apim-Subscription-Key", "Ocp-Apim-Subscription-Key") must be(
        false
      )
    }
  }
}
