/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package config

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest

class ErrorHandlerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .build()

  val errorHandler = app.injector.instanceOf[ErrorHandler]

  "ErrorHandler" should {
    "show error page" in {
      val returnUrl = "invalid"
      val fakeRequest = FakeRequest("GET", s"testUrl?returnUrl=$returnUrl")
      val errorPage = errorHandler.standardErrorTemplate("ErrorTitle", "ErrorHeading", "ErrorMessage")(fakeRequest)
      errorPage.value.map(s =>
        s.get.body should include("It may be that the link you used to get here is no longer in use or incorrect")
      )
      errorPage.value.map(s => s.get.body should include("Try again later"))
      errorPage.value.map(s => s.get.body should include("Go back to your online tax account"))
    }
  }
}
