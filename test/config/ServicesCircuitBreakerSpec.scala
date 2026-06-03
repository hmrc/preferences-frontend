/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package config

import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.mvc.Http.Status._
import uk.gov.hmrc.http.{ BadRequestException, NotFoundException, UpstreamErrorResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.ConnectException

class ServicesCircuitBreakerSpec extends PlaySpec {

  "ServicesCircuitBreaker" should {
    "doesn't count BAD_REQUEST Upstream exception" in new TestCase {
      val exception = new BadRequestException("bad request")
      circuitBreaker.breakOnExceptionDelegate(exception) must be(false)
    }

    "doesn't count NOT_FOUND Upstream exception" in new TestCase {
      val exception = new NotFoundException("not found")
      circuitBreaker.breakOnExceptionDelegate(exception) must be(false)
    }

    "doesn't count any Upstream4xx exception different than BAD_REQUEST" in new TestCase {
      val exception = UpstreamErrorResponse(
        message = "",
        statusCode = NOT_FOUND,
        reportAs = NOT_FOUND,
        headers = Map.empty
      )
      circuitBreaker.breakOnExceptionDelegate(exception) must be(false)
    }

    "count exception for 5xx" in new TestCase {
      val exception = UpstreamErrorResponse(
        message = "",
        statusCode = INTERNAL_SERVER_ERROR,
        reportAs = INTERNAL_SERVER_ERROR
      )
      circuitBreaker.breakOnExceptionDelegate(exception) must be(true)
    }

    "count any non Upstream exception" in new TestCase {
      circuitBreaker.breakOnExceptionDelegate(new ConnectException()) must be(true)
    }
  }

  trait TestCase {
    val config = Configuration.empty

    class TestServicesCircuitBreaker() extends ServicesConfig(Configuration.empty) with ServicesCircuitBreaker {
      override protected val externalServiceName: String = "test service"

      def breakOnExceptionDelegate(t: Throwable): Boolean = breakOnException(t)
    }
    val circuitBreaker = new TestServicesCircuitBreaker()

  }
}
