/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import _root_.connectors.*
import controllers.AuthRetrievalsSetup
import helpers.TestFixtures
import model.{ HostContext, Language, Survey, SurveyType }
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{ eq as is, * }
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, Retrieval, ~ }
import uk.gov.hmrc.http.HeaderCarrier

import java.time.temporal.ChronoUnit
import java.time.{ Instant, ZoneOffset, ZonedDateTime }
import scala.concurrent.{ ExecutionContext, Future }

class ActivationControllerSpec
    extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar with ScalaFutures
    with AuthRetrievalsSetup {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val gracePeriod: Long = 10
  private val request = FakeRequest()
  private val mockPreferencesConnector = mock[PreferencesConnector]
  private val mockAuthConnector = mock[AuthConnector]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "preferences-frontend.host"        -> "",
        "Test.activation.gracePeriodInMin" -> gracePeriod
      )
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector)
      )
      .build()
  private val controller = app.injector.instanceOf[ActivationController]

  when(
    mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )
  ).thenReturn(retrievalResult)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPreferencesConnector)
  }

  "The Activation with an AuthContext" should {
    "store the current user's language stored in the journey cookie when there is no language setting in preferences and" should {
      "return a json body with optedIn set to true if preference is found and opted-in and no alreadyOptedInUrl is present" in {
        val email = EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)

        when(
          mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
        )
          .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
        when(
          mockPreferencesConnector
            .changeEmailLanguage(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.English))))(
              any[HeaderCarrier],
              is(TestFixtures.sampleHostContext),
              any[ExecutionContext]
            )
        ).thenReturn(Future.successful(PreferencesCreated))
        val cookies = Cookie("PLAY_LANG", "en")
        val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request.withCookies(cookies))
        status(res) mustBe Ok.header.status
        val document = Jsoup.parse(contentAsString(res))
        document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")
        verify(mockPreferencesConnector, times(1))
          .changeEmailLanguage(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.English))))(
            any[HeaderCarrier],
            is(TestFixtures.sampleHostContext),
            any[ExecutionContext]
          )
      }
      "redirect to the alreadyOptedInUrl if preference is found and opted-in and an alreadyOptedInUrl is present" in {
        val email = EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
        when(
          mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
        )
          .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
        when(
          mockPreferencesConnector
            .changeEmailLanguage(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.Welsh))))(
              any[HeaderCarrier],
              is(TestFixtures.alreadyOptedInUrlHostContext),
              any[ExecutionContext]
            )
        ).thenReturn(Future.successful(PreferencesCreated))
        val cookies = Cookie("PLAY_LANG", "cy")
        val res: Future[Result] =
          controller.activate(TestFixtures.alreadyOptedInUrlHostContext)(request.withCookies(cookies))

        status(res) mustBe SEE_OTHER
        res.map { result =>
          result.header.headers must contain("Location")
          result.header.headers.get("Location") mustBe TestFixtures.alreadyOptedInUrlHostContext.alreadyOptedInUrl
        }
        verify(mockPreferencesConnector, times(1))
          .changeEmailLanguage(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.Welsh))))(
            any[HeaderCarrier],
            is(TestFixtures.alreadyOptedInUrlHostContext),
            any[ExecutionContext]
          )
      }

      "not attempt to store in preferences the user's language held in the user's cookie when there is an existing language setting in preferences and" should {
        "return a json body with optedIn set to true if preference is found, opted-in and no alreadyOptedInUrl is present" in {
          val email = EmailPreference(
            "test@test.com",
            isVerified = false,
            hasBounces = false,
            mailboxFull = false,
            None,
            Some(Language.Welsh)
          )
          when(
            mockPreferencesConnector
              .getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
          )
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
          val cookies = Cookie("PLAY_LANG", "cy")
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request.withCookies(cookies))
          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")

          verify(mockPreferencesConnector, never()).changeEmailLanguage(any)(any, any, any)
        }

        "redirect to the alreadyOptedInUrl if preference is found, opted-in and an alreadyOptedInUrl is present" in {
          val email = EmailPreference(
            "test@test.com",
            isVerified = false,
            hasBounces = false,
            mailboxFull = false,
            None,
            Some(Language.English)
          )
          when(
            mockPreferencesConnector
              .getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
          )
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
          val cookies = Cookie("PLAY_LANG", "cy")
          val res: Future[Result] =
            controller.activate(TestFixtures.alreadyOptedInUrlHostContext)(request.withCookies(cookies))

          status(res) mustBe SEE_OTHER
          res.map { result =>
            result.header.headers must contain("Location")
            result.header.headers.get("Location") mustBe TestFixtures.alreadyOptedInUrlHostContext.alreadyOptedInUrl
          }
          verify(mockPreferencesConnector, never())
            .changeEmailLanguage(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        }
      }

      "not attempt to store in preferences the user's language held in the user's cookie when user is not opted-in and" should {
        "return a json body with optedIn set to false if preference is found, opted-out and no alreadyOptedInUrl is present" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
          when(
            mockPreferencesConnector
              .getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
          )
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = false, Some(email), paperless = None))))
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

          status(res) mustBe Ok.header.status
          verify(mockPreferencesConnector, never()).changeEmailLanguage(any)(any, any, any)
        }

        "return OK if customer is opted-out but with existing email" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)

          when(
            mockPreferencesConnector
              .getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
          )
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = false, Some(email), paperless = None))))
          val res: Future[Result] = controller.activate(TestFixtures.alreadyOptedInUrlHostContext)(request)

          status(res) mustBe OK
          verify(mockPreferencesConnector, never())
            .changeEmailLanguage(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        }
      }

      "not attempt to store in preferences the user's language held in the user's cookie when user has not accepted T&C and" should {
        "return a json body with optedIn set to false if T&C accepted is false and updatedAt is within the grace period" in {
          val lastUpdated = Instant.now().minus(gracePeriod - gracePeriod / 2, ChronoUnit.MINUTES)
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)

          when(
            mockPreferencesConnector
              .getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
          )
            .thenReturn(
              Future.successful(
                Right(PreferenceFound(accepted = false, Some(email), Some(lastUpdated), paperless = None))
              )
            )
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must include("""{"optedIn":false}""")
          verify(mockPreferencesConnector, never())
            .changeEmailLanguage(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        }

        "return a json body with optedIn set to false if T&C accepted is false and updatedAt is outside of the grace period" in {
          val lastUpdated = Instant.now().minus(gracePeriod + gracePeriod / 2, ChronoUnit.MINUTES)
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)

          when(
            mockPreferencesConnector
              .getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
          )
            .thenReturn(
              Future.successful(
                Right(PreferenceFound(accepted = false, Some(email), Some(lastUpdated), paperless = None))
              )
            )
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must include("""{"optedIn":false}""")
          verify(mockPreferencesConnector, never())
            .changeEmailLanguage(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        }
      }

      "not attempt to store in preferences the user's language held in the user's cookie when no preferences found and" should {
        "return PRECONDITION failed if no preferences are found and no alreadyOptedInUrl is present" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)

          when(
            mockPreferencesConnector
              .getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
          )
            .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

          status(res) mustBe PRECONDITION_FAILED
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must startWith(
            """{"redirectUserTo":"/paperless/choose?email="""
          )
          verify(mockPreferencesConnector, never())
            .changeEmailLanguage(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        }

        "return PRECONDITION failed if no preferences are found and an alreadyOptedInUrl is present" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)

          when(
            mockPreferencesConnector
              .getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
          )
            .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
          val res: Future[Result] = controller.activate(TestFixtures.alreadyOptedInUrlHostContext)(request)

          status(res) mustBe PRECONDITION_FAILED
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must startWith(
            """{"redirectUserTo":"/paperless/choose?email="""
          )
          verify(mockPreferencesConnector, never())
            .changeEmailLanguage(any[TermsAndConditionsUpdate])(
              any[HeaderCarrier],
              any[HostContext],
              any[ExecutionContext]
            )
        }
      }
    }

    "returns redirectUserTo with no survey for new user" in {
      when(
        mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(Future.successful(Right(PreferenceNotFound(None))))
      val res = controller.activate(TestFixtures.sampleHostContext)(request)

      status(res) mustBe PRECONDITION_FAILED
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must be(
        """{"redirectUserTo":"/paperless/choose?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&amp;returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D&amp;survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"}"""
      )
    }

    "returns redirectUserTo with survey request for an opted out user that hasn't seen the survey yet" in {
      val lastUpdated = Instant.now().minus(gracePeriod * 2, ChronoUnit.MINUTES)

      when(
        mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(
          Future.successful(
            Right(PreferenceFound(accepted = false, None, Some(lastUpdated), paperless = None))
          )
        )
      val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

      status(res) mustBe PRECONDITION_FAILED
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must be(
        """{"redirectUserTo":"/paperless/choose?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&amp;returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D&amp;survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"}"""
      )
    }

    "returns redirectUserTo with no request survey for an opted out user that has seen the survey" in {
      val now = Instant.now()

      when(
        mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(
          Future.successful(
            Right(
              PreferenceFound(
                accepted = false,
                None,
                paperless = None,
                surveys = Some(List(Survey(SurveyType.StandardInterruptOptOut, now)))
              )
            )
          )
        )
      val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

      status(res) mustBe PRECONDITION_FAILED
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must be(
        """{"redirectUserTo":"/paperless/choose?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&amp;returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"}"""
      )
    }
  }
}
