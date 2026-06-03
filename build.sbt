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

import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{ defaultSettings, scalaSettings }

val appName = "preferences-frontend"

ThisBuild / majorVersion := 9
ThisBuild / scalaVersion := "3.6.3"

Global / lintUnusedKeysOnLoad := false

Compile / doc / sources := Seq.empty

scalafmtOnCompile := true

val excludedPackages: Seq[String] = Seq(
  "<empty>",
  "Reverse.*",
  ".*Routes.*",
  ".*BuildInfo.*",
  ".*\\$anon.*",
  "uk.gov.hmrc.abtest",
  "testOnlyDoNotUseInAppConf.*"
)

lazy val scoverageSettings =
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(","),
    ScoverageKeys.coverageMinimumStmtTotal := 90.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val commonSettings =
  scalaSettings ++ defaultSettings() ++
    scoverageSettings.settings ++
    Seq(
      libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
      PlayKeys.playDefaultPort := 9024,
      retrieveManaged := true,
      update / evictionWarningOptions :=
        EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      TwirlKeys.templateImports ++= Seq(
        "uk.gov.hmrc.govukfrontend.views.html.components._",
        "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
      )
    )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, BuildInfoPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(SassKeys.embedSources := true)
  .settings(commonSettings)
  .settings(
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
  )
  .settings(
    scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")),
    scalacOptions += "-language:implicitConversions",
    scalacOptions ++= Seq(
      "-Wconf:msg=unused import&src=.*:s",
      "-Wconf:msg=Flag.*repeatedly:s",
      "-Wconf:src=routes/.*:s"
    )
  )

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(`microservice` % "test->test")
  .settings(
    scalacOptions ++= Seq("-Wconf:msg=Flag.*repeatedly:s")
  )
