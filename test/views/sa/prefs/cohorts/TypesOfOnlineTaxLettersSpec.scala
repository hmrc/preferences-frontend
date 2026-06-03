/*
 * Copyright 2026 HM Revenue & Customs
 *
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
