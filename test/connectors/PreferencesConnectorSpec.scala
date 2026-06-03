/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package connectors

import connectors.StatusNameResponse.Alright
import model.Language.English
import model.{ HostContext, Language }
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ reset, times, verify, when }
import org.scalatest.BeforeAndAfterEach
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.http.Status.{ BAD_REQUEST, CONFLICT, CREATED, NOT_FOUND, NO_CONTENT, OK }
import play.api.libs.json.{ JsValue, Json }
import play.api.test.Helpers.*
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream4xxResponse
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, HttpResponse, UpstreamErrorResponse }
import utils.TestData.{ EMPTY_STRING, TEST_EMAIL_VALUE, TEST_LOCAL_DATE, TEST_TIME_INSTANT }

import java.net.{ URI, URL }
import java.time.{ Instant, LocalDate }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class PreferencesConnectorSpec extends PlaySpec with ScalaFutures with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  private lazy val config = Configuration(
    "microservice.services.preferences.host" -> "localhost",
    "microservice.services.preferences.port" -> "443"
  )

  "getPreferences" should {
    "respond normally when everything works" in {

      when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(using any(), any())).thenReturn(Future.successful(Some(preferenceResponse)))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response = preferencesConnector.getPreferences().futureValue
      response.isDefined mustBe true
      verify(mockHttpClient).get(ArgumentMatchers.eq(new URI("http://localhost:443/preferences").toURL))(any())
    }

    "respond with nothing when not found" in {
      when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(using any(), any())).thenReturn(Future.successful(None))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response = preferencesConnector.getPreferences().futureValue
      response.isDefined mustBe false
      verify(mockHttpClient).get(ArgumentMatchers.eq(new URI("http://localhost:443/preferences").toURL))(any())
    }
  }

  "optin" should {
    "work correctly under normal conditions" in {
      implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(OK, ""))))

      val tcu = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(accepted = true), email = None, language = Some(English))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response = preferencesConnector.optIn(tcu).futureValue
      response mustBe PreferencesExists
      verify(mockHttpClient).post(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/optin").toURL))(any())
    }

    "work correctly for ITSA under normal conditions" in {
      implicit val hostContext: HostContext =
        new HostContext(returnUrl = "", returnLinkText = "", regime = Some("itsa"))
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(OK, ""))))

      val tcu = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(accepted = true), email = None, language = Some(English))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response = preferencesConnector.optIn(tcu).futureValue
      response mustBe PreferencesExists
      verify(mockHttpClient)
        .post(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/regime/optin").toURL))(any())
    }
  }

  "optout" should {

    "work correctly under normal conditions" in {
      implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(OK, ""))))

      val tcu = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(accepted = true), email = None, language = Some(English))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response = preferencesConnector.optOut(tcu).futureValue
      response mustBe PreferencesExists
      verify(mockHttpClient).post(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/optout").toURL))(any())
    }

    "work correctly for ITSA under normal conditions" in {
      implicit val hostContext: HostContext =
        new HostContext(returnUrl = "", returnLinkText = "", regime = Some("itsa"))
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(OK, ""))))

      val tcu = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(accepted = true), email = None, language = Some(English))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response = preferencesConnector.optOut(tcu).futureValue
      response mustBe PreferencesExists
      verify(mockHttpClient)
        .post(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/regime/optout").toURL))(any())
    }
  }

  "updateEmailValidationStatusUnsecured" should {
    "work correctly under normal conditions when returnLinkText and returnUrl is returned" in {
      val jsonBody = Json.obj(
        "returnLinkText" -> "someText",
        "returnUrl"      -> "someUrl"
      )
      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(CREATED, jsonBody.toString()))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue
      response mustBe ValidatedWithReturn("someText", "someUrl")
      verify(mockHttpClient).put(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/email").toURL))(any())
    }

    "work correctly under normal conditions and return Validated if status is anything other than CREATED" in {
      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(CONFLICT, "no content"))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue
      response mustBe Validated
      verify(mockHttpClient).put(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/email").toURL))(any())
    }

    "return ValidationExpired when api call returns 410" in {
      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("error occurred", GONE))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue

      response mustBe ValidationExpired
      verify(mockHttpClient).put(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/email").toURL))(any())
    }

    "return ValidationExpired when api call returns 409" in {
      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("error occurred", CONFLICT))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue

      response mustBe WrongToken
      verify(mockHttpClient).put(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/email").toURL))(any())
    }

    "return ValidationExpired when api call returns other than 409, 410 and 412" in {
      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("error occurred", INTERNAL_SERVER_ERROR))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      intercept[Exception] {
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue
      }
    }
  }

  "updateEmailValidationStatusUnsecured v2" should {

    "work correctly under normal conditions when returnLinkText and returnUrl is returned" in {
      val jsonBody = Json.obj(
        "returnLinkText" -> "someText",
        "returnUrl"      -> "someUrl"
      )

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(CREATED, jsonBody.toString()))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue
      response mustBe ValidatedWithReturn("someText", "someUrl")
      verify(mockHttpClient).put(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/email").toURL))(any())
    }

    "work correctly second invocation under normal conditions when returnLinkText and returnUrl is returned" in {
      val jsonBodyOne = Json.obj(
        "returnLinkText" -> "someText",
        "returnUrl"      -> "someUrl"
      )

      val jsonBodyTwo = Json.obj(
        "verifyStatus"   -> "already_verified_links",
        "description"    -> "whatever",
        "returnLinkText" -> "someText",
        "returnUrl"      -> "someUrl"
      )

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(CREATED, jsonBodyOne.toString()))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response1 =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue
      response1 mustBe ValidatedWithReturn("someText", "someUrl")

      // The second invocation should return OK, with a different response body.
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(OK, jsonBodyTwo.toString()))))

      val response2 =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue
      response2 mustBe ValidationErrorWithReturn("someText", "someUrl")
      verify(mockHttpClient, times(2)).put(
        ArgumentMatchers.eq(new URI("http://localhost:443/preferences/email").toURL)
      )(any())
    }

    "work correctly second invocation under normal conditions without links" in {
      val jsonBodyOne = Json.obj()

      val jsonBodyTwo = Json.obj(
        "verifyStatus" -> "already_verified",
        "description"  -> "whatever"
      )

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(NO_CONTENT, jsonBodyOne.toString()))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response1 =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue
      response1 mustBe Validated

      // The second invocation should return OK, with a different response body.
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(OK, jsonBodyTwo.toString()))))

      val response2 =
        preferencesConnector.updateEmailValidationStatusUnsecured("30e37a43-f316-4f1e-a410-2f466290d087").futureValue
      response2 mustBe ValidationError

      verify(mockHttpClient, times(2)).put(
        ArgumentMatchers.eq(new URI("http://localhost:443/preferences/email").toURL)
      )(any())
    }
  }

  "getEmailAddress" should {
    "work correctly under normal conditions" in {
      when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(using any(), any())).thenReturn(Future.successful(Some(Email("test@email.com"))))

      val preferencesConnector = new PreferencesConnector(config, mockHttpClient)
      val response = preferencesConnector.getEmailAddress(SaUtr("1234567890")).futureValue
      response mustBe Some("test@email.com")
    }
  }

  "change email address - pending email" should {
    "work correctly" in {
      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(OK, ""))))

      val c = new PreferencesConnector(config, mockHttpClient)
      c.changeEmailAddress("change@email.com").futureValue
      verify(mockHttpClient)
        .put(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/pending-email").toURL))(any())
    }

    "fail when the downstream errors" in {
      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("oops", BAD_REQUEST))))

      val c = new PreferencesConnector(config, mockHttpClient)

      val ex = recoverToExceptionIf[UpstreamErrorResponse] {
        c.changeEmailAddress("change@email.com")
      }

      whenReady(ex)(_.getMessage must include("oops"))
      verify(mockHttpClient)
        .put(ArgumentMatchers.eq(new URI("http://localhost:443/preferences/pending-email").toURL))(any())

    }
  }

  "getPreferencesStatus" should {
    "find the Preference with correct details" in {
      val emailPreference: EmailPreference = EmailPreference(
        email = TEST_EMAIL_VALUE,
        isVerified = true,
        hasBounces = false,
        mailboxFull = false,
        linkSent = Some(TEST_LOCAL_DATE),
        language = Some(English),
        pendingEmail = Some(TEST_EMAIL_VALUE)
      )

      val preferenceFound: PreferenceFound = PreferenceFound(
        accepted = true,
        email = Some(emailPreference),
        updatedAt = Some(TEST_TIME_INSTANT),
        majorVersion = Some(2),
        paperless = Some(true)
      )

      val termsAndCondition =
        Map("generic" -> TermsAndConditionsAcceptance(true, Some(TEST_TIME_INSTANT), Some(2), Some(true)))

      val prefResponse = PreferenceResponse(
        termsAndConditions = termsAndCondition,
        email = Some(emailPreference),
        surveys = None,
        entityId = None,
        status = Some(PaperlessStatusResponse(Alright))
      )

      when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(using any(), any())).thenReturn(Future.successful(Some(prefResponse)))

      val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

      await(connector.getPreferencesStatus()) mustBe Right(preferenceFound)
    }

    "not find the Preference" in {
      when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(using any(), any())).thenReturn(Future.successful(None))

      val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

      await(connector.getPreferencesStatus()) mustBe Right(PreferenceNotFound(None))
    }

    "return correct status code" when {
      "Upstream4xxResponse occurs with NOT_FOUND" in {
        when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute(using any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("error occurred", NOT_FOUND)))

        val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

        await(connector.getPreferencesStatus()) mustBe Left(NOT_FOUND)
      }

      "Upstream4xxResponse occurs with UNAUTHORIZED" in {
        when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute(using any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("error occurred", UNAUTHORIZED)))

        val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

        await(connector.getPreferencesStatus()) mustBe Left(UNAUTHORIZED)
      }

      "BadRequestException occurs" in {
        when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute(using any(), any()))
          .thenReturn(Future.failed(BadRequestException("error occurred")))

        val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

        await(connector.getPreferencesStatus()) mustBe Left(BAD_REQUEST)
      }
    }
  }

  "getPreferencesUnresolved" should {
    "find the Preference with correct details" in {
      val emailPreference: EmailPreference = EmailPreference(
        email = TEST_EMAIL_VALUE,
        isVerified = true,
        hasBounces = false,
        mailboxFull = false,
        linkSent = Some(TEST_LOCAL_DATE),
        language = Some(English),
        pendingEmail = Some(TEST_EMAIL_VALUE)
      )

      val termsAndCondition =
        Map("generic" -> TermsAndConditionsAcceptance(true, Some(TEST_TIME_INSTANT), Some(2), Some(true)))

      val prefResponse = PreferenceResponse(
        termsAndConditions = termsAndCondition,
        email = Some(emailPreference),
        surveys = None,
        entityId = None,
        status = Some(PaperlessStatusResponse(Alright))
      )

      when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(using any(), any())).thenReturn(Future.successful(Some(prefResponse)))

      val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

      await(connector.getPreferencesUnresolved()) mustBe Right(prefResponse)
    }

    "not find the Preference" in {
      when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(using any(), any())).thenReturn(Future.successful(None))

      val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

      await(connector.getPreferencesUnresolved()) mustBe Left(PreferenceNotFound(None))
    }

    "return correct status code" when {
      "Upstream4xxResponse occurs with NOT_FOUND" in {
        when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute(using any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("error occurred", NOT_FOUND)))

        val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

        await(connector.getPreferencesUnresolved()) mustBe Left(PreferenceNotFound(None))
      }

      "Upstream4xxResponse occurs with UNAUTHORIZED" in {
        when(mockHttpClient.get(any)(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute(using any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("error occurred", CONFLICT)))

        val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

        await(connector.getPreferencesUnresolved()) mustBe Left(MultiplePreferenceFound())
      }
    }
  }

  "changeEmailLanguage" should {
    implicit val hostContext: HostContext = new HostContext(returnUrl = EMPTY_STRING, returnLinkText = EMPTY_STRING)
    val termsAndConditionsUpdate: TermsAndConditionsUpdate = TermsAndConditionsUpdate(
      generic = None,
      email = Some(TEST_EMAIL_VALUE),
      language = Some(English)
    )

    "return PreferencesExists when email-language api call returns response with status code 200" in {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(OK))))

      val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

      await(connector.changeEmailLanguage(termsAndConditionsUpdate)) mustBe PreferencesExists
    }

    "return PreferencesCreated when email-language api call returns response with status code 201" in {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(CREATED))))

      val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

      await(connector.changeEmailLanguage(termsAndConditionsUpdate)) mustBe PreferencesCreated
    }

    "throw exception when email-language api call returns response with status code other than 200 and 201" in {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Right(HttpResponse(INTERNAL_SERVER_ERROR))))

      val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

      intercept[Exception] {
        await(connector.changeEmailLanguage(termsAndConditionsUpdate))
      }
    }

    "throw exception when email-language api call throws exception" in {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsValue])(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using any, any))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("error occurred", INTERNAL_SERVER_ERROR))))

      val connector: PreferencesConnector = new PreferencesConnector(config, mockHttpClient)

      intercept[Exception] {
        await(connector.changeEmailLanguage(termsAndConditionsUpdate))
      }
    }
  }

  override def beforeEach(): Unit =
    reset(mockHttpClient)

  def emailPref(
    email: String = "test@test",
    isVerified: Boolean = true,
    hasBounces: Boolean = false,
    mailboxFull: Boolean = false,
    linkSent: Option[LocalDate] = Some(LocalDate.now()),
    language: Option[Language] = Some(English),
    pendingEmail: Option[String] = None
  ): Option[EmailPreference] =
    Some(EmailPreference(email, isVerified, hasBounces, mailboxFull, linkSent, language, pendingEmail))

  def preferenceResponse: PreferenceResponse =
    PreferenceResponse(
      termsAndConditions = getTnc(),
      email = emailPref(),
      surveys = None,
      entityId = None,
      status = Some(PaperlessStatusResponse(Alright))
    )

  def getTnc(
    accepted: Boolean = true,
    updatedAt: Option[Instant] = None,
    majorVersion: Option[Int] = Some(1),
    paperless: Option[Boolean] = Some(true)
  ): Map[String, TermsAndConditionsAcceptance] =
    Map("generic" -> TermsAndConditionsAcceptance(accepted, updatedAt, majorVersion, paperless))
}
