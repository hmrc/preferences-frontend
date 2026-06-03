/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import java.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyAndroidOptOutPage12CohortSpec extends PlaySpec {

  "AndroidOptOut12 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = AndroidOptOutPage12

      withClue("id") {
        cohortUnderTest.id mustBe 12
      }
      withClue("name") {
        cohortUnderTest.name mustBe "AndroidOptOutPage12"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.AndroidOptOutPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 0
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 0
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
