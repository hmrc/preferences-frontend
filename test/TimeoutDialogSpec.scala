/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import utils.SpecBase
import views.html.timeout_dialog

class TimeoutDialogSpec extends SpecBase {

  "view" should {
    "render the content correctly" in {
      val view: timeout_dialog = app.injector.instanceOf[timeout_dialog]

      val viewHtml: Document = Jsoup.parse(view.apply().body)

      val metaTagValue: Elements = viewHtml.getElementsByTag("meta")

      metaTagValue.attr("data-title") mustBe messages("timeout.title")
      metaTagValue.attr("data-keep-alive-url") mustBe "/session/keep-alive"
      metaTagValue.attr("data-sign-out-url") mustBe "/paperless/timeout"
      metaTagValue.attr("data-message") mustBe messages("timeout.message")
      metaTagValue.attr("data-keep-alive-button-text") mustBe messages("timeout.keep-alive-button")
    }
  }
}
