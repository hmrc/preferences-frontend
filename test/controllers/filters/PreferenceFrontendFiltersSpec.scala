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
