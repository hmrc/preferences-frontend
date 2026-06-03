/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package controllers

import connectors.OutboundProxyConnector
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.util.ByteString
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.libs.streams.Accumulator
import play.api.mvc.{ AnyContentAsEmpty, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.SpecBase
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.http.HttpEntity.Streamed
import play.api.mvc.ResponseHeader
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class ProxyControllerSpec extends SpecBase {

  "proxy" should {
    "return OK status when request is processed successfully" in new Setup {
      when(mockOutboundProxyConnector.proxy(any))
        .thenReturn(Future.successful(Result(header = responseHeader, body = body)))

      val controller: ProxyController = application.injector.instanceOf[ProxyController]

      val result: Accumulator[ByteString, Result] = controller.proxy("test")(request)

      await(result.run()).header.status must be(OK)
    }

    "return INTERNAL_SERVER_ERROR status when InternalServerError occurs while processing" in new Setup {
      when(mockOutboundProxyConnector.proxy(any))
        .thenReturn(Future.failed(UpstreamErrorResponse("error occurred", INTERNAL_SERVER_ERROR)))

      val controller: ProxyController = application.injector.instanceOf[ProxyController]

      val result: Accumulator[ByteString, Result] = controller.proxy("test")(request)

      await(result.run()).header.status must be(INTERNAL_SERVER_ERROR)
    }
  }

  "proxy2" should {
    "return OK status when request is processed successfully" in new Setup {
      when(mockOutboundProxyConnector.proxy(any))
        .thenReturn(Future.successful(Result(header = responseHeader, body = body)))

      val controller: ProxyController = application.injector.instanceOf[ProxyController]

      val result: Accumulator[ByteString, Result] = controller.proxy2("test_predicate", "test")(request)

      await(result.run()).header.status must be(OK)
    }
  }

  trait Setup {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, "test")
    implicit val system: ActorSystem = ActorSystem("system")

    val mockOutboundProxyConnector: OutboundProxyConnector = mock[OutboundProxyConnector]

    val application: Application = applicationBuilder
      .overrides(
        bind[OutboundProxyConnector].toInstance(mockOutboundProxyConnector)
      )
      .build()

    val responseHeader: ResponseHeader = ResponseHeader(status = OK)
    val httpResponse: HttpResponse = HttpResponse()

    val body: Streamed =
      Streamed(httpResponse.entity.dataBytes, Some(400L), Some(s"${httpResponse.entity.contentType}"))
  }
}
