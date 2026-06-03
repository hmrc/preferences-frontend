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

package views.sa.prefs.cohorts

import controllers.internal
import controllers.internal.EmailForm
import helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import model.HostContext
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.cohorts.reoptin_page54

class ReadOnlyReOptInPage54Spec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  implicit lazy val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")

  val template = app.injector.instanceOf[reoptin_page54]
  "ReOptIn54 Template" should {
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
        .text() mustBe "Keep getting your tax letters online"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "You need to confirm if you would still like to get your tax letters online instead of by post. Getting them online means your essential tax letters are:"

      document
        .getElementsByTag("li")
        .get(0)
        .text() mustBe "delivered fast"

      document
        .getElementsByTag("li")
        .get(1)
        .text() mustBe "saved securely"

      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "easy to find"

      document
        .getElementsByTag("li")
        .get(3)
        .text() mustBe "simple to share"

      document
        .getElementsByTag("li")
        .get(4)
        .text() mustBe "ready to print, if you need proof"

      document
        .getElementsByTag("legend")
        .text() mustBe "How do you want to get your tax letters?"

      document.getElementsByClass("govuk-radios__label").first().text() mustBe "Online"

      document
        .getElementsByClass("govuk-radios__hint")
        .first()
        .text() mustBe "Whenever you have a new online letter, we will let you know by email."

      document
        .getElementsByTag("p")
        .get(1)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Because we cannot send all letters online yet, you will still get some by post."

      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "Your responsibilities"

      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "1. Sign in to read Because email cannot include personal information, you need to sign in to HMRC online for the details."

      document
        .getElementsByTag("p")
        .get(4)
        .text() mustBe "2. Take action Some online tax letters need you to act, such as reminders to file a return, or to pay penalties. Others are for information, such as changes to your personal tax code. View all online tax letters HMRC can send"

      document.getElementsByClass("govuk-radios__label").get(1).text() mustBe "By post only"

      document.getElementsByAttributeValue("name", "submitButton").text() mustBe "Continue"

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
        .text() mustBe "Parhau i gael eich llythyrau treth ar-lein"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "Mae angen i chi gadarnhau a hoffech barhau i gael eich llythyrau treth ar-lein yn hytrach na thrwy’r post. Mae eu cael ar-lein yn golygu bod eich llythyrau treth hanfodol:"

      document
        .getElementsByTag("li")
        .get(0)
        .text() mustBe "yn cael eu dosbarthu’n gyflym"

      document
        .getElementsByTag("li")
        .get(1)
        .text() mustBe "yn cael eu cadw’n ddiogel"

      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "yn hawdd i’w canfod"

      document
        .getElementsByTag("li")
        .get(3)
        .text() mustBe "yn syml i’w rhannu"

      document
        .getElementsByTag("li")
        .get(4)
        .text() mustBe "yn barod i’w hargraffu, os bydd angen tystiolaeth arnoch"

      document
        .getElementsByTag("legend")
        .text() mustBe "Sut hoffech gael eich llythyrau treth?"

      document.getElementsByClass("govuk-radios__label").first().text() mustBe "Ar-lein"

      document
        .getElementsByClass("govuk-radios__hint")
        .first()
        .text() mustBe "Pryd bynnag y bydd gennych lythyr ar-lein newydd, byddwn yn anfon e-bost i roi gwybod i chi."

      document
        .getElementsByTag("p")
        .get(1)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Oherwydd na allwn anfon pob llythyr ar-lein eto, byddwch yn dal i gael rhai llythyrau drwy’r post."

      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "Eich cyfrifoldebau"

      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "1. Mewngofnodi i’w darllen Oherwydd na all e-bost gynnwys gwybodaeth bersonol, bydd angen i chi fewngofnodi i CThEM ar-lein i gael y manylion."
      document
        .getElementsByTag("p")
        .get(4)
        .text() mustBe "2. Cymryd camau Mae rhai llythyrau treth ar-lein yn gofyn i chi weithredu, megis llythyrau yn eich atgoffa i gyflwyno Ffurflen Dreth neu dalu cosbau. Mae eraill er gwybodaeth, megis newidiadau i’ch cod treth personol. Ewch ati i fwrw golwg dros yr holl lythyrau treth ar-lein y gall CThEM eu hanfon"
      document.getElementsByClass("govuk-radios__label").get(1).text() mustBe "Drwy’r post yn unig"

      document.getElementsByAttributeValue("name", "submitButton").text() mustBe "Yn eich blaen"

    }
  }
}
