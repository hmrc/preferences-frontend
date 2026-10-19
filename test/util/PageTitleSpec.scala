package util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import util.PageTitle

class PageTitleSpec extends AnyWordSpec with Matchers {

  "PageTitle" should {

    "append the service name and GOV.UK" in {
      PageTitle("For your security, we signed you out", Some("My service")) shouldBe
        "For your security, we signed you out – My service – GOV.UK"
    }

    "omit the service name when it is missing" in {
      PageTitle("For your security, we signed you out", None) shouldBe
        "For your security, we signed you out – GOV.UK"
    }

    "omit the service name when it is blank" in {
      PageTitle("For your security, we signed you out", Some("   ")) shouldBe
        "For your security, we signed you out – GOV.UK"
    }
  }
}
