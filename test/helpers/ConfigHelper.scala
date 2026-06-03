/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package helpers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpAuditing

trait ConfigHelper {
  self: GuiceOneAppPerSuite =>

  lazy val fakeApp: Application = new GuiceApplicationBuilder()
    .overrides(bind[HttpAuditing].to[DefaultHttpAuditing].eagerly())
    .configure("metrics.enabled" -> false)
    .build()

}
