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

package controllers

import config.AppConfig
import controllers.auth.AuthenticatedRequest
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.libs.typedmap.TypedMap
import play.api.mvc.{ AnyContentAsEmpty, RequestHeader }
import play.api.test.FakeRequest
import play.twirl.api.{ Html, HtmlFormat }
import uk.gov.hmrc.sca.services.WrapperService
import utils.SpecBase
import org.mockito.Mockito.*

class LayoutProviderSpec extends SpecBase {

  "layoutProvider" should {

    "display correct content" when {
      "displaySignOutLink has a default(true) value" in new TestCase {
        val result: HtmlFormat.Appendable = layoutProvider.layoutProvider(content, title)

        result.toString must include("Sign out")
        result.toString must include("Back")
        result.toString must include("<h1>")
      }

      "displaySignOutLink is false" in new TestCase {
        val result: HtmlFormat.Appendable = layoutProvider.layoutProvider(
          content = content,
          title = title,
          displaySignOutLink = false
        )

        result.toString must not include "Sign out"
        result.toString must include("Back")
        result.toString must include("<h1>")
      }

      "disableSessionExpired is true" in new TestCase {
        val result: HtmlFormat.Appendable = layoutProvider.layoutProvider(
          content = content,
          title = title,
          displaySignOutLink = false,
          disableSessionExpired = true
        )

        val viewDoc: Document = Jsoup.parse(result.toString)

        result.toString must not include "Sign out"
        result.toString must include("Back")
        result.toString must include("<h1>")

        shouldNotContainTimeOutDialogDetails(viewDoc)
      }

      "disableSessionExpired has default(false) value" in new TestCase {
        val result: HtmlFormat.Appendable = layoutProvider.layoutProvider(
          content = content,
          title = title,
          displaySignOutLink = false
        )

        val viewDoc: Document = Jsoup.parse(result.toString)

        result.toString must not include "Sign out"
        result.toString must include("Back")
        result.toString must include("<h1>")

        shouldContainTimeOutDialogDetails(viewDoc)
      }
    }

    "build the page title" when {
      "appendServiceName is not set" in new TestCase {
        val result: HtmlFormat.Appendable = layoutProvider.layoutProvider(content, title)

        result.toString must include(s"<title>$title</title>")
      }

      "appendServiceName is false and the regime is itsa" in new TestCase {
        val result: HtmlFormat.Appendable =
          layoutProvider.layoutProvider(content, title, appendServiceName = false)(
            messages,
            requestMock,
            hostContext().copy(regime = Some("itsa"))
          )

        result.toString must include(s"<title>$title</title>")
      }

      "appendServiceName is true and there is no regime" in new TestCase {
        val result: HtmlFormat.Appendable =
          layoutProvider.layoutProvider(content, title, appendServiceName = true)

        result.toString must include(s"<title>$title – GOV.UK</title>")
      }

      "appendServiceName is true and the regime is itsa" in new TestCase {
        val result: HtmlFormat.Appendable =
          layoutProvider.layoutProvider(content, title, appendServiceName = true)(
            messages,
            requestMock,
            hostContext().copy(regime = Some("itsa"))
          )

        result.toString must include(
          s"<title>$title – Sign up for Making Tax Digital for Income Tax – GOV.UK</title>"
        )
      }
    }

    def shouldNotContainTimeOutDialogDetails(viewDoc: Document) =
      viewDoc.getElementsByAttributeValueContaining("name", "hmrc-timeout-dialog").size() mustBe 0

    def shouldContainTimeOutDialogDetails(viewDoc: Document) =
      viewDoc.getElementsByAttributeValueContaining("name", "hmrc-timeout-dialog").size() mustBe 1
  }

  trait TestCase {
    val appConfigOb: AppConfig = app.injector.instanceOf[AppConfig]
    val wrapperServiceOb: WrapperService = app.injector.instanceOf[WrapperService]
    val content = Html("<h1></h1>")
    val title = "test_title"

    implicit val requestHeaders: RequestHeader = fakeRequest().withAttrs(TypedMap.empty)
    implicit val requestMock: AuthenticatedRequest[?] = mock[AuthenticatedRequest[?]]
    implicit val hostContextProvider: HostContext = hostContext()

    when(requestMock.attrs).thenReturn(TypedMap.empty)
    when(requestMock.rawQueryString).thenReturn("")

    val layoutProvider: LayoutProvider = new LayoutProvider {
      override def wrapperService: WrapperService = wrapperServiceOb

      override def appConfig: AppConfig = appConfigOb
    }
  }
}
