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
