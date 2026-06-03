/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import java.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidOptOutPage44CohortSpec extends PlaySpec {

  "AndroidOptOut44 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidOptOutPage44

      withClue("id") {
        cohortUnderTest.id mustBe 44
      }
      withClue("name") {
        cohortUnderTest.name mustBe "AndroidOptOutPage44"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidOptOutPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 1
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 3
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
