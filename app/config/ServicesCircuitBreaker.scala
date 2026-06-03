/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package config

import uk.gov.hmrc.circuitbreaker.{ CircuitBreakerConfig, UsingCircuitBreaker }
import uk.gov.hmrc.http.UpstreamErrorResponse.{ Upstream4xxResponse, Upstream5xxResponse }
import uk.gov.hmrc.http.{ BadRequestException, NotFoundException }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait ServicesCircuitBreaker extends ServicesConfig with UsingCircuitBreaker {

  protected val externalServiceName: String

  override protected def circuitBreakerConfig =
    CircuitBreakerConfig(
      serviceName = externalServiceName,
      numberOfCallsToTriggerStateChange =
        config(externalServiceName).getOptional[Int]("circuitBreaker.numberOfCallsToTriggerStateChange"),
      unavailablePeriodDuration = config(externalServiceName)
        .getOptional[Int]("circuitBreaker.unavailablePeriodDurationInSeconds") map (_ * 1000),
      unstablePeriodDuration = config(externalServiceName)
        .getOptional[Int]("circuitBreaker.unstablePeriodDurationInSeconds") map (_ * 1000)
    )

  override protected def breakOnException(t: Throwable): Boolean =
    t match {
      case _: BadRequestException => false
      case _: NotFoundException   => false
      case Upstream4xxResponse(_) => false
      case Upstream5xxResponse(_) => true
      case _                      => true
    }
}
