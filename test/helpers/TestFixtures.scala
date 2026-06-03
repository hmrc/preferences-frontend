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
