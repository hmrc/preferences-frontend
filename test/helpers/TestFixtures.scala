/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package helpers

import controllers.internal.ReOptInPage10
import controllers.internal.IPage8
import model.HostContext

object TestFixtures {
  val sampleHostContext = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText"
  )

  val sampleHostContextWithSurveyRequest = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText",
    survey = true
  )

  val sampleHostContextWithNoSurveyRequest = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText",
    survey = false
  )

  def alreadyOptedInUrlHostContext =
    HostContext(
      returnUrl = "someReturnUrl",
      returnLinkText = "someReturnLinkText",
      alreadyOptedInUrl = Some("someAlreadyOptedInUrl")
    )

  def reOptInHostContext(email: String) =
    HostContext(
      returnUrl = "someReturnUrl",
      returnLinkText = "someReturnLinkText",
      email = Some(email),
      cohort = Some(ReOptInPage10)
    )

  def reOptInHostContext() = reOptInHostContextWithRegime()

  def reOptInHostContextWithRegime(regime: Option[String] = None) =
    HostContext(
      returnUrl = "someReturnUrl",
      returnLinkText = "someReturnLinkText",
      cohort = Some(ReOptInPage10),
      regime = regime
    )

  def optinHostContext(email: String) =
    HostContext(
      returnUrl = "someReturnUrl",
      returnLinkText = "someReturnLinkText",
      email = Some(email),
      cohort = Some(IPage8)
    )

  def optinHostContext() =
    HostContext(
      returnUrl = "someReturnUrl",
      returnLinkText = "someReturnLinkText",
      cohort = Some(IPage8)
    )

}
