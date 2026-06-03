/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package util

import utils.SpecBase

class ToolsSpec extends SpecBase {

  "urlEncode" should {
    "encode the value correctly" in new Setup {
      tools.urlEncode("test") must be("test")
    }
  }

  "encryptAndEncode" should {
    "encrypt and encode the value correctly" in new Setup {
      tools.encryptAndEncode("test") must be("E7IFTVaps4le10Lth9NxBw%3D%3D")
    }
  }

  trait Setup {
    val tools: Tools = app.injector.instanceOf[Tools]
  }
}
