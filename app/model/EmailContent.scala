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
