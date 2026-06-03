/*
 * Copyright 2025 HM Revenue & Customs
 *
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
