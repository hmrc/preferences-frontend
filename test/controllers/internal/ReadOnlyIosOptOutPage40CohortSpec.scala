/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import java.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosOptOutPage40CohortSpec extends PlaySpec {

  "IosOptOut40 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosOptOutPage40

      withClue("id") {
        cohortUnderTest.id mustBe 40
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IosOptOutPage40"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosOptOutPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 1
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 2
      }
      withClue("description") {
        cohortUnderTest.description mustBe ""
      }
      withClue("date") {
        cohortUnderTest.date mustBe LocalDate.parse("2020-01-01")
      }
    }
  }

}
