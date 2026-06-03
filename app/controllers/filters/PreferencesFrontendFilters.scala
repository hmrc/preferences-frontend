/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package controllers.filters

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.csp.CSPFilter
import uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters

import javax.inject.{ Inject, Singleton }

@Singleton
class PreferencesFrontendFilters @Inject() (
  exceptionHandlingFilter: ExceptionHandlingFilter,
  cspFilter: CSPFilter
) extends HttpFilters {

  override val filters: Seq[EssentialFilter] = Seq(exceptionHandlingFilter, cspFilter)
}
