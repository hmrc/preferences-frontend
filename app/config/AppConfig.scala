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

package config

import controllers.ExternalUrls
import play.api.Configuration

import javax.inject.Inject

class AppConfig @Inject() (val configuration: Configuration, externalUrls: ExternalUrls) {
  lazy val betaFeedbackUrl = externalUrls.betaFeedbackUrl
  lazy val betaFeedbackUnauthenticatedUrl = externalUrls.betaFeedbackUnauthenticatedUrl
  lazy val homeUrl = externalUrls.taxAccountRedirect
  def signOutUrl(returnUrl: Option[String]): String = externalUrls.survey(returnUrl)
  def sessionTimeoutInSeconds: Int = configuration.getOptional[Int]("session.timeoutSeconds").getOrElse(900)
  def sessionCountdownInSeconds: Int = configuration.getOptional[Int]("session.countdownSeconds").getOrElse(60)
}
