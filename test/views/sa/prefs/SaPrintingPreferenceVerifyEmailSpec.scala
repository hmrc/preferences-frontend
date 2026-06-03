/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package views.sa.prefs

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import model.HostContext
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.sa_printing_preference_verify_email

class SaPrintingPreferenceVerifyEmailSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  implicit lazy val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")
  val template = app.injector.instanceOf[sa_printing_preference_verify_email]

  "printing preferences verify email template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(template(None, None)(engRequest, messagesInEnglish(), hostContext).toString())

      document.getElementById("success-heading").text() mustBe "Email address verified"
      document
        .getElementById("success-message1")
        .text() mustBe "To read your online tax letters, sign in to HMRC services and select 'Messages'."
      document.getElementById("link-to-home").child(0).text() mustBe "Continue to your HMRC online account"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(template(None, None)(welshRequest, messagesInWelsh(), hostContext).toString())

      document.getElementById("success-heading").text() mustBe "Cyfeiriad e-bost wedi'i ddilysu"
      document
        .getElementById("success-message1")
        .text() mustBe "I ddarllen eich llythyrau treth ar-lein, mewngofnodwch i wasanaethau CThEM a dewiswch 'Negeseuon'."
      document.getElementById("link-to-home").child(0).text() mustBe "Yn eich blaen i'ch cyfrif ar-lein gyda CThEM"
    }
  }
}
