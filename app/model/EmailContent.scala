/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model

import play.api.libs.json.{ Json, OFormat }

final case class EmailContent(
  channel: String,
  from: String,
  to: List[To],
  tags: Option[Map[String, String]],
  options: Options,
  contactPolicy: ContactPolicy,
  requestedReceipts: Seq[String],
  content: Content,
  notifyUrl: String
)

final case class EmailAddress2(value: String)

object EmailAddress2 {
  implicit val format: OFormat[EmailAddress2] = Json.format[EmailAddress2]
}

final case class To(email: List[String], correlationId: String)
object To {
  implicit val format: OFormat[To] = Json.format[To]

}

final case class Content(`type`: String, subject: String, replyTo: Option[EmailAddress2], text: String, html: String)

object Content {

  implicit val format: OFormat[Content] = Json.format[Content]
}

final case class Options(trackClicks: Boolean, trackOpens: Boolean, fromName: String)

object Options {

  implicit val format: OFormat[Options] = Json.format[Options]
}

final case class ContactPolicy(
  contactPolicyGroup: String,
  channelCheckConsent: Boolean,
  channelApplyFrequencyCap: Boolean
)

object ContactPolicy {
  implicit val format: OFormat[ContactPolicy] = Json.format[ContactPolicy]
}

object EmailContent {
  implicit val format: OFormat[EmailContent] = Json.format[EmailContent]
}

object Channel {
  val EMAIL = "email"
}
