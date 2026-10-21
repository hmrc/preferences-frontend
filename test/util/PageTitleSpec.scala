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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import util.PageTitle

class PageTitleSpec extends AnyWordSpec with Matchers {

  "PageTitle" should {

    "append the service name and GOV.UK" in {
      PageTitle("For your security, we signed you out", Some("My service")) shouldBe
        "For your security, we signed you out – My service – GOV.UK"
    }

    "omit the service name when it is missing" in {
      PageTitle("For your security, we signed you out", None) shouldBe
        "For your security, we signed you out – GOV.UK"
    }

    "omit the service name when it is blank" in {
      PageTitle("For your security, we signed you out", Some("   ")) shouldBe
        "For your security, we signed you out – GOV.UK"
    }
  }
}
