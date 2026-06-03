/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import _root_.connectors.*
import controllers.AuthRetrievalsSetup
import helpers.TestFixtures
import model.Language.English
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ when, * }
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, Retrieval, ~ }
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{ ZoneOffset, ZonedDateTime }
import scala.concurrent.{ ExecutionContext, Future }

class ReOptInActivationSpec
    extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar with ScalaFutures
    with AuthRetrievalsSetup {

  val gracePeriod = 10
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val mockPreferencesConnector: PreferencesConnector = mock[PreferencesConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
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
  val controller = app.injector.instanceOf[ActivationController]
  val router = app.injector.instanceOf[Router]

  "ActivationController.activate" when {
    "paperless is true and " when {
      "preference's majorVersion is lower than current majorVersion and " when {
        "Affinity group is Individual and " when {
          "ConfidenceLevel == 200" should {
            "return PRECONDITION_FAILED" in new TestCase(paperless = Some(true)) {
              val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
              status(response) mustBe PRECONDITION_FAILED
              withClue("response content should have a redirect link to a current ReOptIn page") {
                contentAsJson(response) mustBe (Json.parse(s"""{"redirectUserTo": "$reOptInUrl"}"""))
              }
            }
          }
        }
      }
    }

    "paperless is false and " when {
      "preference's majorVersion is lower than current majorVersion and " when {
        "Affinity group is Individual and " when {
          "ConfidenceLevel == 200" should {
            "return OK" in new TestCase(paperless = Some(false)) {
              val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
              status(response) mustBe OK
            }
          }
        }
      }
    }

    "paperless is not defined and " when {
      "preference's majorVersion is lower than current majorVersion and" when {
        "Affinity group is Individual and " when {
          "ConfidenceLevel == 200" should {
            "return OK" in new TestCase(paperless = None) {
              val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
              status(response) mustBe OK
            }
          }
        }
      }
    }
    "preference's majorVersion is lower than the current majorVersion and " when {
      "Affinity group is Organization and " when {
        "ConfidenceLevel is == 200" should {
          "return OK" in new TestCase() {
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe OK
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLevel is <  200" should {
          "return OK" in new TestCase() {
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe OK
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLevel is >  200" should {
          "return OK" in new TestCase() {
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe OK
          }
        }
      }
    }
    "preference's majorVersion is the same as current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLevel is >= 200" should {
          "return OK" in new TestCase(prefMajor = CohortCurrent.ipage.majorVersion) {
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe OK
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "there is a pending email in preferneces" when {
        "Affinity group is Individual and " when {
          "ConfidenceLevel is ==  200" should {
            "return OK" in new TestCase(pendingEmail = Some("foo@bar.com")) {
              val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
              status(response) mustBe OK
            }
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLevel is !=  200 and " when {
          "Is paperless " should {
            "return OK" in new TestCase(paperless = Some(true)) {
              val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
              status(response) mustBe PRECONDITION_FAILED
            }
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "Email has bounces and " when {
        "Affinity group is Individual and " when {
          "ConfidenceLevel is =  200 and " when {
            " reOptInOptimisation.switchOn flag is true" should {
              "return PRECONDITION_FAILED" in new TestCase(
                hasBounces = true
              ) {
                val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
                status(response) mustBe PRECONDITION_FAILED
              }
            }
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "Email is not verified and " when {
        "Affinity group is Individual and " when {
          "ConfidenceLevel is ==  200 and " when {
            " reOptInOptimisation.switchOn flag is true" should {
              "return PRECONDITION_FAILED" in new TestCase(
                isEmailVerified = false
              ) {
                val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
                status(response) mustBe OK
              }
            }
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLevel is ==  200 and " when {
          "Is paperless " should {
            "return OK" in new TestCase(paperless = Some(true)) {
              val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
              status(response) mustBe PRECONDITION_FAILED
            }
          }
        }
      }
    }

  }
  class TestCase(
    prefMajor: Int = CohortCurrent.ipage.majorVersion - 1,
    paperless: Option[Boolean] = Option.empty[Boolean],
    pendingEmail: Option[String] = None,
    hasBounces: Boolean = false,
    isEmailVerified: Boolean = true
  ) {

    val email = EmailPreference("test@test.com", isEmailVerified, hasBounces, false, None, Some(English), pendingEmail)
    def initMocks(): OngoingStubbing[Future[AuthRetrievals]] = {
      reset(mockAuthConnector)
      reset(mockPreferencesConnector)
      when(
        mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(
          Future.successful(
            Right(PreferenceFound(true, Some(email), majorVersion = Some(prefMajor), paperless = paperless))
          )
        )

      when(
        mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(retrievalResult)
    }

    val reOptInUrl = controllers.internal.paperless.routes.ChoosePaperlessController
      .displayForm(
        Some(CohortCurrent.reoptinpage),
        email = None,
        TestFixtures.reOptInHostContext(email.email).copy(cohort = Some(ReOptInPage55))
      )
    initMocks()
  }

}
