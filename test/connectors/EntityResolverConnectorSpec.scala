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

package connectors

import controllers.internal.{ IPage7, OptInCohort }
import model.HostContext
import model.Language.Welsh
import model.PageType.IPage
import model.SurveyType.StandardInterruptOptOut
import org.mockito.ArgumentMatchers.{ eq as eqTo, * }
import org.mockito.Mockito.*
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.*
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{ Application, Configuration }
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{ JsBoolean, JsResultException, JsString, Json, JsonValidationError, Writes }
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, HttpReads, HttpResponse, InternalServerException, NotFoundException, UpstreamErrorResponse }
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.Instant
import java.net.URL
import scala.concurrent.{ ExecutionContext, Future }
import model.{ Survey, SurveyType }
import utils.TestData.{ EMPTY_STRING, TEST_EMAIL_VALUE, TEST_MSG }

class EntityResolverConnectorSpec
    extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar
    with Eventually with SpanSugar with play.api.http.Status {

  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.entity-resolver.circuitBreaker.numberOfCallsToTriggerStateChange"  -> "5",
        "microservice.services.entity-resolver.circuitBreaker.unavailablePeriodDurationInSeconds" -> "1",
        "metrics.enabled"                                                                         -> false
      )
      .overrides(bind[HttpClientV2].toInstance(mockHttpClient))
      .build()

  // feature flag features.bypass.entityresolver defaults to false, so test will exercise entity-resolver connector
  val preferencesConnector: PreferencesConnector = app.injector.instanceOf[PreferencesConnector]
  lazy val configuration: Configuration = app.injector.instanceOf[Configuration]

  val connector = new EntityResolverConnector(configuration, mockHttpClient)

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")

  override protected def beforeEach(): Unit = {
    reset(mockHttpClient, requestBuilder)
    when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
    when(requestBuilder.execute[Option[PreferenceResponse]](using any, any)).thenReturn(Future.successful(None))
  }

  "The responseToEmailVerificationLinkStatus method" should {

    "return ok if updateEmailValidationStatusUnsecured returns 200" in {
      val result = connector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(OK, "")))
      result.futureValue mustBe Validated
    }

    "return ok if updateEmailValidationStatusUnsecured returns 204" in {
      val result = connector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(NO_CONTENT, "")))
      result.futureValue mustBe Validated
    }

    "return ok with the return link text and return url if updateEmailValidationStatusUnsecured returns 201" in {
      val responseJson = Json.parse("""{
                                      |     "returnLinkText": "Return Link Text",
                                      |     "returnUrl": "ReturnUrl"
                                      |}""".stripMargin)
      val result = connector.responseToEmailVerificationLinkStatus(
        Future.successful(HttpResponse(CREATED, responseJson, Map()))
      )
      result.futureValue mustBe ValidatedWithReturn("Return Link Text", "ReturnUrl")
    }

    "return error if updateEmailValidationStatusUnsecured returns 400" in {
      val result = connector.responseToEmailVerificationLinkStatus(Future.failed(new BadRequestException("")))
      result.futureValue mustBe ValidationError
    }

    "return 'error with return' with error if updateEmailValidationStatusUnsecured returns 412" in {
      val result = connector.responseToEmailVerificationLinkStatus(
        Future.failed(
          UpstreamErrorResponse(
            """PUT of something Response body: '{"returnLinkText":"a message", "returnUrl": "https://some/place"}'""",
            PRECONDITION_FAILED,
            0,
            Map()
          )
        )
      )
      result.futureValue mustBe ValidationErrorWithReturn("a message", "https://some/place")
    }

    "return error if updateEmailValidationStatusUnsecured returns 404" in {
      val result = connector.responseToEmailVerificationLinkStatus(Future.failed(new NotFoundException("")))
      result.futureValue mustBe ValidationError
    }

    "pass through the failure if updateEmailValidationStatusUnsecured returns 500" in {
      val expectedErrorResponse = UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)
      val result = connector.responseToEmailVerificationLinkStatus(Future.failed(expectedErrorResponse))

      result.failed.futureValue mustBe expectedErrorResponse
    }

    "return expired if updateEmailValidationStatusUnsecured returns 410" in {
      val result =
        connector.responseToEmailVerificationLinkStatus(
          Future.failed(UpstreamErrorResponse("", GONE, INTERNAL_SERVER_ERROR))
        )
      result.futureValue mustBe ValidationExpired
    }

    "return wrong token if updateEmailValidationStatusUnsecured returns 409" in {
      val result =
        connector.responseToEmailVerificationLinkStatus(
          Future.failed(UpstreamErrorResponse("", CONFLICT, INTERNAL_SERVER_ERROR))
        )
      result.futureValue mustBe WrongToken
    }
  }

  "The updateTermsAndConditions method" should {
    trait PayloadCheck {
      def postReturns(status: Int): OngoingStubbing[Future[Either[UpstreamErrorResponse, HttpResponse]]] = {
        when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
          .thenReturn(Future.successful(Right(HttpResponse(status, ""))))
      }

      def checkPayload(
        url: String,
        payload: TermsAndConditionsUpdate
      )(implicit ec: ExecutionContext): RequestBuilder = {
        when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)

        val bodyToVerify = Json.toJson(payload)
        verify(requestBuilder).withBody(eqTo(bodyToVerify))(using any(), any(), any())
      }
    }

    "send generic accepted true and return preferences created if terms and conditions are accepted and updated and preferences created" in new PayloadCheck {
      postReturns(OK)
      val payload =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(true), email = None, language = Some(Welsh))
      connector.optIn(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/optin", payload)
    }

    "send generic accepted false and return preferences created if terms and conditions are not accepted and updated and preferences created" in new PayloadCheck {
      postReturns(OK)
      val payload =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(false), email = None, language = Some(Welsh))
      connector.optIn(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/optin", payload)
    }

    "return failure if any problems" in new PayloadCheck {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("", UNAUTHORIZED, UNAUTHORIZED)))

      val payload = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(true), email = None, language = Some(Welsh))

      private val error = connector.optIn(payload).failed.futureValue
      error mustBe an[UpstreamErrorResponse]
      error.asInstanceOf[UpstreamErrorResponse].statusCode mustBe UNAUTHORIZED
    }
  }

  "New user" should {
    trait NewUserPayloadCheck {

      def postReturns(status: Int): OngoingStubbing[Future[Either[UpstreamErrorResponse, HttpResponse]]] = {
        when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
          .thenReturn(Future.successful(Right(HttpResponse(status, ""))))
      }

      def checkPayload(
        url: String,
        payload: TermsAndConditionsUpdate
      ): RequestBuilder = {

        when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)

        val bodyToVerify = Json.toJson(payload)
        verify(requestBuilder).withBody(eqTo(bodyToVerify))(using any(), any(), any())
      }
    }

    "send generic accepted true with email" in new NewUserPayloadCheck {
      postReturns(CREATED)
      val termsAndConditionsUpdate =
        TermsAndConditionsUpdate.from(
          GenericTerms -> TermsAccepted(true),
          email = Some("test@test.com"),
          language = Some(Welsh)
        )

      connector.optIn(termsAndConditionsUpdate).futureValue must be(PreferencesCreated)

      checkPayload("/preferences/optin", termsAndConditionsUpdate)
    }

    "send generic accepted false with no email" in new NewUserPayloadCheck {
      postReturns(CREATED)
      val expectedPayload =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(true), email = None, language = Some(Welsh))

      connector.optIn(expectedPayload).futureValue must be(PreferencesCreated)
    }

    "try and send accepted true with email where preferences not working" in new NewUserPayloadCheck {
      val expectedPayload =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(true), email = None, language = Some(Welsh))

      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("", UNAUTHORIZED, UNAUTHORIZED)))

      private val error = connector.optIn(expectedPayload).failed.futureValue
      error mustBe an[UpstreamErrorResponse]
      error.asInstanceOf[UpstreamErrorResponse].statusCode mustBe UNAUTHORIZED
    }
  }

  "getPreferencesStatusFinal" should {
    "return PreferenceFound when preference exists with terms accepted" in {
      val email = EmailPreference("test@example.com", true, false, false, None)
      val termsAcceptance = TermsAndConditionsAcceptance(
        accepted = true,
        updatedAt = Some(Instant.parse("2023-01-01T00:00:00Z")),
        majorVersion = Some(1),
        paperless = Some(true)
      )
      val preferenceResponse = PreferenceResponse(
        termsAndConditions = Map("generic" -> termsAcceptance),
        email = Some(email)
      )

      when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute[Option[PreferenceResponse]](using any, any))
        .thenReturn(Future.successful(Some(preferenceResponse)))

      val result = connector.getPreferencesStatusFinal("generic", "/preferences/sa/individual")
      result.futureValue mustBe Right(
        PreferenceFound(
          accepted = true,
          email = Some(email),
          updatedAt = Some(Instant.parse("2023-01-01T00:00:00Z")),
          majorVersion = Some(1),
          paperless = Some(true),
          surveys = None
        )
      )
    }

    "return PreferenceFound when preference exists with surveys" in {
      val email = EmailPreference("test@example.com", true, false, false, None)
      val termsAcceptance = TermsAndConditionsAcceptance(accepted = true, paperless = Some(true))
      val surveys = List(Survey(SurveyType.StandardInterruptOptOut, Instant.parse("2023-01-01T00:00:00Z")))
      val preferenceResponse = PreferenceResponse(
        termsAndConditions = Map("generic" -> termsAcceptance),
        email = Some(email),
        surveys = Some(surveys)
      )

      when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute[Option[PreferenceResponse]](using any, any))
        .thenReturn(Future.successful(Some(preferenceResponse)))

      val result = connector.getPreferencesStatusFinal("generic", "/preferences/sa/individual")
      result.futureValue mustBe Right(
        PreferenceFound(
          accepted = true,
          email = Some(email),
          updatedAt = None,
          majorVersion = None,
          paperless = Some(true),
          surveys = Some(surveys)
        )
      )
    }

    "return PreferenceNotFound when preference exists but termsAndConditions key not found" in {
      val email = EmailPreference("test@example.com", true, false, false, None)
      val preferenceResponse = PreferenceResponse(
        termsAndConditions = Map("some-other-key" -> TermsAndConditionsAcceptance(true)),
        email = Some(email)
      )

      when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute[Option[PreferenceResponse]](using any, any))
        .thenReturn(Future.successful(Some(preferenceResponse)))

      val result = connector.getPreferencesStatusFinal("generic", "/preferences/sa/individual")
      result.futureValue mustBe Right(PreferenceNotFound(Some(email)))
    }

    "return PreferenceNotFound when no preference response" in {
      when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute[Option[PreferenceResponse]](using any, any))
        .thenReturn(Future.successful(None))

      val result = connector.getPreferencesStatusFinal("generic", "/preferences/sa/individual")
      result.futureValue mustBe Right(PreferenceNotFound(None))
    }

    "return Left(NOT_FOUND) when service returns 404" in {
      when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute[Option[PreferenceResponse]](using any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("Not Found", NOT_FOUND, NOT_FOUND)))

      val result = connector.getPreferencesStatusFinal("generic", "/preferences/sa/individual")
      result.futureValue mustBe Left(NOT_FOUND)
    }

    "return Left(UNAUTHORIZED) when service returns 401" in {
      when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute[Option[PreferenceResponse]](using any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED, UNAUTHORIZED)))

      val result = connector.getPreferencesStatusFinal("generic", "/preferences/sa/individual")
      result.futureValue mustBe Left(UNAUTHORIZED)
    }

    "return Left(BAD_REQUEST) when service returns BadRequestException" in {
      when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute[Option[PreferenceResponse]](using any, any))
        .thenReturn(Future.failed(new BadRequestException("Bad Request")))

      val result = connector.getPreferencesStatusFinal("generic", "/preferences/sa/individual")
      result.futureValue mustBe Left(BAD_REQUEST)
    }
  }

  "The changeEmailAddress method" should {

    "successfully update email address" in {
      val newEmail = "newemail@example.com"
      val httpResponse = HttpResponse(OK, "")

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(httpResponse)))

      val result = connector.changeEmailAddress(newEmail)
      result.futureValue mustBe httpResponse

      verify(requestBuilder).withBody(eqTo(Json.toJson(UpdateEmail(newEmail, None))))(using any(), any(), any())
    }

    "successfully update email address with journey parameter" in {
      val newEmail = "newemail@example.com"
      val journey = Some("itsa")
      val httpResponse = HttpResponse(OK, "")

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(httpResponse)))

      val result = connector.changeEmailAddress(newEmail, journey)
      result.futureValue mustBe httpResponse

      verify(requestBuilder).withBody(eqTo(Json.toJson(UpdateEmail(newEmail, journey))))(using any(), any(), any())
    }

    "throw UpstreamErrorResponse when service returns error" in {
      val newEmail = "newemail@example.com"
      val upstreamError = UpstreamErrorResponse("Service unavailable", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Left(upstreamError)))

      val result = connector.changeEmailAddress(newEmail)
      result.failed.futureValue mustBe upstreamError
    }

    "throw error when service fails" in {
      val newEmail = "newemail@example.com"
      val exception = new RuntimeException("Connection failed")

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.failed(exception))

      val result = connector.changeEmailAddress(newEmail)
      result.failed.futureValue mustBe exception
    }
  }

  "The optOut method" should {
    trait OptOutPayloadCheck {
      def postReturns(status: Int): OngoingStubbing[Future[Either[UpstreamErrorResponse, HttpResponse]]] = {
        when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
          .thenReturn(Future.successful(Right(HttpResponse(status, ""))))
      }

      def checkPayload(
        url: String,
        payload: TermsAndConditionsUpdate
      ): RequestBuilder = {
        when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)

        val bodyToVerify = Json.toJson(payload)
        verify(requestBuilder).withBody(eqTo(bodyToVerify))(using any(), any(), any())
      }
    }

    "send generic accepted false and return PreferencesExists when opting out existing preferences" in new OptOutPayloadCheck {
      postReturns(OK)
      val payload: TermsAndConditionsUpdate =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(false), email = None, language = Some(Welsh))
      connector.optOut(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/optout", payload)
    }

    "send generic accepted false and return PreferencesCreated when creating opt-out preference" in new OptOutPayloadCheck {
      postReturns(CREATED)
      val payload: TermsAndConditionsUpdate =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(false), email = Some("test@test.com"), language = Some(Welsh))
      connector.optOut(payload).futureValue must be(PreferencesCreated)
      checkPayload("/preferences/optout", payload)
    }

    "use ITSA endpoint when hostContext.isItsa is true" in new OptOutPayloadCheck {
      implicit val itsaHostContext: HostContext = HostContext(
        returnUrl = "",
        returnLinkText = "",
        regime = Some("itsa")
      )
      postReturns(OK)
      val payload: TermsAndConditionsUpdate =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(false), email = None, language = Some(Welsh))

      connector.optOut(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/regime/optout", payload)
    }

    "use non-ITSA endpoint when hostContext.isItsa is false" in new OptOutPayloadCheck {
      postReturns(OK)
      val payload: TermsAndConditionsUpdate =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(false), email = None, language = Some(Welsh))

      connector.optOut(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/optout", payload)
    }

    "throw exception when service returns unexpected status code" in new OptOutPayloadCheck {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(NO_CONTENT, ""))))

      val payload: TermsAndConditionsUpdate = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(false), email = None, language = Some(Welsh))

      val error: Throwable = connector.optOut(payload).failed.futureValue
      error mustBe an[Exception]
      error.getMessage must include("Unhandled status in optOut(...): 204")
    }

    "throw UpstreamErrorResponse when service returns error" in new OptOutPayloadCheck {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(using any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("Service error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val payload: TermsAndConditionsUpdate = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(false), email = None, language = Some(Welsh))

      val error: Throwable = connector.optOut(payload).failed.futureValue
      error mustBe an[UpstreamErrorResponse]
      error.asInstanceOf[UpstreamErrorResponse].statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "OptInPage.optInPageFormat" should {
    import OptInPage.optInPageFormat

    "read the json correctly" in new Setup {
      Json.parse(optInPageJsonString).as[OptInPage] mustBe optInPage
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(optInPageInvalidJsonString1).as[OptInPage]
      }

      intercept[JsResultException] {
        Json.parse(optInPageInvalidCohortNumberJsonString).as[OptInPage]
      }

      intercept[JsResultException] {
        Json.parse(optInPageMissingCohortJsonString).as[OptInPage]
      }

      intercept[IllegalArgumentException] {
        Json.parse(optInPageInvalidMajorVersionJsonString).as[OptInPage]
      }.getMessage must be("Invalid constructor arguments")
    }

    "write the object correctly" in new Setup {
      Json.toJson(optInPage) mustBe Json.parse(optInPageJsonString)
    }
  }

  "TermsAccepted.format" should {
    import TermsAccepted.format

    "read the json correctly" in new Setup {
      Json.parse(termsAcceptedJsonString).as[TermsAccepted] mustBe termsAccepted
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(termsAcceptedInvalidJsonString).as[TermsAccepted]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(termsAccepted) mustBe Json.parse(termsAcceptedJsonString)
    }
  }

  "ActivationStatus.format" should {
    val entityResolverConnector: EntityResolverConnector = app.injector.instanceOf[EntityResolverConnector]

    import entityResolverConnector.ActivationStatus.format
    import entityResolverConnector.ActivationStatus

    "read the json correctly" in {
      Json.parse("""{"active":true}""".stripMargin).as[ActivationStatus] mustBe ActivationStatus(true)
      Json.parse("""{"active":false}""".stripMargin).as[ActivationStatus] mustBe ActivationStatus(false)
    }

    "throw the exception for invalid json" in {
      intercept[JsResultException] {
        JsString(TEST_MSG).as[ActivationStatus]
      }

      intercept[JsResultException] {
        JsString(EMPTY_STRING).as[ActivationStatus]
      }
    }

    "write the object correctly" in {
      Json.toJson(ActivationStatus(true)) mustBe Json.parse("""{"active":true}""".stripMargin)
    }
  }

  "Email.readsEmail" should {
    import Email.readsEmail

    "read the json correctly" in new Setup {
      Json.parse("""{"email":"test@test.com"}""").as[Email] mustBe email
    }

    "throw the exception for invalid json" in {
      intercept[JsResultException] {
        Json.parse("""{}""").as[Email]
      }
    }
  }

  "Version.versionFormat" should {
    import Version.versionFormat

    "read the json correctly" in new Setup {
      Json.parse(versionJsonString).as[Version] mustBe version
    }

    "throw the exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(versionInvalidJsonString).as[Version]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(version) mustBe Json.parse(versionJsonString)
    }
  }

  trait Setup {
    val version: Version = Version(2, 1)
    val optInPage: OptInPage = OptInPage.from(IPage7)
    val email: Email = Email(TEST_EMAIL_VALUE)
    val termsAccepted: TermsAccepted =
      TermsAccepted(accepted = true, optInPage = Some(optInPage), surveyType = Some(StandardInterruptOptOut))

    val versionJsonString: String = """{"major":2,"minor":1}""".stripMargin
    val versionInvalidJsonString: String = """{"major":2}""".stripMargin

    val optInPageJsonString: String = """{"version":{"major":0,"minor":0},"cohort":7,"pageType":"IPage"}""".stripMargin
    val optInPageInvalidJsonString1: String = """{"cohort":7,"pageType":"IPage"}""".stripMargin
    val optInPageInvalidCohortNumberJsonString: String =
      """{"version":{"major":0,"minor":0},"cohort":90,"pageType":"IPage"}""".stripMargin

    val optInPageMissingCohortJsonString: String =
      """{"version":{"major":0,"minor":0},"pageType":"IPage"}""".stripMargin

    val optInPageInvalidMajorVersionJsonString: String =
      """{"version":{"major":5,"minor":0},"cohort":7,"pageType":"IPage"}""".stripMargin

    val termsAcceptedJsonString: String =
      """{
        |"accepted":true,
        |"optInPage":{"version":{"major":0,"minor":0},"cohort":7,"pageType":"IPage"},
        |"surveyType":"StandardInterruptOptOut"
        |}""".stripMargin

    val termsAcceptedInvalidJsonString: String =
      """{
        |"optInPage":{"version":{"major":0,"minor":0},"cohort":7,"pageType":"IPage"},
        |"surveyType":"StandardInterruptOptOut"
        |}""".stripMargin
  }
}
