/*
 * Copyright 2023 HM Revenue & Customs
 *
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
