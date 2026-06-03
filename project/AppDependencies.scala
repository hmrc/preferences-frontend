/*
 * Copyright 2021 HM Revenue & Customs
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

import sbt.*

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "io.lemonlabs"  %% "scala-uri"                % "4.0.3",
    "org.typelevel" %% "cats-core"                % "2.13.0",
    "uk.gov.hmrc"   %% "reactive-circuit-breaker" % "6.1.0",
    "uk.gov.hmrc"   %% "sca-wrapper-play-30"      % "5.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % "10.7.0"   % Test,
    "org.scalatestplus" %% "scalacheck-1-17"        % "3.2.18.0" % Test
  )
}
