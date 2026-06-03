/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.internal.paperless

import controllers.internal.CohortCurrent
import helpers.Resources
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers.{ contentAsJson, defaultAwaitTimeout, status }

class ChoosePaperlessControllerSpecAdmin extends PlaySpec with GuiceOneAppPerSuite with ChoosePaperlessControllerSetup {

  val controller = app.injector.instanceOf[ChoosePaperlessController]

  "/paperless/opt-in-cohort/display/:cohort" should {

    "display form of the current ipage cohort" in {
      val request = FakeRequest()
      val page = controller.displayCohort(Some(CohortCurrent.ipage))(request)
      status(page) mustBe 200
    }
    "return BadRequest if cohort is missing" in {
      val request = FakeRequest()
      val page = controller.displayCohort(None)(request)
      status(page) mustBe 400
    }
  }

  "/paperless/opt-in-cohort/list" should {

    "return list of available cohorts" in {
      val request = FakeRequest()
      val page = controller.cohortList()(request)
      status(page) mustBe 200
      contentAsJson(page) mustBe (
        Resources.readJson("CohortList.json")
      )
    }
  }

}
