/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers

import play.api.Configuration
import javax.inject.{ Inject, Singleton }

@Singleton
class ExternalUrlPrefixes @Inject() (configuration: Configuration) {
  lazy val pfUrlPrefix = configuration.get[String](s"preferences-frontend.host")
  lazy val ytaUrlPrefix = configuration.get[String](s"yta.host")
  lazy val caUrlPrefix = configuration.getOptional[String](s"company-auth.host").getOrElse("")
  lazy val bas_gateway_frontendUrl = configuration.getOptional[String]("bas-gateway-frontend.host").getOrElse("")
}

@Singleton
class ExternalUrls @Inject() (externalUrlPrefixes: ExternalUrlPrefixes) {
  lazy val betaFeedbackUrl = s"${externalUrlPrefixes.caUrlPrefix}/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"${externalUrlPrefixes.caUrlPrefix}/contact/beta-feedback-unauthenticated"
  lazy val taxAccountRedirect = s"${externalUrlPrefixes.ytaUrlPrefix}/account"
  lazy val bta = "business-account"
  lazy val btaSignoutUrl = s"${externalUrlPrefixes.ytaUrlPrefix}/$bta/sso-sign-out"
  lazy val ptaSignoutUrl = s"${externalUrlPrefixes.bas_gateway_frontendUrl}/bas-gateway/sign-out-without-state"

  def survey(returnUrl: Option[String] = None): String =
    returnUrl
      .map { r =>
        if (r.contains(bta)) btaSignoutUrl else ptaSignoutUrl
      }
      .getOrElse(ptaSignoutUrl)
}
