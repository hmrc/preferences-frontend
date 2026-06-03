/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import java.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIosReOptInPage37CohortSpec extends PlaySpec {

  "IosReOptInPage37 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IosReOptInPage37

      withClue("id") {
        cohortUnderTest.id mustBe 37
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IosReOptInPage37"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IosReOptInPage)
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
