/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.auth

import play.api.mvc.{ Request, Result, Results }
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ ExecutionContext, Future }

trait WithAuthRetrievals extends AuthorisedFunctions {
  def withAuthenticatedRequest[A](
    block: AuthenticatedRequest[A] => HeaderCarrier => Future[Result]
  )(implicit request: Request[A], ec: ExecutionContext) = {
    implicit val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised().retrieve(
      Retrievals.loginTimes and Retrievals.nino and Retrievals.saUtr and
        Retrievals.affinityGroup and Retrievals.confidenceLevel
    ) {
      case retLoginTimes ~ nino ~ utr ~ affinityGroup ~ confidenceLevel =>
        val previousLoginTime = retLoginTimes.previousLogin
        block(
          AuthenticatedRequest[A](request, previousLoginTime, nino, utr, affinityGroup, Some(confidenceLevel))
        )(hc)
      case null => Future.successful(Results.Unauthorized)
    }
  }.recover {
    case _: InsufficientConfidenceLevel => Results.Unauthorized
    case _: UnsupportedAffinityGroup    => Results.Unauthorized
    case _: UnsupportedCredentialRole   => Results.Unauthorized
    case _: UnsupportedAuthProvider     => Results.Unauthorized
    case _: BearerTokenExpired          => Results.Unauthorized
    case _: MissingBearerToken          => Results.Unauthorized
    case _: InvalidBearerToken          => Results.Unauthorized
    case _: SessionRecordNotFound       => Results.Unauthorized
    case _: IncorrectCredentialStrength => Results.Unauthorized
    case _: InsufficientEnrolments      => Results.Unauthorized
    case e                              => throw e
  }

}
