/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc.QueryStringBindable

case class TimeoutHostContext(serviceUrl: Option[String] = None, regime: Option[String] = None)

object TimeoutHostContext {

  implicit def hostContextBinder(implicit
    stringBinder: play.api.mvc.QueryStringBindable[Encrypted[String]]
  ): QueryStringBindable[TimeoutHostContext] = {

    implicit class OptionOps(binderResult: Option[Either[String, Encrypted[String]]]) {
      def liftDecryptedOption: Option[String] =
        binderResult match {
          case Some(Right(encryptedValue)) => Some(encryptedValue.decryptedValue)
          case _                           => None
        }
    }

    new QueryStringBindable[TimeoutHostContext] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, TimeoutHostContext]] = {

        val maybeServiceUrl = stringBinder.bind("serviceUrl", params).liftDecryptedOption
        val maybeRegime = stringBinder.bind("regime", params).liftDecryptedOption

        Some(Right(TimeoutHostContext(maybeServiceUrl, maybeRegime)))
      }

      override def unbind(key: String, value: TimeoutHostContext): String = {
        value.regime.fold("") { regime =>
          stringBinder.unbind("regime", Encrypted(s"$regime"))
        } +
          value.serviceUrl.fold("") { serviceUrl =>
            "&" + stringBinder.unbind("serviceUrl", Encrypted(serviceUrl))
          }
      }.stripPrefix("&")

    }
  }

}
