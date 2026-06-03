/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package views

import org.scalatestplus.play.PlaySpec
import _root_.helpers.LanguageHelper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ViewHelperSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper {

  "serviceName" should {
    "return correct service name text" when {

      "input service name identifier is itsa" in {
        val result: Option[String] = ViewHelper.serviceName(Some("itsa"), None)

        result.map { msgKey =>
          msgKey mustBe "sa_printing_preference_svc_name_itsa"
          messagesInEnglish()(msgKey) mustBe "Sign up for Making Tax Digital for Income Tax"
          messagesInWelsh()(msgKey) mustBe "Cofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm"
        }
      }

      "input regime name is itsa" in {
        val result: Option[String] = ViewHelper.serviceName(Some("unknown"), Some("itsa"))

        result.map { msgKey =>
          msgKey mustBe "sa_printing_preference_svc_name_itsa"
          messagesInEnglish()(msgKey) mustBe "Sign up for Making Tax Digital for Income Tax"
          messagesInWelsh()(msgKey) mustBe "Cofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm"
        }
      }

      "neither service identifier nor regime name is itsa" in {
        val nonITSAServiceName = "nonitsa"
        val result: Option[String] = ViewHelper.serviceName(Some(nonITSAServiceName), Some(nonITSAServiceName))

        result.map { serviceName =>
          serviceName mustBe nonITSAServiceName
        }
      }
    }
  }
}
