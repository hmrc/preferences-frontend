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

package views.sa.prefs.cohorts

import helpers.TestFixtures
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import utils.SpecBase
import views.html.sa.prefs.cohorts.types_of_online_tax_letters

class TypesOfOnlineTaxLettersSpec extends SpecBase {

  "view" should {

    "render the content correctly" in {
      val view: types_of_online_tax_letters = app.injector.instanceOf[types_of_online_tax_letters]

      implicit val hostContext: HostContext = TestFixtures.sampleHostContext

      val viewHtml: Document = Jsoup.parse(view.apply().body)

      viewHtml.getElementsByTag("h1").html() mustBe messages("types_of_online_tax_letters_h1")

      val pElements: Elements = viewHtml.getElementsByTag("p")
      pElements.get(0).text() mustBe messages("types_of_online_tax_letters_p1")
      pElements.get(1).text() mustBe messages("types_of_online_tax_letters_p2")
      pElements.get(2).text() mustBe messages("types_of_online_tax_letters_p_1")
      pElements.get(3).text() mustBe messages("types_of_online_tax_letters_p_2")

      viewHtml.getElementsByTag("h2").text() mustBe messages("types_of_online_tax_letters_h2")

      val h3Elements: Elements = viewHtml.getElementsByTag("h3")
      h3Elements.get(0).text() mustBe messages("types_of_online_tax_letters_h3_1")
      h3Elements.get(1).text() mustBe messages("types_of_online_tax_letters_h3_2")
    }
  }
}
