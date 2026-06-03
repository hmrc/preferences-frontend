/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.emailaddress

import scala.util.matching.Regex

trait ObfuscatedEmailAddress {
  val value: String
  override def toString: String = value
}

object ObfuscatedEmailAddress {
  final private val shortMailbox: Regex = "(.{1,2})".r
  final private val longMailbox: Regex = "(.)(.*)(.)".r

  implicit def obfuscatedEmailToString(e: ObfuscatedEmailAddress): String = e.value

  def apply(plainEmailAddress: String): ObfuscatedEmailAddress =
    new ObfuscatedEmailAddress {
      val value: String = plainEmailAddress match {
        case EmailAddressValidation.validEmail(shortMailbox(m), domain) =>
          s"${obscure(m)}@$domain"

        case EmailAddressValidation.validEmail(longMailbox(firstLetter, middle, lastLetter), domain) =>
          s"$firstLetter${obscure(middle)}$lastLetter@$domain"

        case invalidEmail =>
          throw new IllegalArgumentException(s"Cannot obfuscate invalid email address '$invalidEmail'")
      }
    }

  private def obscure(text: String): String = "*" * text.length
}
