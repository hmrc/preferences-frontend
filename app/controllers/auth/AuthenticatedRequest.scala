/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.auth

import play.api.mvc.{ Request, WrappedRequest }
import uk.gov.hmrc.auth.core.{ AffinityGroup, ConfidenceLevel }

import java.time.Instant

case class AuthenticatedRequest[A](
  request: Request[A],
  previousLoginTime: Option[Instant],
  nino: Option[String],
  saUtr: Option[String],
  affinityGroup: Option[AffinityGroup] = None,
  confidenceLevel: Option[ConfidenceLevel] = None
) extends WrappedRequest[A](request)
