/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package views.manage

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import connectors.EmailPreference
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.test.FakeRequest
import views.html.manage._

class DigitalTruePendingFullSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template: digital_true_pending_full = app.injector.instanceOf[digital_true_pending_full]

  "settings page for digital true pending" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)

      val authRequest = AuthenticatedRequest(FakeRequest(), None, None, None, None)
      val document =
        Jsoup.parse(template(email, "pta")(authRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())
      document.getElementsByClass("govuk-link").get(1).attr("href") must be(
        "/paperless/email-re-verify?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      )
      document.getElementById("saCheckSettings").text() mustBe "Check your settings"
      document.getElementsByClass("govuk-button").text() mustBe "Continue"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document =
        Jsoup.parse(template(email, "pta")(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementById("saCheckSettings").text() mustBe "Gwirio’ch gosodiadau"
      document.getElementsByClass("govuk-button").text() mustBe "Yn eich blaen"
    }
  }

  "settings page for digital false" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)

      val authRequest = AuthenticatedRequest(FakeRequest(), None, None, None, None)
      val documentPta =
        Jsoup.parse(template(email, "pta")(authRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      documentPta.getElementById("saCheckSettings").text() mustBe "Check your settings"
      documentPta.getElementsByClass("govuk-button").text() mustBe "Continue"

      val documentBta =
        Jsoup.parse(template(email, "bta")(authRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      documentBta.getElementById("saCheckSettings").text() mustBe "Check your settings"
      documentBta.getElementsByClass("govuk-button").text() mustBe "Return to your business tax account details"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val documentPta =
        Jsoup.parse(template(email, "pta")(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      documentPta.getElementById("saCheckSettings").text() mustBe "Gwirio’ch gosodiadau"
      documentPta.getElementsByClass("govuk-button").text() mustBe "Yn eich blaen"

      val documentBta =
        Jsoup.parse(template(email, "bta")(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      documentBta.getElementById("saCheckSettings").text() mustBe "Gwirio’ch gosodiadau"
      documentBta.getElementsByClass("govuk-button").text() mustBe "Yn ôl i fanylion eich cyfrif treth busnes"
    }
  }
}
