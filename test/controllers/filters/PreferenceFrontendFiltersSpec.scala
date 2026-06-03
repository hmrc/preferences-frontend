/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package controllers.filters

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HttpFilters
import play.api.inject.guice.GuiceApplicationBuilder
import play.filters.csp.CSPFilter

class PreferenceFrontendFiltersSpec
    extends AnyWordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite with ScalaFutures {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().build()

  private val frontendFilters = app.injector.instanceOf[PreferencesFrontendFilters]
  private val exceptionHandlingFilter = app.injector.instanceOf[ExceptionHandlingFilter]
  private val cspFilter = app.injector.instanceOf[CSPFilter]

  "PreferencesFrontendFilters" should {

    "extend HttpFilters" in {
      frontendFilters shouldBe a[HttpFilters]
    }

    "contain the ExceptionHandlingFilter followed by the CSPFilter" in {
      frontendFilters.filters shouldBe Seq(exceptionHandlingFilter, cspFilter)
    }

    "contain exactly 2 filters" in {
      frontendFilters.filters should have size 2
    }
  }
}
