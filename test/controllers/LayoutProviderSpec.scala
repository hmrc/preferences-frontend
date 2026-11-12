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

import controllers.auth.AuthenticatedRequest
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.*
import org.scalactic.source.Position
import play.api.libs.typedmap.TypedMap
import play.api.mvc.RequestHeader
import play.twirl.api.{ Html, HtmlFormat }
import utils.SpecBase

import scala.jdk.CollectionConverters.ListHasAsScala

class LayoutProviderSpec extends SpecBase {

  "layoutProvider" should {

    "display correct content" when {
      "displaySignOutLink has a default(true) value" in new TestCase {
        implicit val hostContext: HostContext = createHostContext(None, None)

        val result = layoutProvider.provideLayout(content, title).toString

        result must include("Sign out")
        result must include("Back")
        result must include("<h1>")

        val datatimeOutURl = getTimeOutUrl(result)
        datatimeOutURl mustBe List(
          "/paperless/timeout?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D"
        )

      }

      "displaySignOutLink is false" in new TestCase {
        implicit val hostContext: HostContext = createHostContext(None, None)

        val result = layoutProvider
          .provideLayout(
            content = content,
            title = title,
            displaySignOutLink = false
          )
          .toString

        result must not include "Sign out"
        result must include("Back")
        result must include("<h1>")

        val datatimeOutURl = getTimeOutUrl(result)
        datatimeOutURl mustBe List(
          "/paperless/timeout?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D"
        )

      }

      "disableSessionExpired is true" in new TestCase {
        implicit val hostContext: HostContext = createHostContext(None, None)

        val result = layoutProvider
          .provideLayout(
            content = content,
            title = title,
            displaySignOutLink = false,
            disableSessionExpired = true
          )
          .toString

        val viewDoc: Document = Jsoup.parse(result)

        result must not include "Sign out"
        result must include("Back")
        result must include("<h1>")

        shouldNotContainTimeOutDialogDetails(viewDoc)

        val datatimeOutURl = getTimeOutUrl(result)
        datatimeOutURl mustBe List.empty

      }

      "disableSessionExpired has default(false) value" in new TestCase {
        implicit val hostContext: HostContext = createHostContext(maybeServiceUrl = None, maybeRegime = None)

        val result = layoutProvider
          .provideLayout(
            content = content,
            title = title,
            displaySignOutLink = false
          )
          .toString

        val viewDoc: Document = Jsoup.parse(result)

        result must not include "Sign out"
        result must include("Back")
        result must include("<h1>")

        shouldContainTimeOutDialogDetails(viewDoc)

        val datatimeOutURl = getTimeOutUrl(result)
        datatimeOutURl mustBe List(
          "/paperless/timeout?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D"
        )
      }

      "all values in the serviceurl and regime are in the params and copied used in the timeout url" in new TestCase {
        implicit val hostContext: HostContext =
          createHostContext(maybeServiceUrl = Some("http://service-url"), maybeRegime = Some("itsa"))

        val result = layoutProvider
          .provideLayout(
            content = content,
            title = title,
            displaySignOutLink = false
          )
          .toString

        val datatimeOutURl = getTimeOutUrl(result)
        datatimeOutURl mustBe List(
          "/paperless/timeout?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&regime=KucfrgeglpOjHad59vo1xg%3D%3D&serviceUrl=QK70Zfm%2BLYTZMSuFZSJmTHLQxyxnLhNRXtljGbalg1A%3D"
        )
      }

    }

    "build the page title" when {
      "appendServiceName is not set" in new TestCase {

        implicit val hostContext: HostContext = createHostContext(None, None)

        val result = layoutProvider.provideLayout(content, title).toString

        result must include(s"<title>$title</title>")

        val datatimeOutURl = getTimeOutUrl(result)
        datatimeOutURl mustBe List(
          "/paperless/timeout?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D"
        )
      }

      "appendServiceName is false and the regime is itsa and there is no service url" in new TestCase {
        implicit val hostContext: HostContext = createHostContext(maybeServiceUrl = None, maybeRegime = Some("itsa"))

        val result: HtmlFormat.Appendable =
          layoutProvider.provideLayout(content, title, appendServiceName = false)(
            messages,
            requestMock,
            hostContext
          )

        result.toString must include(s"<title>$title</title>")
      }

      "appendServiceName is true and there is no regime" in new TestCase {
        implicit val hostContext: HostContext = createHostContext(None, None)

        val result =
          layoutProvider.provideLayout(content, title, appendServiceName = true).toString

        result must include(s"<title>$title – GOV.UK</title>")

        val datatimeOutURl = getTimeOutUrl(result)
        datatimeOutURl mustBe List(
          "/paperless/timeout?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D"
        )
      }

      "appendServiceName is true and the regime is itsa" in new TestCase {
        implicit val hostContext: HostContext = createHostContext(maybeServiceUrl = None, maybeRegime = Some("itsa"))

        val result =
          layoutProvider
            .provideLayout(content, title, appendServiceName = true)(
              messages,
              requestMock,
              hostContext
            )
            .toString

        result must include(
          s"<title>$title – Sign up for Making Tax Digital for Income Tax – GOV.UK</title>"
        )

        val datatimeOutURl = getTimeOutUrl(result)
        datatimeOutURl mustBe List(
          "/paperless/timeout?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&regime=KucfrgeglpOjHad59vo1xg%3D%3D"
        )
      }
    }

    def shouldNotContainTimeOutDialogDetails(viewDoc: Document)(implicit position: Position) =
      viewDoc.getElementsByAttributeValueContaining("name", "hmrc-timeout-dialog").size() mustBe 0

    def shouldContainTimeOutDialogDetails(viewDoc: Document)(implicit position: Position) =
      viewDoc.getElementsByAttributeValueContaining("name", "hmrc-timeout-dialog").size() mustBe 1

    def getTimeOutUrl(result: String): List[String] = {
      val viewDocument = Jsoup.parse(result)
      viewDocument
        .getElementsByAttributeValue("name", "hmrc-timeout-dialog")
        .asScala
        .toList
        .map(_.attributes().get("data-timeout-url"))
    }

  }

  trait TestCase {

    val dynamicSigninWrapperServiceFactory: DynamicSigninWrapperServiceFactory =
      app.injector.instanceOf[DynamicSigninWrapperServiceFactory]
    val content = Html("<h1></h1>")
    val title = "test_title"

    implicit val requestHeaders: RequestHeader = fakeRequest().withAttrs(TypedMap.empty)
    implicit val requestMock: AuthenticatedRequest[?] = mock[AuthenticatedRequest[?]]

    def createHostContext(maybeServiceUrl: Option[String], maybeRegime: Option[String]): HostContext =
      HostContext("", "", regime = maybeRegime, serviceUrl = maybeServiceUrl)

    when(requestMock.attrs).thenReturn(TypedMap.empty)
    when(requestMock.rawQueryString).thenReturn("")

    val layoutProvider: LayoutProvider = app.injector.instanceOf[LayoutProvider]
  }
}
