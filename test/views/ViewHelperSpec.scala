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

package views

import org.scalatestplus.play.PlaySpec
import _root_.helpers.LanguageHelper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ViewHelperSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper {

  "serviceName" should {
    "return correct service name text" when {

      "input service name identifier is itsa" in {
        val result: Option[String] = ViewHelper.serviceName(Some("itsa"), None)

        result.map { msgKey =>
          msgKey mustBe "sa_printing_preference_svc_name_itsa"
          messagesInEnglish()(msgKey) mustBe "Sign up for Making Tax Digital for Income Tax"
          messagesInWelsh()(msgKey) mustBe "Cofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm"
        }
      }

      "input regime name is itsa" in {
        val result: Option[String] = ViewHelper.serviceName(Some("unknown"), Some("itsa"))

        result.map { msgKey =>
          msgKey mustBe "sa_printing_preference_svc_name_itsa"
          messagesInEnglish()(msgKey) mustBe "Sign up for Making Tax Digital for Income Tax"
          messagesInWelsh()(msgKey) mustBe "Cofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm"
        }
      }

      "neither service identifier nor regime name is itsa" in {
        val nonITSAServiceName = "nonitsa"
        val result: Option[String] = ViewHelper.serviceName(Some(nonITSAServiceName), Some(nonITSAServiceName))

        result.map { serviceName =>
          serviceName mustBe nonITSAServiceName
        }
      }
    }
  }
}
