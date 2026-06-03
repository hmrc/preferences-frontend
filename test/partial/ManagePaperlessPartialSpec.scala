/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package partial

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import connectors.PreferenceResponse._
import connectors.SaEmailPreference.Status
import connectors.{ SaEmailPreference, SaPreference }
import controllers.ExternalUrlPrefixes
import controllers.auth.AuthenticatedRequest
import controllers.internal.routes
import model.HostContext
import java.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import partial.paperless.manage.ManagePaperlessPartial
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier

class ManagePaperlessPartialSpec
    extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with LanguageHelper with ConfigHelper {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val hostContext: HostContext = TestFixtures.sampleHostContext
  override implicit lazy val app: Application = fakeApp
  val externalUrlPrefixes = app.injector.instanceOf[ExternalUrlPrefixes]
  val managePaperlessPartial = app.injector.instanceOf[ManagePaperlessPartial]

  implicit val messages: Messages = messagesInEnglish()

  def linkTo(s: Call) = externalUrlPrefixes.pfUrlPrefix + s.url.replaceAll("&", "&amp;")

  "Manage Paperless partial" should {
    implicit val request = AuthenticatedRequest(FakeRequest("GET", "/portal/sa/123456789"), None, None, None, None)

    "contain pending email details in content when opted-in and unverified" in {
      val emailPreferences = SaEmailPreference(
        email = "test@test.com",
        status = Status.Pending,
        mailboxFull = false,
        linkSent = Some(LocalDate.of(2014, 10, 2))
      )
      val saPreference = SaPreference(digital = true, Some(emailPreferences)).toNewPreference()

      managePaperlessPartial(Some(saPreference)).body must (
        include("Email for paperless notifications") and
          include(emailPreferences.email) and
          include("Send a new verification email") and
          include(linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))) and
          include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
          include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
          include("2 October 2014")
      )
    }

    "contain verified email details in content when opted-in and verified" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.Verified, false)
      val saPreference = SaPreference(true, Some(emailPreferences)).toNewPreference()

      managePaperlessPartial(Some(saPreference)).body must (
        include("Email address for HMRC digital communications") and
          include("Emails are sent to") and
          include(EmailAddress(emailPreferences.email).obfuscated) and
          include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
          include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
          not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain bounced email with 'mailbox filled up' details in content when the 'current' email is bounced with full mailbox error" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.Bounced, mailboxFull = true)
      val saPreference = SaPreference(true, Some(emailPreferences)).toNewPreference()

      managePaperlessPartial(Some(saPreference)).body must (
        include("You need to verify") and
          include(emailPreferences.email) and
          include("your inbox is full") and
          include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
          include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
          not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain bounced email with 'email can't be delivered' in content when the 'current' email is bounced with email can't be delivered error" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.Bounced, mailboxFull = false)
      val saPreference = SaPreference(true, Some(emailPreferences)).toNewPreference()

      managePaperlessPartial(Some(saPreference)).body must (
        include("You need to verify") and
          include(emailPreferences.email) and
          include("The email telling you how to do this can&#x27;t be delivered.") and
          include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
          include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
          not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain bounced email but no 'full mailbox' details in content when the 'current' email is bounced with other error" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.Bounced, false)
      val saPreference = SaPreference(true, Some(emailPreferences)).toNewPreference()

      managePaperlessPartial(Some(saPreference)).body must (
        include("You need to verify") and
          include(emailPreferences.email) and
          include("can&#x27;t be delivered") and
          include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
          include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
          not include "your inbox is full" and
          not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain opted out details in content when user is opted-out" in {
      val saPreference = SaPreference(false, None).toNewPreference()

      managePaperlessPartial(Some(saPreference)).body must (
        include("Replace the letters you get about taxes with emails.") and
          include(
            linkTo(
              controllers.internal.paperless.routes.ChoosePaperlessController
                .redirectToDisplayFormWithCohort(None, hostContext)
            )
          ) and
          not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain opted out details in content when user has no preference set" in {
      managePaperlessPartial(None).body must (
        include("Replace the letters you get about taxes with emails.") and
          include(
            linkTo(
              controllers.internal.paperless.routes.ChoosePaperlessController
                .redirectToDisplayFormWithCohort(None, hostContext)
            )
          ) and
          not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }
  }
}
