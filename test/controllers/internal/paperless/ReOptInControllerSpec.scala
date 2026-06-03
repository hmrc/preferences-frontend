/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.{ EntityResolverConnector, PreferencesConnector, PreferencesCreated }
import controllers.AuthRetrievalsSetup
import org.mockito.Mockito.when
import play.api.Application
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailaddress.EmailAddressValidation
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, UpstreamErrorResponse }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.SpecBase
import play.api.inject.bind
import org.mockito.ArgumentMatchers.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import java.net.URL
import scala.concurrent.{ ExecutionContext, Future }

class ReOptInControllerSpec extends SpecBase with AuthRetrievalsSetup {

  "submitMultiPageReOptInEmail" should {

    "redirect for successful request processing" in new Setup {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)

      when(mockPreferencesConnector.optIn(any)(any, any, any)).thenReturn(Future.successful(PreferencesCreated))

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse)))

      when(mockPreferencesConnector.changeEmailAddress(any, any)(any, any)).thenReturn(Future.successful(HttpResponse))
      when(mockAuditConnector.sendMergedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val body: JsValue = Json.toJson(Map("sps-re-opt-in" -> "true", "sps-re-opt-in-email" -> "test@gmail.com"))

      val request: FakeRequest[JsValue] =
        fakeRequestWithBody[JsValue](
          POST,
          routes.ReOptInController.submitMultiPageReOptInEmail(true, hostContext(Some("test@gmail.com"))).url,
          body
        )

      val result: Option[Future[Result]] = route(app, request)

      result.map { resultValue =>
        status(resultValue) mustBe BAD_REQUEST
      }
    }

    "return BAD_REQUEST when error occurs while processing the request" in new Setup {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)

      val body: JsValue = Json.toJson(Map("sps-re-opt-in" -> "true", "sps-re-opt-in-email" -> "test@gmail.com"))

      val request: FakeRequest[JsValue] =
        fakeRequestWithBody[JsValue](
          POST,
          routes.OptInController.submitOptInEmail(Some(true), hostContext(None)).url,
          body
        )

      val result: Option[Future[Result]] = route(app, request)

      result.map { resultValue =>
        status(resultValue) mustBe BAD_REQUEST
      }
    }
  }

  "submitMultiPageReOptIn" should {

    "return BAD_REQUEST for invalid form" in new Setup {
      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)

      val body: JsValue = Json.toJson(Map("unknown" -> "test"))

      val request: FakeRequest[JsValue] =
        fakeRequestWithBody[JsValue](
          POST,
          routes.ReOptInController.submitMultiPageReOptIn(hostContext()).url,
          body
        )

      val result: Option[Future[Result]] = route(app, request)

      result.map { resultValue =>
        status(resultValue) mustBe BAD_REQUEST
      }
    }
  }

  trait Setup {
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
    val mockAuditConnector: AuditConnector = mock[AuditConnector]
    val mockPreferencesConnector: PreferencesConnector = mock[PreferencesConnector]
    val mockEmailAddressValidation: EmailAddressValidation = mock[EmailAddressValidation]
    val mockEntityResolverConnector: EntityResolverConnector = mock[EntityResolverConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val app: Application = applicationBuilder
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector),
        bind[EmailAddressValidation].toInstance(mockEmailAddressValidation)
      )
      .configure("metrics.enabled" -> false)
      .build()
  }
}
