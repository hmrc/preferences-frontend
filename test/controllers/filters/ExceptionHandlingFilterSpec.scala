/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.filters

import org.apache.pekko.util.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.http.NotFoundException

import java.util.concurrent.TimeUnit.SECONDS
import scala.concurrent.Future

class ExceptionHandlingFilterSpec
    extends AnyWordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite with ScalaFutures {

  implicit val timeout: Timeout = Timeout(5, SECONDS)
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .build()
  val exceptionHandlingFilter = app.injector.instanceOf[ExceptionHandlingFilter]

  "ExceptionHandlingFilter" should {

    "not recover when there is an UnhealthyServiceException thrown" in {
      val returnUrl = "Wa6yuBSzGvUaibkXblJ8aQ%3D%3D"
      val fakeRequest = FakeRequest("GET", s"testUrl?returnUrl=$returnUrl")

      val actionException = new UnhealthyServiceException("She kanna take any more captain!")

      val filterResult = exceptionHandlingFilter(_ => Future.failed(actionException))(fakeRequest)

      filterResult.failed.futureValue shouldBe actionException
    }

    "recover and redirect to the returnUrl if there is an exception thrown" in {
      val returnUrl = "Wa6yuBSzGvUaibkXblJ8aQ%3D%3D"
      val fakeRequest = FakeRequest("GET", s"testUrl?returnUrl=$returnUrl")

      val filterResult: Future[Result] =
        exceptionHandlingFilter(_ => Future.failed(new NotFoundException("Unable to parse preferences")))(fakeRequest)

      Helpers.redirectLocation(filterResult) shouldBe Some("foo&value")
    }

    "not recover if there is an exception thrown and returnUrl is not present in the request" in {
      val fakeRequest = FakeRequest("GET", s"testUrl")
      val actionException = new RuntimeException

      val filterResult = exceptionHandlingFilter(_ => Future.failed(actionException))(fakeRequest)

      filterResult.failed.futureValue shouldBe actionException
    }
  }
}
