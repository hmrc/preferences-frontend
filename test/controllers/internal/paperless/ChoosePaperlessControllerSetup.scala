/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package controllers.internal.paperless

import controllers.AuthRetrievalsSetup
import controllers.internal.{ CohortCurrent, IPage53, OptInCohort }
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{ AffinityGroup, ConfidenceLevel }
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, ~ }
import uk.gov.hmrc.domain.SaUtr

import java.time.{ ZoneOffset, ZonedDateTime }
import scala.concurrent.Future

trait ChoosePaperlessControllerSetup extends AuthRetrievalsSetup {
  val validUtr: SaUtr = SaUtr("1234567890")
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  def assignedCohort: OptInCohort = CohortCurrent.ipage
  def cohort53: OptInCohort = IPage53
}
