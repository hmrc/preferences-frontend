/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.PageType
import java.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyIPage8CohortSpec extends PlaySpec {

  "IPage8 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = IPage8

      withClue("id") {
        cohortUnderTest.id mustBe 8
      }
      withClue("name") {
        cohortUnderTest.name mustBe "IPage8"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.IPage)
      }
      withClue("majorVersion") {
        cohortUnderTest.majorVersion mustBe 1
      }
      withClue("minorVersion") {
        cohortUnderTest.minorVersion mustBe 0
      }
      withClue("description") {
        cohortUnderTest.description mustBe "SOL changes to wording to improve litigation cases"
      }
      withClue("date") {
        cohortUnderTest.date mustBe LocalDate.parse("2020-05-12")
      }
      withClue("journeyType") {
        cohortUnderTest.journeyType mustBe None
      }
    }
  }

}
