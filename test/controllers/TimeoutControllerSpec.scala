/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package controllers

import play.api.libs.json.JsValue
import play.api.mvc.{ Action, AnyContent, AnyContentAsEmpty, Headers, Result }
import play.api.test.{ FakeHeaders, FakeRequest }
import utils.SpecBase
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class TimeoutControllerSpec extends SpecBase {

  "timeout" should {
    "return OK" in {
      val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.TimeoutController.timeout().url)

      val result: Future[Result] = route(app, request).get
      status(result) mustBe OK
    }
  }

  "keepAliveSession" should {
    "return NoContent" in {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(GET, routes.TimeoutController.keepAliveSession().url)

      val result: Future[Result] = route(app, request).get
      status(result) mustBe NO_CONTENT
    }
  }
}
