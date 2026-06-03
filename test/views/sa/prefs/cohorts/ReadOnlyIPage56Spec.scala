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
import org.jsoup.select.Elements
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.cohorts.i_page56

class ReadOnlyIPage56Spec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  implicit lazy val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")

  val template: i_page56 = app.injector.instanceOf[i_page56]

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

//      document
//        .getElementsByTag("title")
//        .text() mustBe "Choose how to get your tax letters"
      document
        .getElementsByTag("h1")
        .first()
        .text() mustBe "Choose how to get your tax letters"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "You can now get your tax letters and information online instead of by post."
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "This means your essential tax letters are:"

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
      document
        .getElementById("sps-opt-in-item-hint")
        .text() mustBe "Give your preferred email address next. You can change it any time. We will email you when you have a new online letter."
      document.getElementsByClass("govuk-radios__label").first().text() mustBe "Online"
      document
        .getElementById("online-para-1")
        .text() mustBe "Because we cannot send all letters online yet, you will still get some by post."
      document
        .getElementById("online-para-2")
        .text() mustBe "Your responsibilities if you choose online"
      document
        .getElementById("online-para-3")
        .text() mustBe "1. Sign in to read Because email cannot include personal information, you need to sign in to HMRC online for the details."
      document
        .getElementById("online-para-4")
        .text() mustBe "2. Take action Some online tax letters need you to act, such as reminders to file a return, or to pay penalties. Others are for information, such as changes to your personal tax code. View all online tax letters HMRC can send"
      document.getElementsByTag("a").get(0).text() mustBe "View all online tax letters HMRC can send"
      document.getElementsByClass("govuk-radios__label").get(1).text() mustBe "By post only"
      document.getElementsByAttributeValue("name", "submitButton").text() mustBe "Continue"

      val h1Tag: Elements = document.getElementsByTag("h1")
      val h1HeadingValue = h1Tag.get(0).getElementsByClass("govuk-heading-l")

      h1HeadingValue.text() mustBe "Choose how to get your tax letters"

      val h2Tag: Elements = document.getElementsByTag("h2")
      val h2HeadingValue = h2Tag.get(0).getElementsByClass("govuk-heading-l")

      h2HeadingValue.text() mustBe "Your responsibilities if you choose online"

      val h3Tag: Elements = document.getElementsByTag("h3")
      val h3HeadingValue = h3Tag.get(0).getElementsByClass("govuk-heading-m")

      h3HeadingValue.text() mustBe "1. Sign in to read"

      val radioButtonHeading = h3Tag.get(2).getElementsByClass("govuk-heading-m")

      radioButtonHeading.text() mustBe "How do you want to get your tax letters?"
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
//      document
//        .getElementsByTag("title")
//        .text() mustBe "Dewis sut i gael eich llythyrau treth"
      document
        .getElementsByTag("h1")
        .first()
        .text() mustBe "Dewis sut i gael eich llythyrau treth"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "Erbyn hyn gallwch gael eich llythyrau treth a gwybodaeth ar-lein yn lle drwy’r post."
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "Mae hyn yn golygu bod eich llythyrau treth hanfodol:"

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
      document
        .getElementById("sps-opt-in-item-hint")
        .text() mustBe "Rhowch eich cyfeiriad e-bost dewisol nesaf. Gallwch ei newid ar unrhyw adeg. Byddwn yn anfon e-bost atoch pan fydd gennych lythyr ar-lein newydd."
      document.getElementsByClass("govuk-radios__label").first().text() mustBe "Ar-lein"
      document
        .getElementById("online-para-1")
        .text() mustBe "Oherwydd na allwn anfon pob llythyr ar-lein eto, byddwch yn dal i gael rhai llythyrau drwy’r post."
      document
        .getElementById("online-para-2")
        .text() mustBe "Eich cyfrifioldebau os dewiswch gael llythyrau ar-lein"
      document
        .getElementById("online-para-3")
        .text() mustBe "1. Mewngofnodi i’w darllen Oherwydd na all e-bost gynnwys gwybodaeth bersonol, bydd angen i chi fewngofnodi i CThEM ar-lein i gael y manylion."
      document
        .getElementById("online-para-4")
        .text() mustBe "2. Cymryd camau Mae rhai llythyrau treth ar-lein yn gofyn i chi weithredu, megis llythyrau yn eich atgoffa i gyflwyno Ffurflen Dreth neu dalu cosbau. Mae eraill er gwybodaeth, megis newidiadau i’ch cod treth personol. Ewch ati i fwrw golwg dros yr holl lythyrau treth ar-lein y gall CThEM eu hanfon"
      document
        .getElementsByTag("a")
        .get(0)
        .text() mustBe "Ewch ati i fwrw golwg dros yr holl lythyrau treth ar-lein y gall CThEM eu hanfon"
      document.getElementsByClass("govuk-radios__label").get(1).text() mustBe "Drwy’r post yn unig"
      document.getElementsByAttributeValue("name", "submitButton").text() mustBe "Yn eich blaen"

      val h1Tag: Elements = document.getElementsByTag("h1")
      val h1HeadingValue = h1Tag.get(0).getElementsByClass("govuk-heading-l")

      h1HeadingValue.text() mustBe "Dewis sut i gael eich llythyrau treth"

      val h2Tag: Elements = document.getElementsByTag("h2")
      val h2HeadingValue = h2Tag.get(0).getElementsByClass("govuk-heading-l")

      h2HeadingValue.text() mustBe "Eich cyfrifioldebau os dewiswch gael llythyrau ar-lein"

      val h3Tag: Elements = document.getElementsByTag("h3")
      val h3HeadingValue = h3Tag.get(0).getElementsByClass("govuk-heading-m")

      h3HeadingValue.text() mustBe "1. Mewngofnodi i’w darllen"

      val radioButtonHeading = h3Tag.get(2).getElementsByClass("govuk-heading-m")

      radioButtonHeading.text() mustBe "Sut hoffech gael eich llythyrau treth?"
    }
  }
}
