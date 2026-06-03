/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import model.HostContext
import org.scalatestplus.play.PlaySpec

class OptInCohortCalculatorSpec extends PlaySpec {

  "The calculator" should {
    "Return the paperless interrupt if t&c is generic" in {
      new OptInCohortCalculator {}.calculateCohort(HostContext("", "", Some("generic"))) mustBe CohortCurrent.ipage
    }
  }
  "Return the cohort if explicitly specified in the HostContext" in {
    new OptInCohortCalculator {}
      .calculateCohort(HostContext("", "", None, cohort = Some(ReOptInPage10))) mustBe ReOptInPage10
  }
}
