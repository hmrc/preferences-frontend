/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.GenericTerms
import model.JourneyType.MultiPage2
import model.PageType
import java.time.LocalDate
import org.scalatestplus.play.PlaySpec

class ReadOnlyReOptInPage54CohortSpec extends PlaySpec {

  "ReOptInPage54 OptInCohort" should {
    "never change fields values " in {
      val cohortUnderTest = ReOptInPage54

      withClue("id") {
        cohortUnderTest.id mustBe 54
      }
      withClue("name") {
        cohortUnderTest.name mustBe "ReOptInPage54"
      }
      withClue("terms") {
        cohortUnderTest.terms mustBe GenericTerms
      }
      withClue("pageType") {
        cohortUnderTest.pageType mustBe (PageType.ReOptInPage)
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
        cohortUnderTest.date mustBe LocalDate.parse("2021-05-05")
      }
      withClue("journeyType") {
        cohortUnderTest.journeyType mustBe Some(MultiPage2)
      }
    }
  }

}
