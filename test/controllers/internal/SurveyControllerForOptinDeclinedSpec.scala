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

package controllers.internal

import controllers.AuthRetrievalsSetup
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import helpers.TestFixtures
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.{ ArgumentCaptor, Mockito }
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{ DefaultMessagesApiProvider, Lang }
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, Retrieval, ~ }
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, ConfidenceLevel }
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ EventTypes, ExtendedDataEvent }

import java.time.{ ZoneOffset, ZonedDateTime }
import scala.concurrent.{ ExecutionContext, Future }

trait SurveyControllerForOptinDeclinedSetup {

  val validUtr = SaUtr("1234567890")
  val request = FakeRequest()

  def paramValue(url: String, param: String): Option[String] =
    url.split(Array('=', '?', '&')).drop(1).sliding(2, 2).map(x => x(0) -> x(1)).toMap.get(param)
}

class SurveyControllerForOptinDeclinedSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite
    with SurveyControllerForOptinDeclinedSetup with AuthRetrievalsSetup {

  val mockAuditConnector = mock[AuditConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  when(
    mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )
  ).thenReturn(retrievalResult)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "sso.encryption.key"          -> "P5xsJ9Nt+quxGZzB4DeLfw==",
        "sso.encryption.previousKeys" -> Seq.empty
      )
      .configure("metrics.enabled" -> false)
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[Metrics].toInstance(Mockito.mock(classOf[Metrics]))
      )
      .build()

  val messageApi = fakeApplication().injector.instanceOf[DefaultMessagesApiProvider].get
  val controller = app.injector.instanceOf[SurveyController]

  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuthConnector)
    when(
      mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(retrievalResult)
  }

  "displayOptInDeclinedSurveyForm for survey request" should {

    "show main banner" in new SurveyControllerSetup {
      val page = controller.displayOptinDeclinedSurveyForm(TestFixtures.optinHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "show survey title" in new SurveyControllerSetup {
      val optinTitle = messageApi.translate("paperless.survey.optin_declined.title", Nil)(Lang("en", "")).get
      val page = controller.displayOptinDeclinedSurveyForm(TestFixtures.optinHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("title").get(0).text mustBe optinTitle
    }

    "have correct form action to submit the survey" in new SurveyControllerSetup {
      val page = controller.displayOptinDeclinedSurveyForm(TestFixtures.optinHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-survey").attr("action") must endWith(
        routes.SurveyController.submitOptinDeclinedSurveyForm(TestFixtures.optinHostContext("foo@bar.com")).url
      )
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a user submits a survey" in new SurveyControllerSetup {

      val page = controller.submitOptinDeclinedSurveyForm(TestFixtures.optinHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe" -> "true",
          "choice-717c2da0-4411-41ad-9a78-b335786e7107" -> "true",
          "choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b" -> "true",
          "choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861" -> "false",
          "choice-ca31965c-dd40-4a2c-a606-fe961da485c0" -> "true",
          "reason"                                      -> "test test test",
          "submissionType"                              -> "submitted"
        )
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendExtendedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.tags must contain("transactionName" -> "OptIn Declined Survey Answered")
      val detail = Json.fromJson[EventDetail](value.detail).get
      detail.utr mustBe validUtr.value
      detail.nino mustBe "N/A"
      detail.choices.get("choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe").get.answer mustBe "true"
      detail.choices.get("choice-717c2da0-4411-41ad-9a78-b335786e7107").get.answer mustBe "true"
      detail.choices.get("choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b").get.answer mustBe "true"
      detail.choices.get("choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861").get.answer mustBe "false"
      detail.choices.get("choice-ca31965c-dd40-4a2c-a606-fe961da485c0").get.answer mustBe "true"
      detail.reason mustBe "test test test"
      detail.submissionType mustBe "submitted"
    }

    "be created as EventTypes.Succeeded when a user skips a survey" in new SurveyControllerSetup {

      val page = controller.submitOptinDeclinedSurveyForm(TestFixtures.optinHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe" -> "true",
          "choice-717c2da0-4411-41ad-9a78-b335786e7107" -> "true",
          "choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b" -> "true",
          "choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861" -> "false",
          "choice-ca31965c-dd40-4a2c-a606-fe961da485c0" -> "true",
          "reason"                                      -> "test test test",
          "submissionType"                              -> "skipped"
        )
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendExtendedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.tags must contain("transactionName" -> "OptIn Declined Survey Answered")
      val detail = Json.fromJson[EventDetail](value.detail).get
      detail.utr mustBe validUtr.value
      detail.nino mustBe "N/A"
      detail.choices.get("choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe").get.answer mustBe "true"
      detail.choices.get("choice-717c2da0-4411-41ad-9a78-b335786e7107").get.answer mustBe "true"
      detail.choices.get("choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b").get.answer mustBe "true"
      detail.choices.get("choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861").get.answer mustBe "false"
      detail.choices.get("choice-ca31965c-dd40-4a2c-a606-fe961da485c0").get.answer mustBe "true"
      detail.reason mustBe "test test test"
      detail.submissionType mustBe "skipped"
    }

    "not be created when a user submits an invalid survey form with more than 3000 characters in the reason field" in new SurveyControllerSetup {

      val page = controller.submitOptinDeclinedSurveyForm(TestFixtures.optinHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe" -> "true",
          "choice-717c2da0-4411-41ad-9a78-b335786e7107" -> "true",
          "choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b" -> "true",
          "choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861" -> "false",
          "choice-ca31965c-dd40-4a2c-a606-fe961da485c0" -> "true",
          "reason"                                      -> "A" * 3001,
          "submissionType"                              -> "submitted"
        )
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("reason-error")
        .toString mustBe """<p id="reason-error" class="govuk-error-message"><span class="govuk-visually-hidden">Error:</span> Reason must be 3000 characters or fewer</p>"""

      verifyNoInteractions(mockAuditConnector)
    }

    "be created and reason field trimmed to 3000 characters when the survey is skipped with more than 3000 characters in the reason field" in new SurveyControllerSetup {

      val page = controller.submitOptinDeclinedSurveyForm(TestFixtures.optinHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe" -> "true",
          "choice-717c2da0-4411-41ad-9a78-b335786e7107" -> "true",
          "choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b" -> "true",
          "choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861" -> "false",
          "choice-ca31965c-dd40-4a2c-a606-fe961da485c0" -> "true",
          "reason"                                      -> "A" * 5000,
          "submissionType"                              -> "skipped"
        )
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendExtendedEvent(eventArg.capture())(any[HeaderCarrier], any[ExecutionContext])

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.tags must contain("transactionName" -> "OptIn Declined Survey Answered")
      val detail = Json.fromJson[EventDetail](value.detail).get
      detail.utr mustBe validUtr.value
      detail.nino mustBe "N/A"
      detail.choices.get("choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe").get.answer mustBe "true"
      detail.choices.get("choice-717c2da0-4411-41ad-9a78-b335786e7107").get.answer mustBe "true"
      detail.choices.get("choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b").get.answer mustBe "true"
      detail.choices.get("choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861").get.answer mustBe "false"
      detail.choices.get("choice-ca31965c-dd40-4a2c-a606-fe961da485c0").get.answer mustBe "true"
      detail.reason mustBe "A" * 3000 // Reason is trimmed to 3000 characters on strip
      detail.submissionType mustBe "skipped"
    }
  }
}
