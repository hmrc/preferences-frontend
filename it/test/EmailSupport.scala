/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

import model.EmailContent
import org.scalatest.Assertion
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.matchers.{ HavePropertyMatchResult, HavePropertyMatcher, Matcher }
import play.api.libs.json.{ JsString, Json, OWrites, Reads }
import play.api.libs.ws.WSResponse
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.test.Helpers.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.util.matching.Regex.Match

trait EmailSupport extends TestCaseWithFrontEndAuthentication with IntegrationPatience with Eventually {

  import EmailSupport._

  import scala.concurrent.duration._

  implicit val emailReads: Reads[Email] = Json.reads[Email]
  implicit val emailTokenWrites: OWrites[Token] = Json.writes[Token]

  lazy val digitalContactStubUrl = servicesConfig.baseUrl("digital-contact-stub")
  private lazy val emailBaseUrl = servicesConfig.baseUrl("email")
  private lazy val timeout = 5.seconds

  val emptyJsonValue = Json.parse("{}")

  def reset() = wsClient.url(s"$digitalContactStubUrl/digital-contact-stub/imi/reset").get().futureValue

  def clearEmailQueue =
    wsClient
      .url(s"$emailBaseUrl/test-only/hmrc/email-admin/clear-email-queues")
      .post(JsString(""))
      .futureValue
      .status must be(
      OK
    )

  def clearEmails() = {
    eventually(
      wsClient
        .url(s"$emailBaseUrl/test-only/hmrc/email-admin/process-email-queue")
        .post(JsString(""))
        .futureValue
        .status must be(
        OK
      )
    )
    wsClient.url(s"$digitalContactStubUrl/digital-contact-stub/imi/reset").get().futureValue
  }

  def emails: Future[List[EmailContent]] = {
    val resp = wsClient.url(s"$digitalContactStubUrl/digital-contact-stub/imi/messages").get()
    resp.futureValue.status must be(OK)
    resp.map(r => r.json.as[List[EmailContent]])
  }

  def emailForAddress(email: String): Future[List[EmailContent]] = {
    val resp = wsClient.url(s"$digitalContactStubUrl/digital-contact-stub/imi/messages/email/$email").get()
    resp.futureValue.status must be(OK)
    resp.map(r => r.json.as[List[EmailContent]])
  }

  def verificationTokenFromEmail(email: String) = {
    val emailList = Await.result(emailForAddress(email), timeout)

    val regex = "/sa/print-preferences/verification/([-a-f0-9]+)".r

    val token: Option[Match] = regex.findFirstMatchIn(emailList.head.content.text)
    token.map(matches => matches.group(1)).get
  }

  def verificationTokenFromMultipleEmailsFor(emailRecipient: String) = {
    val emailList =
      emailForAddress(emailRecipient).futureValue // Await.result(emailForAddress(emailRecipient), timeout)
    val emailMatchedList = emailList.filter(x => x.to.head.email.contains(emailRecipient))

    val regex = "/sa/print-preferences/verification/([-a-f0-9]+)".r

    val token: Option[Match] = regex.findFirstMatchIn(emailMatchedList.head.content.text)
    token.map(matches => matches.group(1)).get
  }

  case object SaPrintPreferencesVerification {
    def verify(token: String) = wsUrl(s"/sa/print-preferences/verification/$token").get()
  }

  def withReceivedEmails(expectedCount: Int)(assertions: List[EmailContent] => Assertion): Assertion = {
    val listOfMails = eventually {
      val emailList = emails.futureValue
      emailList.size mustBe expectedCount
      emailList
    }
    assertions(listOfMails)
  }

  def withReceivedEmailsForAddress(expectedCount: Int, email: String)(
    assertions: List[EmailContent] => Assertion
  ): Assertion = {
    val listOfMails = eventually {
      val emailList = emailForAddress(email).futureValue
      emailList.size mustBe expectedCount
      emailList
    }
    assertions(listOfMails)
  }

  def aVerificationEmailIsReceivedFor(email: String): Assertion =
    withReceivedEmailsForAddress(1, email) {
      case List(mail) =>
        mail.to.head.email.head must be(email)
        mail.content.subject must be("HMRC electronic communications: verify your email address")
      case _ => fail()
    }

  def beForAnExpiredOldEmail: Matcher[Future[WSResponse]] =
    have(statusWith(200)) and
      have(bodyWith("You&#x27;ve used a link that has now expired")) and
      have(bodyWith("It may have been sent to an old or alternative email address.")) and
      have(bodyWith("Please use the link in the latest verification email sent to your specified email address."))

  def bodyWith(expected: String) =
    new HavePropertyMatcher[Future[WSResponse], String] {
      def apply(response: Future[WSResponse]) =
        HavePropertyMatchResult(
          matches = response.futureValue.body.contains(expected),
          propertyName = "Response Body",
          expectedValue = expected,
          actualValue = response.futureValue.body
        )
    }
  def statusWith(expected: Int) =
    new HavePropertyMatcher[Future[WSResponse], Int] {
      def apply(response: Future[WSResponse]) =
        HavePropertyMatchResult(
          matches = response.futureValue.status.equals(expected),
          propertyName = "Response Status",
          expectedValue = expected,
          actualValue = response.futureValue.status
        )
    }
}

object EmailSupport {
  // TODO simplify this type
  case class Email(
    from: String,
    to: Option[String],
    subject: String,
    text: Option[String],
    html: Option[String],
    cc: Option[String],
    bcc: Option[String]
  )
  case class Token(token: String)
}
