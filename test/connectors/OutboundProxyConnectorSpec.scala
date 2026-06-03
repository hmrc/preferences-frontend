/*
 * Copyright 2026 HM Revenue & Customs
 *
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
