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

package controllers

import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }

import java.time.{ ZoneOffset, ZonedDateTime }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, Retrieval, ~ }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ ExecutionContext, Future }

class AuthControllerSpecs extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with AuthRetrievalsSetup {

  val fakeRequest = FakeRequest("GET", "/")

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()
  class FakeController(val authConnector: AuthConnector, mcc: MessagesControllerComponents)(implicit
    ec: ExecutionContext
  ) extends FrontendController(mcc) with WithAuthRetrievals {
    def onPageLoad() =
      Action.async { implicit request =>
        withAuthenticatedRequest { (_: AuthenticatedRequest[?]) => (_: HeaderCarrier) =>
          Future.successful(Ok)
        }
      }
  }
  val mcc = app.injector.instanceOf[MessagesControllerComponents]
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val controller = new FakeController(mockAuthConnector, mcc)

  "Auth Action" when {
    "the user has authenticated should return a successful response" in {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)
      val result = controller.onPageLoad()(fakeRequest)
      status(result) mustBe OK
    }

    "return not authorised then no credentials supplied" in {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.failed(SessionRecordNotFound()))
      val result = controller.onPageLoad()(fakeRequest)
      status(result) mustBe 401
    }

  }
}
