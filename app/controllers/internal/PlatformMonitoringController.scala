/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import controllers.Assets
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject

// DC-679: Moving monitoring to new controller because we require to disable auditing.

class PlatformHealthCheckController @Inject() (mcc: MessagesControllerComponents, assets: Assets)
    extends FrontendController(mcc) {
  def getAsset(fileName: String) = assets.at(path = "/public", file = fileName)
}
