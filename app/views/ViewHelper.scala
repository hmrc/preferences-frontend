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

package views

object ViewHelper {
  val ITSA = "itsa"

  val EMAIL_RE_VERIFY = "re-verify"
  val EMAIL_BOUNCE = "bounce"
  val RE_OPT_IN_MODIFY = "re-opt-in-modify"

  def serviceName(svcName: Option[String], regimeName: Option[String]): Option[String] =
    (svcName, regimeName) match {
      case (Some(svc), _) if svc == ITSA => Some("sa_printing_preference_svc_name_itsa")
      case (_, Some(rn)) if rn == ITSA   => Some("sa_printing_preference_svc_name_itsa")
      case (_, _)                        => svcName
    }
}
