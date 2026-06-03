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

package utils

import controllers.auth.AuthenticatedRequest
import controllers.internal.ReOptInPage54
import model.HostContext
import org.apache.pekko.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.{ Lang, Messages, MessagesApi, MessagesImpl }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{ GET, POST }
import play.api.test.{ FakeHeaders, FakeRequest }
import utils.TestData.{ EMPTY_STRING, TEST_EMAIL_VALUE, TEST_LINK_TEXT, TEST_URL }

trait SpecBase extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {
  lazy val applicationBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()

  lazy val app: Application = applicationBuilder
    .configure(
      "metrics.enabled" -> "false"
    )
    .build()

  implicit val messageApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messageApi.preferred(fakeRequest())

  val messagesInWelsh: Messages = MessagesImpl(Lang("cy"), messageApi)
  val messagesInEnglish: Messages = MessagesImpl(Lang("en"), messageApi)

  implicit lazy val system: ActorSystem = ActorSystem("system")

  implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(fakeRequest(), None, None, None, None)

  def fakeRequest(method: String = EMPTY_STRING, path: String = EMPTY_STRING): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path)
      .asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
      .withHeaders(newHeaders = "X-Session-Id" -> "someSessionId")

  def fakeRequestWithBody[A](method: String = GET, path: String = EMPTY_STRING, body: A): FakeRequest[A] =
    FakeRequest(method, path, FakeHeaders(), body)

  def hostContext(email: Option[String] = Some(TEST_EMAIL_VALUE)): HostContext = HostContext(
    returnUrl = TEST_URL,
    returnLinkText = TEST_LINK_TEXT,
    email = email,
    cohort = Some(ReOptInPage54)
  )
}
