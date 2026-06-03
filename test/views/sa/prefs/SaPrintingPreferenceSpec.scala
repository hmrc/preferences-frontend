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

package views.sa.prefs

import connectors.{ GenericTerms, TermsType }
import controllers.internal.{ CYSConfirmPage47, EmailForm, IPage53, IPage56, IPage7, IPage8, OptInCohort, ReOptInPage10, ReOptInPage52, ReOptInPage54, ReOptInPage55, routes }
import model.JourneyType.MultiPage2
import model.{ HostContext, JourneyType, PageType }
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.abtest.Cohort
import utils.SpecBase
import views.html.sa.prefs.sa_printing_preference

import java.time.LocalDate

class SaPrintingPreferenceSpec extends SpecBase {

  "view" should {
    "render correct contents" when {

      "cohort is IPage7" in new Setup {
        val viewDocIPage7: Document = viewDocument(IPage7)

        viewDocIPage7.getElementById("message-after-email-input-para-1").html() mustBe messages(
          "i_page7.fg_page.yes_send_email_info_1"
        )

        viewDocIPage7.getElementById("message-after-email-input").html() mustBe messages(
          "i_page7.fg_page.yes_send_email_info_2"
        )
      }

      "cohort is IPage8" in new Setup {
        val viewDocIPage8: Document = viewDocument(IPage8)

        viewDocIPage8.getElementById("message-after-email-input-para-1").html() mustBe messages(
          "i_page8.fg_page.yes_send_email_info_1"
        )

        viewDocIPage8.getElementById("message-after-email-input").html() mustBe messages(
          "i_page8.fg_page.yes_send_email_info_2"
        )
      }

      "cohort is IPage53" in new Setup {
        val viewDocIPage53: Document = viewDocument(IPage53)

        viewDocIPage53.getElementById("online-para-1").html() mustBe messages(
          "i_page53.fg_page.yes_send_email_info_1"
        )

        viewDocIPage53.getElementById("online-para-2").html() mustBe messages(
          "i_page53.fg_page.yes_send_email_info_2"
        )

        val uiElements: Elements = viewDocIPage53.getElementsByClass("govuk-list govuk-list--bullet")
      }

      "cohort is IPage56" in new Setup {
        val viewDocIPage56: Document = viewDocument(IPage56)

        viewDocIPage56.getElementById("online-para-3").html() must include(
          messages(
            "i_page56.fg_page.yes_send_email_info_3"
          )
        )

        viewDocIPage56.getElementById("online-para-3").html() must include(
          messages(
            "i_page56.fg_page.yes_send_email_info_4"
          )
        )

        viewDocIPage56.getElementById("online-para-4").html() must include(
          messages(
            "i_page56.fg_page.yes_send_email_info_5"
          )
        )

        viewDocIPage56.getElementById("online-para-4").html() must include(
          messages(
            "i_page56.fg_page.yes_send_email_info_6"
          )
        )
      }

      "cohort is ReOptInPage10" in new Setup {
        val viewDocReOptInPage10: Document = viewDocument(ReOptInPage10)

        viewDocReOptInPage10.getElementById("message-after-email-input-para-1").html() mustBe messages(
          "reoptin_page10.fg_page.yes_send_email_info_1"
        )

        viewDocReOptInPage10.getElementById("message-after-email-input").html() mustBe messages(
          "reoptin_page10.fg_page.yes_send_email_info_2"
        )

        val uiElements: Elements = viewDocReOptInPage10.getElementsByTag("ul")
        val liElements: Elements = uiElements.get(0).getElementsByTag("li")

        liElements.get(0).html() mustBe messages("reoptin_page10.fg_page.list_1")
        liElements.get(1).html() mustBe messages("reoptin_page10.fg_page.list_2")
        liElements.get(2).html() mustBe messages("reoptin_page10.fg_page.list_3")
      }

      "cohort is ReOptInPage52" in new Setup {
        val viewDocReOptInPage52: Document = viewDocument(ReOptInPage52)

        viewDocReOptInPage52.getElementById("message-after-email-input-para-1").html() mustBe messages(
          "reoptin_page52.fg_page.yes_send_email_info_1"
        )

        viewDocReOptInPage52.getElementById("message-after-email-input-para-2").html() mustBe messages(
          "reoptin_page52.fg_page.yes_send_email_info_2"
        )

        viewDocReOptInPage52.getElementById("message-after-email-input-para-3").html() mustBe messages(
          "reoptin_page52.fg_page.yes_send_email_info_3"
        )

        val uiElements: Elements = viewDocReOptInPage52.getElementsByTag("ul")
        val liElements: Elements = uiElements.get(0).getElementsByTag("li")

        liElements.get(0).html() mustBe messages("reoptin_page52.fg_page.list_1")
        liElements.get(1).html() mustBe messages("reoptin_page52.fg_page.list_2")
        liElements.get(2).html() mustBe messages("reoptin_page52.fg_page.list_3")
      }

      "cohort is ReOptInPage54" in new Setup {
        val viewDocReOptInPage54: Document = viewDocument(ReOptInPage54)

        viewDocReOptInPage54.getElementById("online-para-1").html() mustBe messages(
          "reoptin_page54.fg_page.yes_send_email_info_1"
        )

        viewDocReOptInPage54.getElementById("online-para-2").html() must include(
          messages(
            "reoptin_page54.fg_page.yes_send_email_info_2"
          )
        )

        viewDocReOptInPage54.getElementById("online-para-3").html() must include(
          messages(
            "reoptin_page54.fg_page.yes_send_email_info_3"
          )
        )

        val uiElements: Elements = viewDocReOptInPage54.getElementsByTag("ul")
        val liElements: Elements = uiElements.get(0).getElementsByTag("li")

        liElements.get(0).html() mustBe messages("reoptin_page54.fg_page.list_1")
        liElements.get(1).html() mustBe messages("reoptin_page54.fg_page.list_2")
        liElements.get(2).html() mustBe messages("reoptin_page54.fg_page.list_3")
        liElements.get(3).html() mustBe messages("reoptin_page54.fg_page.list_4")
        liElements.get(4).html() mustBe messages("reoptin_page54.fg_page.list_5")
      }

      "cohort is ReOptInPage55" in new Setup {
        val viewDocReOptInPage55: Document = viewDocument(ReOptInPage55)

        viewDocReOptInPage55.getElementById("online-para-1").html() mustBe messages(
          "reoptin_page55.fg_page.yes_send_email_info_1"
        )

        viewDocReOptInPage55.getElementById("online-para-2").html() must include(
          messages(
            "reoptin_page55.fg_page.yes_send_email_info_2"
          )
        )

        viewDocReOptInPage55.getElementById("online-para-3").html() must include(
          messages(
            "reoptin_page55.fg_page.yes_send_email_info_3"
          )
        )

        viewDocReOptInPage55.getElementById("online-para-4").html() must include(
          messages(
            "reoptin_page55.fg_page.yes_send_email_info_5"
          )
        )

        val uiElements: Elements = viewDocReOptInPage55.getElementsByTag("ul")
        val liElements: Elements = uiElements.get(0).getElementsByTag("li")

        liElements.get(0).html() mustBe messages("reoptin_page55.fg_page.list_1")
        liElements.get(1).html() mustBe messages("reoptin_page55.fg_page.list_2")
        liElements.get(2).html() mustBe messages("reoptin_page55.fg_page.list_3")
        liElements.get(3).html() mustBe messages("reoptin_page55.fg_page.list_4")
        liElements.get(4).html() mustBe messages("reoptin_page55.fg_page.list_5")
      }
    }

    "throw exception for invalid cohort value" in new Setup {
      intercept[Exception] {
        viewDocument(CYSConfirmPage47)
      }.getMessage must be(s"Unexpected page: $CYSConfirmPage47")
    }
  }

  trait Setup {
    val view: sa_printing_preference = app.injector.instanceOf[sa_printing_preference]

    implicit val hostContext: HostContext = HostContext(
      returnUrl = "someReturnUrl",
      returnLinkText = "someReturnLinkText"
    )

    val form: Form[EmailForm.Data] = EmailForm()
    val submitCall: Call = routes.ActivationController.preferences()

    def viewDocument(cohort: OptInCohort): Document = Jsoup.parse(view.apply(form, submitCall, cohort).body)
  }
}
