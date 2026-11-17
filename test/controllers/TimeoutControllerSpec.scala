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

import model.TimeoutHostContext
import org.scalactic.source.Position
import org.scalatest.OptionValues
import play.api.mvc.{ AnyContentAsEmpty, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.SpecBase

import scala.concurrent.Future

class TimeoutControllerSpec extends SpecBase with OptionValues {
  private val serviceUrl = "http://testurl"
  private val serviceName = messagesInEnglish("sa_printing_preference_svc_name_itsa")
  private val serviceNameHtmlWithoutServiceUrl = s"""<span class="govuk-service-navigation__text">$serviceName</span>"""
  private val serviceNameHtmlWithServiceUrl =
    s"""<a href="$serviceUrl" class="govuk-service-navigation__link"> $serviceName </a>"""

  "timeout" should {

    "have no service name" when {

      "there is no regime" in {
        val noRegimeUrl = routes.TimeoutController.timeout(TimeoutHostContext(serviceUrl = None, regime = None)).url
        testNoServiceName(noRegimeUrl)
      }

      def testNoServiceName(url: String)(implicit position: Position) = {
        val request = fakeRequest(GET, url)

        val result: Future[Result] = route(app, request).value

        val viewContent = contentAsString(result)
        viewContent must include(messagesInEnglish("session.timeout.heading"))
        viewContent must not include "Back"
        viewContent must not include "Sign out"
        viewContent must not include "hmrc-timeout-dialog"

        status(result) mustBe OK
      }

      "regime is not itsa" in {
        val noRegimeUrl =
          routes.TimeoutController.timeout(TimeoutHostContext(serviceUrl = None, regime = Some("not-itsa"))).url
        testNoServiceName(noRegimeUrl)
      }

    }

    "show page title when the regime is itsa but with no link as there is no service url" in {
      val url = routes.TimeoutController.timeout(TimeoutHostContext(serviceUrl = None, regime = Some("itsa"))).url
      println(url)
      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(
          GET,
          url
        )

      val result: Future[Result] = route(app, request).value

      val content = contentAsString(result)
      content must include(
        s"${messagesInEnglish("session.timeout.heading")} – $serviceName – GOV.UK</title>"
      )

      content must include(
        serviceNameHtmlWithoutServiceUrl
      )

      content must not include serviceNameHtmlWithServiceUrl

      status(result) mustBe OK
    }

    "show page title when the regime is itsa but with a link as there is a service url" in {
      val url = routes.TimeoutController
        .timeout(TimeoutHostContext(serviceUrl = Some(serviceUrl), regime = Some("itsa")))
        .url

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(
          GET,
          url
        )

      val result: Future[Result] = route(app, request).value

      val content = contentAsString(result)

      content must include(
        s"${messagesInEnglish("session.timeout.heading")} – $serviceName – GOV.UK</title>"
      )

      content must not include
        serviceNameHtmlWithoutServiceUrl

      content must include(
        serviceNameHtmlWithServiceUrl
      )

      status(result) mustBe OK
    }

  }

  "keepAliveSession" should {
    "return NoContent" in {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(GET, routes.TimeoutController.keepAliveSession().url)

      val result: Future[Result] = route(app, request).value
      status(result) mustBe NO_CONTENT
    }
  }

}
