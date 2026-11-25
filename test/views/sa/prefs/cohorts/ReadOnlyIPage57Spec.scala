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

import controllers.internal
import controllers.internal.EmailForm
import helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.cohorts.i_page57

class ReadOnlyIPage57Spec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  implicit lazy val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")

  val template: i_page57 = app.injector.instanceOf[i_page57]

  "I Page Template" should {
    "render the correct content in english" in {
      val form = EmailForm()
      val document = Jsoup.parse(
        template(
          form,
          internal.paperless.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext)
        )(
          engRequest,
          messagesInEnglish(),
          hostContext
        ).toString()
      )

      document
        .getElementsByTag("h1")
        .first()
        .text() mustBe "We're going paperless"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "You can choose to get some of your HMRC tax letters and messages online. We'll email you to let you know when you've got a digital letter."
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "Going paperless is fast, secure and saves paper."

      document
        .getElementById("online-para-1")
        .text() mustBe "We cannot send all letters online yet, so you'll still get some by post."
      document
        .getElementById("online-para-2")
        .text() mustBe "Your responsibilities if you go paperless"
      document
        .getElementById("online-para-3")
        .text() mustBe "1. Sign in to read Email cannot include personal information, so you need to sign in to HMRC online for the details."
      document
        .getElementById("online-para-4")
        .text() mustBe "2. Take action Some online tax letters need you to act, such as reminders to file a return or to pay penalties. Others are for information, such as changes to your Income Tax or tax code."
      document
        .getElementById("online-para-5")
        .text() mustBe "We'll ask for your preferred email address. You can change it any time. We'll email you when you have a new online letter."

      document.getElementsByAttributeValue("name", "sps-opt-in").text() mustBe "Go paperless Get tax letters by post"
    }

    "render the correct content in welsh" in {
      val form = EmailForm()
      val document = Jsoup.parse(
        template(
          form,
          internal.paperless.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext)
        )(
          welshRequest,
          messagesInWelsh(),
          hostContext
        ).toString()
      )

      document
        .getElementsByTag("h1")
        .first()
        .text() mustBe "Rydym yn mynd yn ddi-bapur"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "Gallwch ddewis cael rhai o’ch llythyrau treth a’ch negeseuon oddi wrth CThEF ar-lein. Byddwn yn anfon e-bost atoch i roi gwybod i chi pan fydd gennych lythyr digidol."
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "Mae mynd yn ddi-bapur yn gyflym ac yn ddiogel, ac mae’n arbed papur."

      document
        .getElementById("online-para-1")
        .text() mustBe "Ni allwn anfon pob llythyr ar-lein eto, felly byddwch yn parhau i gael rhai drwy’r post."
      document
        .getElementById("online-para-2")
        .text() mustBe "Eich cyfrifoldebau os byddwch yn mynd yn ddi-bapur"
      document
        .getElementById("online-para-3")
        .text() mustBe "1. Mewngofnodi i’w darllen Ni ellir cynnwys gwybodaeth bersonol mewn e-bost, felly mae angen i chi fewngofnodi i wasanaethau ar-lein CThEF i weld y manylion."
      document
        .getElementById("online-para-4")
        .text() mustBe "2. Cymryd camau Mae rhai llythyrau treth ar-lein yn gofyn i chi gymryd camau, megis llythyrau yn eich atgoffa i gyflwyno Ffurflen Dreth neu dalu cosbau. Mae eraill er gwybodaeth, megis newidiadau i’ch Treth Incwm neu’ch cod treth."

      document
        .getElementById("online-para-5")
        .text() mustBe "Byddwn yn gofyn i chi am eich cyfeiriad e-bost dewisol. Gallwch ei newid ar unrhyw adeg. Byddwn yn anfon e-bost atoch pan fydd gennych lythyr newydd ar-lein."

      document
        .getElementsByAttributeValue("name", "sps-opt-in")
        .text() mustBe "Ewch yn ddi-bapur Cael llythyrau treth drwy’r post"
    }
  }
}
