/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model

import controllers.internal.{ ReOptInPage10, ReOptInPage52 }
import helpers.ConfigHelper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application

class HostContextSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "Binding a host context" should {
    val validReturnUrl = "returnUrl"                          -> Seq("J5lnze8P0QQ8NwFTHVHhVw==")
    val validReturnLinkText = "returnLinkText"                -> Seq("w/PwaxV+KgqutfsU0cyrJQ==")
    val validAlreadyOptedInUrl = "alreadyOptedInUrl"          -> Seq("Co1YzTJv/KYa5nRQXLAqlw==")
    val validGenericTermsAndConditions = "termsAndConditions" -> Seq("HYymhRDn1B7qdcKcjIf/1A==")
    val validWelshLanguage = "language"                       -> Seq("5W0FAIi6JRZBSf4/hwE00w==") // cy
    val validCohortType52 = "cohort"                          -> Seq("dPFnTTu7gdct/zMj/owK2Q==") // ReOptInPage52
    val surveyYes = "survey"                                  -> Seq("hrcOMaf19lUfbNYcQ9B7mA==") // yes
    val regime = "regime"                                     -> Seq("KucfrgeglpOjHad59vo1xg==") // itsa
    val entityId =
      "entityId" -> Seq(
        "2N2GyqTh0egUZazS9deq4A06NP6q%2BH4Ozpi%2BUNqIBs1nTZPr1T4AqZU67fpoYnvK"
      ) // 450262a0-1842-4885-8fa1-6fbc2aeb867d

    val validServiceUrl = "serviceUrl" -> Seq(
      "42aO70DPMFVgpFdKEsKkRSEXIZFOqu6HvasXX7XME4UPeXjGnVjtd3RlgW5d/uOmJaQzr5LM0OrIPOeOwgWuLdo39A/ctEn5c3uwO6aUo3DaEG4U54Wj24mXuEsKLR2H"
    )

    "read the serviceUrl if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validServiceUrl)) should contain(
        Right(
          HostContext(
            returnUrl = "test@test.com",
            returnLinkText = "bar",
            serviceUrl = Some("https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax")
          )
        )
      )
    }

    "write out all parameters when serviceUrl is present" in {
      model.HostContext.hostContextBinder
        .unbind(
          "anyValName",
          HostContext(
            returnUrl = "foo&value",
            returnLinkText = "bar",
            serviceUrl = Some("https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax")
          )
        ) should be(
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&serviceUrl=42aO70DPMFVgpFdKEsKkRSEXIZFOqu6HvasXX7XME4UPeXjGnVjtd3RlgW5d%2FuOmJaQzr5LM0OrIPOeOwgWuLdo39A%2FctEn5c3uwO6aUo3DaEG4U54Wj24mXuEsKLR2H"
      )
    }

    "read the returnURL and returnLinkText if both present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText)) should contain(
        Right(HostContext(returnUrl = "test@test.com", returnLinkText = "bar"))
      )
    }

    "read the returnURL and returnLinkText if both present and alreadyOptedInUrl if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validAlreadyOptedInUrl)) should contain(
        Right(HostContext(returnUrl = "test@test.com", returnLinkText = "bar", alreadyOptedInUrl = Some("AnotherUrl")))
      )
    }

    "read the returnURL and returnLinkText if both present and termsAndConditions if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validGenericTermsAndConditions)) should contain(
        Right(HostContext(returnUrl = "test@test.com", returnLinkText = "bar", termsAndConditions = Some("generic")))
      )
    }

    "read the survey to false if not present " in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText)) should contain(
        Right(HostContext(returnUrl = "test@test.com", returnLinkText = "bar", survey = false))
      )
    }

    "read the survey to true if present " in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, surveyYes)) should contain(
        Right(HostContext(returnUrl = "test@test.com", returnLinkText = "bar", survey = true))
      )
    }

    "fail if the returnURL is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnLinkText)) should be(
        Some(Left("No returnUrl query parameter"))
      )
    }

    "fail if the returnLinkText is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl)) should be(
        Some(Left("No returnLinkText query parameter"))
      )
    }

    "read the language if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validWelshLanguage)) should contain(
        Right(HostContext(returnUrl = "test@test.com", returnLinkText = "bar"))
      )
    }
    "read the cohort if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validCohortType52)) should contain(
        Right(HostContext(returnUrl = "test@test.com", returnLinkText = "bar", cohort = Some(ReOptInPage52)))
      )
    }

    "read the entityId if present " in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, entityId)) should contain(
        Right(
          HostContext(
            returnUrl = "test@test.com",
            returnLinkText = "bar",
            entityId = Some("450262a0-1842-4885-8fa1-6fbc2aeb867d")
          )
        )
      )
    }
    "read the regime if present " in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, regime)) should contain(
        Right(
          HostContext(
            returnUrl = "test@test.com",
            returnLinkText = "bar",
            regime = Some("itsa")
          )
        )
      )
    }
  }

  "Unbinding a host context" should {
    "write out all parameters when headers = Blank" in {
      model.HostContext.hostContextBinder
        .unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar")) should be(
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D"
      )
    }
    "write out all parameters when pageType if ReOptInPage" in {
      model.HostContext.hostContextBinder
        .unbind(
          "anyValName",
          HostContext(
            returnUrl = "foo&value",
            returnLinkText = "bar",
            cohort = Some(ReOptInPage10),
            email = Some("foo@bar.com")
          )
        ) should be(
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&email=yCVwXTaKNqm1whFZ7gcFkQ%3D%3D&cohort=u%2Fn1h8qcsJrhpRofXkhmXg%3D%3D"
      )
    }
    "write out all parameters when survey is present " in {
      model.HostContext.hostContextBinder
        .unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar", survey = true)) should be(
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D"
      )
    }
    "write out all parameters when entityId is present " in {
      model.HostContext.hostContextBinder
        .unbind(
          "anyValName",
          HostContext(
            returnUrl = "foo&value",
            returnLinkText = "bar",
            survey = true,
            entityId = Some("450262a0-1842-4885-8fa1-6fbc2aeb867d")
          )
        ) should be(
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D&entityId=2N2GyqTh0egUZazS9deq4A06NP6q%2BH4Ozpi%2BUNqIBs1nTZPr1T4AqZU67fpoYnvK"
      )
    }

    "write out all parameters when regime is present " in {
      model.HostContext.hostContextBinder
        .unbind(
          "anyValName",
          HostContext(
            returnUrl = "/paperless/optin",
            returnLinkText = "bar",
            survey = true,
            regime = Some("itsa")
          )
        ) should be(
        "returnUrl=AvoA%2Bb2J%2FW7iTSpzkruMsFWAccrhVkAUGQBqXubUoJs%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&survey=hrcOMaf19lUfbNYcQ9B7mA%3D%3D&regime=KucfrgeglpOjHad59vo1xg%3D%3D"
      )
    }

  }
}
