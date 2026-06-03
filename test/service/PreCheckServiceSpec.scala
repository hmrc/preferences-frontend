/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package service

import connectors.{ EmailPreference, MultiplePreferenceFound, PreferenceNotFound, PreferenceResponse, PreferenceStatus, PreferencesConnector, TermsAndConditionsAcceptance }
import model.JourneyTypeDC.ReOptIn
import model.{ BounceEmailJourney, ConflictJourney, EmailVerificationJourney, OptInJourney, ReOptInJourney, ReOptInModifiedJourney, SilentRedirectJourney }

import java.time.LocalDate
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.*

import scala.concurrent.{ ExecutionContext, Future }

class PreCheckServiceSpec
    extends AnyWordSpec with PreferenceResponseGen with Matchers with MockitoSugar with ScalaFutures {

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private lazy val mockPreferencesConnector: PreferencesConnector = mock[PreferencesConnector]
  private val preCheckService = new PreferencesPreCheckService(mockPreferencesConnector)

  "determineJourney" should {
    "return SilentRedirect for a preference with accepted general terms and conditions and email verified and no bounces" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(preferences())))
      preCheckService.determineJourney().futureValue mustBe SilentRedirectJourney(entityId, "HappyPath")
    }

    "return OptIn Journey for a customer who doesn't have any preferences settings" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Left(PreferenceNotFound(None))))
      preCheckService.determineJourney().futureValue mustBe OptInJourney("New customer with no preference record")
    }

    "return OptIn Journey for a customer who is not yet opted for paperless" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(preferences(termsAccepted = false))))
      preCheckService.determineJourney().futureValue mustBe OptInJourney("Existing customer with T&C not accepted")
    }

    "return Email Verification Journey for a customer who accepted T&Cs, but email is not verified yet" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(preferences(isVerified = false))))
      preCheckService.determineJourney().futureValue mustBe EmailVerificationJourney(
        "T&C accepted, but email is not verified yet",
        "pihklyljtgoxeoh@mail.com"
      )
    }

    "return Email Verification Journey for a customer who accepted T&Cs, but email is not verified yet and on old T&Cs" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(preferences(isVerified = false, majorVersion = 0))))
      preCheckService.determineJourney().futureValue mustBe EmailVerificationJourney(
        "T&C accepted, but email is not verified yet",
        "pihklyljtgoxeoh@mail.com"
      )
    }

    "return Bounce Journey for a customer who has email bounces" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(preferences(hasBounces = true))))
      preCheckService.determineJourney().futureValue mustBe BounceEmailJourney(
        "T&C accepted, but email bounced",
        "pihklyljtgoxeoh@mail.com"
      )
    }

    "return Re-OptIn Journey for a customer who has accepted old T&Cs" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(preferences(majorVersion = 0))))
      preCheckService.determineJourney().futureValue mustBe ReOptInJourney(
        "New T&Cs are need to be accepted",
        emailPreference(true, true, false, false)
      )
    }

    "return Bounce Journey for a customer who has accepted old T&Cs and has the pending email bounced" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(
            Right(preferences(hasBounces = true, majorVersion = 0, pendingEmail = Some("pihklyljtgoxeoh@mail.com")))
          )
        )
      preCheckService.determineJourney().futureValue mustBe
        BounceEmailJourney("Pending Email bounced, so new T&Cs are not accepted", "pihklyljtgoxeoh@mail.com")
    }

    "return ReOptInModifiedJourney for a customer who has accepted old T&Cs and has a bounced email" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(preferences(hasBounces = true, majorVersion = 0))))
      preCheckService.determineJourney().futureValue mustBe ReOptInModifiedJourney(
        "Email bounced, so new T&Cs are not accepted",
        emailPreference(true, true, false, true)
      )
    }

    "return ConflictJourney when multiple preferences is found" in {
      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Left(MultiplePreferenceFound())))

      preCheckService.determineJourney().futureValue mustBe ConflictJourney("Multiple Preferences Found")
    }

    "throw exception when api returns invalid value of PreferenceStatus" in {
      case class Unknown() extends PreferenceStatus

      when(mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Left(Unknown())))

      intercept[Exception] {
        await(preCheckService.determineJourney())
      }
    }
  }

  "determineJourney" should {

    "behave as expected" in {
      forAll(decisionTable) {
        (hasConsented, hasEmail, isVerified, emailLinkExpired, bounced, newTnCs, expectedResult) =>
          forAll(preferenceResponseGen(hasConsented, hasEmail, isVerified, emailLinkExpired, bounced, newTnCs)) {
            preferenceResponse =>
              when(
                mockPreferencesConnector.getPreferencesUnresolved()(any[HeaderCarrier], any[ExecutionContext])
              )
                .thenReturn(Future.successful(Right(preferenceResponse)))

              preCheckService.determineJourney().futureValue mustBe expectedResult
          }
      }
    }
  }

  private def preferences(
    isVerified: Boolean = true,
    hasBounces: Boolean = false,
    termsAccepted: Boolean = true,
    containsEmail: Boolean = true,
    majorVersion: Int = 1,
    pendingEmail: Option[String] = None
  ) =
    PreferenceResponse(
      Map("generic" -> TermsAndConditionsAcceptance(termsAccepted, majorVersion = Some(majorVersion))),
      if (!containsEmail)
        None
      else
        emailPreference(true, isVerified, false, hasBounces, pendingEmail),
      entityId = Some(entityId)
    )
}

trait PreferenceResponseGen extends ScalaCheckPropertyChecks {

  val boolGen: Gen[Boolean] = Gen.oneOf(true, false)

  val entityId = "testEntityid"
  def preferenceResponseGen(
    hasConsentedGen: Gen[Boolean] = boolGen,
    hasEmailAddressGen: Gen[Boolean] = boolGen,
    isVerifiedGen: Gen[Boolean] = boolGen,
    hasEmailLinkExpiredGen: Gen[Boolean] = boolGen,
    isBouncedGen: Gen[Boolean] = boolGen,
    hasNewTncGen: Gen[Boolean] = boolGen
  ): Gen[PreferenceResponse] =
    for {
      hasConsented        <- hasConsentedGen
      hasEmailAddress     <- hasEmailAddressGen
      isVerified          <- isVerifiedGen
      hasEmailLinkExpired <- hasEmailLinkExpiredGen
      isBounced           <- isBouncedGen
      hasNewTnc           <- hasNewTncGen
    } yield {

      val email = emailPreference(hasEmailAddress, isVerified, hasEmailLinkExpired, isBounced)

      val tncsVersion = if (hasNewTnc) 1 else 0
      val tncs =
        Map("generic" -> TermsAndConditionsAcceptance(accepted = hasConsented, majorVersion = Some(tncsVersion)))

      PreferenceResponse(tncs, email, entityId = Some(entityId))
    }

  def emailPreference(
    hasEmailAddress: Boolean,
    isVerified: Boolean,
    hasEmailLinkExpired: Boolean,
    isBounced: Boolean,
    pendingEmail: Option[String] = None
  ): Option[EmailPreference] =
    if (!hasEmailAddress) None
    else {
      val now = LocalDate.now()
      val linkSent = if (hasEmailLinkExpired) Some(now) else Some(now.minusDays(8))
      val emailPreference =
        EmailPreference(
          email = "pihklyljtgoxeoh@mail.com",
          isVerified = isVerified,
          hasBounces = isBounced,
          mailboxFull = false,
          linkSent = linkSent,
          language = None,
          pendingEmail = pendingEmail
        )
      Some(emailPreference)
    }

  val decisionTable =
    Table(
      ("HasConsented", "HasEmail", "IsVerified", "Email Link Expired", "Bounced", "New T&Cs", "result"),
      (
        Gen.const(false),
        boolGen,
        boolGen,
        boolGen,
        boolGen,
        boolGen,
        OptInJourney("Existing customer with T&C not accepted")
      ),
      (
        Gen.const(true),
        Gen.const(false),
        boolGen,
        boolGen,
        boolGen,
        boolGen,
        OptInJourney("T&Cs are accepted but no valid email address provided")
      ),
      (
        Gen.const(true),
        Gen.const(true),
        Gen.const(false),
        Gen.const(false),
        Gen.const(false),
        boolGen,
        EmailVerificationJourney("T&C accepted, but email is not verified yet", "pihklyljtgoxeoh@mail.com")
      ),
      (
        Gen.const(true),
        Gen.const(true),
        Gen.const(true),
        boolGen,
        Gen.const(true),
        Gen.const(true),
        BounceEmailJourney("T&C accepted, but email bounced", "pihklyljtgoxeoh@mail.com")
      ),
      (
        Gen.const(true),
        Gen.const(true),
        Gen.const(true),
        boolGen,
        Gen.const(false),
        Gen.const(true),
        SilentRedirectJourney(entityId, "HappyPath")
      ),
      (
        Gen.const(true),
        Gen.const(true),
        Gen.const(true),
        Gen.const(false),
        Gen.const(false),
        Gen.const(false),
        ReOptInJourney("New T&Cs are need to be accepted", emailPreference(true, true, false, false), ReOptIn)
      )
    )

}
