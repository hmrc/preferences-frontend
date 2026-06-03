/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import java.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidOptOutPage20CohortSpec extends PlaySpec {

  "AndroidOptOut20 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidOptOutPage20

      withClue("id") {
        cohortUnderTest.id mustBe 20
      }
      withClue("name") {
        cohortUnderTest.name mustBe "AndroidOptOutPage20"
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
        cohortUnderTest.minorVersion mustBe 1
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
