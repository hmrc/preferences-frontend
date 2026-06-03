/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.{ EmailPreference, EntityResolverConnector, PreferenceNotFound, PreferenceResponse, PreferenceStatus, PreferencesConnector, PreferencesCreated, TermsAndConditionsUpdate }
import controllers.internal.{ CohortCurrent, OptInCohort }
import model.{ HostContext, Language }
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{ any, eq => is }
import org.mockito.Mockito.{ reset, when }
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{ contentAsString, defaultAwaitTimeout, status }
import service.PreCheckService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.emailaddress.EmailAddressValidation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ ExecutionContext, Future }

class ChoosePaperlessControllerSpecTC
    extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach
    with ChoosePaperlessControllerSetup {

  override def assignedCohort: OptInCohort = CohortCurrent.ipage

  val mockAuditConnector = mock[AuditConnector]
  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockPreferencesConnector = mock[PreferencesConnector]
  val mockAuthConnector = mock[AuthConnector]
  val emailAddressValidation = mock[EmailAddressValidation]
  val mockPreCheckService = mock[PreCheckService]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector),
        bind[PreferencesConnector].toInstance(mockPreferencesConnector),
        bind[EmailAddressValidation].toInstance(emailAddressValidation)
      )
      .configure(
        "metrics.enabled" -> false
      )
      .build()

  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuthConnector)
    reset(mockEntityResolverConnector)
    reset(mockPreferencesConnector)
    reset(emailAddressValidation)

    when(
      mockPreferencesConnector.getPreferencesStatus(any[String])(any[HeaderCarrier], any[ExecutionContext])
    )
      .thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(None))))
    when(
      mockAuthConnector.authorise[AuthRetrievals](any[Predicate], any[Retrieval[AuthRetrievals]])(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(retrievalResult)
  }

  val controller = app.injector.instanceOf[ChoosePaperlessController]

  "The language form" should {

    "render english radio button checked for undefiend preferences" in new ChoosePaperlessControllerSetup {

      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(
        Future.successful(None)
      )

      val page = controller.displayLanguageForm(
        HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText"
        )
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("lang").attributes().hasKey("checked") must be(true)
      document.getElementById("lang-2").attributes().hasKey("checked") must be(false)
    }

    "render english radio button checked for English in preferences" in new ChoosePaperlessControllerSetup {
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(
        Future.successful(
          Some(
            PreferenceResponse(
              termsAndConditions = Map(),
              email = Some(EmailPreference("test@test.com", false, false, false, None, Some(Language.English)))
            )
          )
        )
      )

      val page = controller.displayLanguageForm(
        HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText"
        )
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("lang").attributes().hasKey("checked") must be(true)
      document.getElementById("lang-2").attributes().hasKey("checked") must be(false)
    }

    "render welsh radio button checked for Welsh in preferences" in new ChoosePaperlessControllerSetup {
      when(mockPreferencesConnector.getPreferences()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(
        Future.successful(
          Some(
            PreferenceResponse(
              termsAndConditions = Map(),
              email = Some(EmailPreference("test@test.com", false, false, false, None, Some(Language.Welsh)))
            )
          )
        )
      )
      val page = controller.displayLanguageForm(
        HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText"
        )
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("lang").attributes().hasKey("checked") must be(false)
      document.getElementById("lang-2").attributes().hasKey("checked") must be(true)
    }
  }

  "A post to submitLanguageForm" should {
    "send a request to entityResolverConnector with Welsh" in new ChoosePaperlessControllerSetup {

      val requestHostContext = HostContext(
        returnUrl = "someReturnUrl",
        returnLinkText = "someReturnLinkText"
      )

      when(
        mockPreferencesConnector
          .changeEmailLanguage(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.Welsh))))(
            any[HeaderCarrier],
            is(requestHostContext),
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitLanguageForm(requestHostContext)(FakeRequest().withFormUrlEncodedBody("language" -> "true"))

      status(page) mustBe 303

    }

    "send a request to preferences connector with English" in new ChoosePaperlessControllerSetup {

      val requestHostContext = HostContext(
        returnUrl = "someReturnUrl",
        returnLinkText = "someReturnLinkText"
      )
      when(
        mockPreferencesConnector
          .changeEmailLanguage(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.English))))(
            any[HeaderCarrier],
            is(requestHostContext),
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitLanguageForm(requestHostContext)(FakeRequest().withFormUrlEncodedBody("language" -> "false"))

      status(page) mustBe 303
    }

    "return 400 if form is invalid" in new ChoosePaperlessControllerSetup {

      val requestHostContext = HostContext(
        returnUrl = "someReturnUrl",
        returnLinkText = "someReturnLinkText"
      )
      val page =
        controller.submitLanguageForm(requestHostContext)(
          FakeRequest().withFormUrlEncodedBody("language" -> "foobar")
        )

      status(page) mustBe 400

    }

  }

}
