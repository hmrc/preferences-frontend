/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package stubs

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import play.api.libs.json.Json.JsValueWrapper
import uk.gov.hmrc.domain.{ Nino, SaUtr }
import utils.FileLoader

import java.time.Instant
import java.time.temporal.ChronoUnit

trait WireMockStubs {
  self: WireMockUtil =>

  def stubWithResponse(
    stubMapping: String => StubMapping,
    expectedResponseFile: String,
    properties: Map[String, String] = Map()
  ): StubMapping =
    stubMapping(FileLoader.readAndSubstitute(expectedResponseFile, properties))

  def stubForPreferencesStatus(majorVersion: Int = 3): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": false,
                |            "majorVersion": $majorVersion,
                |            "paperless": false
                |        }
                |    },
                |    "status" : {
                |      "name": "NEW_CUSTOMER",
                |      "category": "ACTION_REQUIRED",
                |      "majorVersion": $majorVersion
                |    },
                |    "updatedAt": "2025-07-01",
                |    "digital": false
                |}
                |""".stripMargin)
        )
    )

  def stubForPreferencesStatusOk(majorVersion: Int = 3): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": true
                |        }
                |    },
                |    "status" : {
                |      "name": "NEW_CUSTOMER",
                |      "category": "ACTION_REQUIRED"
                |    },
                |    "updatedAt": "2025-07-01",
                |    "digital": false
                |}
                |""".stripMargin)
        )
    )

  def stubForPreferencesStatusOkVerified(majorVersion: Int = 3): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": true
                |        }
                |    },
                |    "status"    : {
                |      "name"    : "NEW_CUSTOMER",
                |      "category": "ACTION_REQUIRED"
                |    },
                |    "email"     : {
                |      "email"     : "test@test.com",
                |      "isVerified": true,
                |      "hasBounces": false,
                |      "mailboxFull": false
                |    },
                |    "updatedAt" : "2025-07-01",
                |    "digital"   : false
                |}
                |""".stripMargin)
        )
    )

  def stubForPreferencesStatusOkOptedOut(majorVersion: Int = 3): StubMapping = {
    val instantAsString = Instant.now().toString
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": false,
                |            "updatedAt" : "$instantAsString"
                |        }
                |    },
                |    "updatedAt" : "2025-07-01T09:00:00.000Z",
                |    "digital"   : false
                |}
                |""".stripMargin)
        )
    )
  }

  def stubForPreferencesStatusOkOptedOutWithSurvey(majorVersion: Int = 3): StubMapping = {
    val instantAsString = Instant.now().minus(2, ChronoUnit.HOURS).toString
    val completedAtEpochMillis = Instant.now().minus(2, ChronoUnit.HOURS).toEpochMilli

    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": false,
                |            "updatedAt" : "$instantAsString"
                |        }
                |    },
                |    "surveys": [
                |        {
                |            "surveyType": "StandardInterruptOptOut",
                |            "completedAt": {
                |                "$$date": $completedAtEpochMillis
                |            }
                |        }
                |    ],
                |    "updatedAt" : "2025-07-01T09:00:00.000Z",
                |    "digital" : false
                |}
                |""".stripMargin)
        )
    )
  }

  def stubForPreferencesStatusOkVerifiedWithLanguage(language: String, majorVersion: Int = 3): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": true
                |        }
                |    },
                |    "status"    : {
                |      "name"    : "NEW_CUSTOMER",
                |      "category": "ACTION_REQUIRED"
                |    },
                |    "email"     : {
                |      "email"     : "test@test.com",
                |      "isVerified": true,
                |      "hasBounces": false,
                |      "mailboxFull": false,
                |      "language"  : "$language"
                |    },
                |    "updatedAt" : "2025-07-01",
                |    "digital"   : false
                |}
                |""".stripMargin)
        )
    )

  def stubForPreferencesStatusOkOptedOutAfterVerification(majorVersion: Int = 3): StubMapping = {
    val pastTime = Instant.now().minus(2, ChronoUnit.HOURS).toString
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": false,
                |            "updatedAt" : "$pastTime"
                |        }
                |    },
                |    "status" : {
                |      "name": "NEW_CUSTOMER",
                |      "category": "ACTION_REQUIRED"
                |    },
                |    "updatedAt" : "2025-07-01T09:00:00.000Z",
                |    "digital"   : false
                |}
                |""".stripMargin)
        )
    )
  }

  def stubForPreferencesStatusOkOptedOutAfterVerificationWithinGracePeriod(majorVersion: Int = 3): StubMapping = {
    val recentTime = Instant.now().minus(5, ChronoUnit.MINUTES).toString
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": false,
                |            "updatedAt" : "$recentTime"
                |        }
                |    },
                |    "email"     : {
                |      "email"     : "test@test.com",
                |      "isVerified": true,
                |      "hasBounces": false,
                |      "mailboxFull": false
                |    },
                |    "updatedAt" : "2025-07-01T09:00:00.000Z",
                |    "digital"   : false
                |}
                |""".stripMargin)
        )
    )
  }

  def stubForPreferencesStatusOkOptedOutUnverifiedWithinGracePeriod(majorVersion: Int = 3): StubMapping = {
    val recentTime = Instant.now().minus(5, ChronoUnit.MINUTES).toString
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {
                |        "generic": {
                |            "accepted": false,
                |            "updatedAt" : "$recentTime"
                |        }
                |    },
                |    "email"     : {
                |      "email"     : "test@test.com",
                |      "isVerified": false,
                |      "hasBounces": false,
                |      "mailboxFull": false
                |    },
                |    "updatedAt" : "2025-07-01T09:00:00.000Z",
                |    "digital"   : false
                |}
                |""".stripMargin)
        )
    )
  }

  def stubForPreferencesNotFound: StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(notFound())
    )

  def stubForPreferencesStatusNotFound(majorVersion: Int = 3): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences"))
        .willReturn(
          ok(s"""{
                |    "termsAndConditions": {},
                |    "email" : {
                |      "email"      : "test@test.com",
                |      "isVerified" : false,
                |      "hasBounces" : false,
                |      "mailboxFull": false,
                |      "status": "EMAIL_NOT_VERIFIED"
                |    },
                |    "digital" : false
                |}
                |""".stripMargin)
        )
    )

  def stubForPreferencesWithResponse(response: String): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo("/preferences")).willReturn(ok(response))
    )

  def stubForPreferencesEmailLanguage: StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo("/preferences/email-language"))
        .willReturn(ok())
    )

  def stubForPreferencesEmailLanguageUpdate(language: String): StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo("/preferences/email-language"))
        .withRequestBody(containing(s""""language":"$language""""))
        .willReturn(ok())
    )

  def stubForPreferencesPendingEmail: StubMapping =
    wireMockServer.stubFor(
      put(urlEqualTo("/preferences/pending-email"))
        .willReturn(ok())
    )

  def stubForPreferencesPutEmail(status: Int, verifyStatus: String): StubMapping =
    wireMockServer.stubFor(
      put(urlEqualTo("/preferences/email"))
        .willReturn(
          jsonResponse(
            s"""{
               |  "verifyStatus": "$verifyStatus",
               |  "description": "description"
               |}
               |""".stripMargin,
            status
          )
        )
    )

  def stubForOptIn =
    wireMockServer.stubFor(
      post(urlEqualTo("/preferences/optin"))
        .willReturn(ok())
    )

  // =========

  def stubForAuthorisedAndEnrolled(response: String): StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          ok(response)
        )
    )

  def stubForUnauthorised: StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          unauthorized().withHeader("WWW-Authenticate", """MDTP detail="InsufficientConfidenceLevel"""")
        )
    )

  def buildAuthStub(
    withUtr: Option[SaUtr] = None,
    withNino: Option[Nino] = None,
    affinityGroup: String = "Organisation",
    confidenceLevel: Int = 200
  ) = {
    var list = Seq[JsValueWrapper]()

    list = withUtr.fold(list)(utr => list :+ Json.obj("key" -> "UTR", "value" -> s"$utr"))
    list = withNino.fold(list)(nino => list :+ Json.obj("key" -> "NINO", "value" -> s"$nino"))

    var builder = new AuthStubResponseBuilder()
      .withAffinityGroup(affinityGroup)
      .withConfidenceLevel(confidenceLevel)
      .withEnrolments(
        Json.arr(
          Json.obj(
            "key"         -> "IR-SA",
            "identifiers" -> Json.arr(list*)
          )
        )
      )

    builder = withNino.fold(builder)(nino => builder.withNino(nino))
    builder = withUtr.fold(builder)(utr => builder.withUtr(utr))

    val response = builder.build()
    stubForAuthorisedAndEnrolled(response)
  }
}
