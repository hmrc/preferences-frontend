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

package views

import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.{ Document, Element }
import play.api.libs.json.Json
import utils.SpecBase
import views.html.sessionTimeout

class SessionTimeoutSpec extends SpecBase {

  "view" should {

    "display correct contents" in new TestCase {
      implicit val view: Document = viewAsDoc

      shouldContainCorrectTitle()
      shouldContainCorrectWarningText()
      shouldContainSignInLinkWithCorrectAddress()
    }
  }

  private def shouldContainCorrectTitle(implicit view: Document) =
    view.title() mustBe ""

  private def shouldContainCorrectWarningText(implicit view: Document) =
    view.getElementsByTag("h1").get(0).text() mustBe messagesInEnglish("session.timeout.heading")

  private def shouldContainSignInLinkWithCorrectAddress(implicit view: Document) = {
    val signInButton: Element = view.getElementById("sign-in-button")

    signInButton.text() mustBe messagesInEnglish("session.timeout.sign-in.button.text")

    signInButton.attributes().get("href") mustBe "/personal-account"
    signInButton.attributes().get("class") mustBe "govuk-button"
  }

  trait TestCase {
    val signInUrl = "/personal-account"
    implicit val hostContextOb: HostContext = hostContext()

    val viewAsDoc: Document = Jsoup.parse(app.injector.instanceOf[sessionTimeout].apply(signInUrl).body)
  }
}
