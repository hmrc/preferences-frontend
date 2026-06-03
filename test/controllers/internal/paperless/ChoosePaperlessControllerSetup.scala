/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
