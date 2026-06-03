/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package partial.paperless.manage

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import partial.paperless.manage.html.digital_true_links
import play.api.Application

class DigitalTrueLinksSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_true_links]

  "digital true links partial" should {
    "render the correct content in english" in {
      val linkId = "test_id"
      val document = Jsoup.parse(
        template(linkId)(messagesInEnglish(), TestFixtures.sampleHostContext).toString()
      )

      document.getElementById(linkId).text() mustBe "Change your email address"
      document.getElementById("opt-out-of-email-link").text() mustBe "Stop emails from HMRC"
    }

    "render the correct content in welsh" in {
      val linkId = "test_id"
      val document =
        Jsoup.parse(template(linkId)(messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementById(linkId).text() mustBe "Newid eich cyfeiriad e-bost"
      document.getElementById("opt-out-of-email-link").text() mustBe "Atal e-byst gan CThEM"
    }
  }
}
