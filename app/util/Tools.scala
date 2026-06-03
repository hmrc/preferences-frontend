/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package util

import com.google.inject.Inject
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto

import java.net.URLEncoder

class Tools @Inject() (val applicationCrypto: ApplicationCrypto) {
  def urlEncode(u: String): String = URLEncoder.encode(u, "UTF-8")
  def encryptAndEncode(s: String): String =
    urlEncode(applicationCrypto.QueryParameterCrypto.encrypt(PlainText(s)).value)
}
