/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package model

import utils.TestData.*

import model.JourneyTypeDC.*
import play.api.libs.json.{ JsResultException, Json }
import utils.SpecBase
import JourneyTypeDC.*

class PreCheckStatusSpec extends SpecBase {

  "silentRedirectFormat" should {
    import JourneyDC.silentRedirectFormat

    "read the json correctly" in new Setup {
      Json.parse(silentRedirectJourneyJsonString1()).as[SilentRedirectJourney] mustBe silentRedirectJourney()
      Json
        .parse(silentRedirectJourneyJsonString1(Conflict.entryName))
        .as[SilentRedirectJourney] mustBe silentRedirectJourney(
        Conflict
      )

      Json
        .parse(silentRedirectJourneyJsonString1(OptIn.entryName))
        .as[SilentRedirectJourney] mustBe silentRedirectJourney(
        OptIn
      )

      Json
        .parse(silentRedirectJourneyJsonString1(EmailVerification.entryName))
        .as[SilentRedirectJourney] mustBe silentRedirectJourney(EmailVerification)

      Json
        .parse(silentRedirectJourneyJsonString1(BounceEmail.entryName))
        .as[SilentRedirectJourney] mustBe silentRedirectJourney(BounceEmail)

      Json
        .parse(silentRedirectJourneyJsonString1(ReOptIn.entryName))
        .as[SilentRedirectJourney] mustBe silentRedirectJourney(
        ReOptIn
      )

      Json
        .parse(silentRedirectJourneyJsonString1(ReOptInModified.entryName))
        .as[SilentRedirectJourney] mustBe silentRedirectJourney(ReOptInModified)
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(silentRedirectJourneyInvalidJsonString).as[SilentRedirectJourney]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(silentRedirectJourney()) mustBe Json.parse(silentRedirectJourneyJsonString2)
      Json.toJson(silentRedirectJourney())(JourneyDC.silentRedirectWrites) mustBe Json.parse(
        silentRedirectJourneyJsonString2
      )
    }
  }

  "conflictFormat" should {
    import JourneyDC.conflictFormat

    "read the json correctly" in new Setup {
      Json.parse(conflictJourneyJsonString()).as[ConflictJourney] mustBe conflictJourney()
      Json.parse(conflictJourneyJsonString(SilentRedirect.entryName)).as[ConflictJourney] mustBe conflictJourney(
        SilentRedirect
      )

      Json.parse(conflictJourneyJsonString(OptIn.entryName)).as[ConflictJourney] mustBe conflictJourney(OptIn)
      Json.parse(conflictJourneyJsonString(EmailVerification.entryName)).as[ConflictJourney] mustBe conflictJourney(
        EmailVerification
      )

      Json.parse(conflictJourneyJsonString(BounceEmail.entryName)).as[ConflictJourney] mustBe conflictJourney(
        BounceEmail
      )

      Json.parse(conflictJourneyJsonString(ReOptIn.entryName)).as[ConflictJourney] mustBe conflictJourney(ReOptIn)
      Json.parse(conflictJourneyJsonString(ReOptInModified.entryName)).as[ConflictJourney] mustBe conflictJourney(
        ReOptInModified
      )
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(conflictJourneyInvalidJsonString).as[ConflictJourney]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(conflictJourney()) mustBe Json.parse(conflictJourneyJsonString())
      Json.toJson(conflictJourney(SilentRedirect)) mustBe Json.parse(
        conflictJourneyJsonString(SilentRedirect.entryName)
      )

      Json.toJson(conflictJourney(OptIn)) mustBe Json.parse(conflictJourneyJsonString(OptIn.entryName))
      Json.toJson(conflictJourney(EmailVerification)) mustBe Json.parse(
        conflictJourneyJsonString(EmailVerification.entryName)
      )

      Json.toJson(conflictJourney(BounceEmail)) mustBe Json.parse(conflictJourneyJsonString(BounceEmail.entryName))
      Json.toJson(conflictJourney(ReOptIn)) mustBe Json.parse(conflictJourneyJsonString(ReOptIn.entryName))

      Json.toJson(conflictJourney(ReOptInModified)) mustBe Json.parse(
        conflictJourneyJsonString(ReOptInModified.entryName)
      )

      Json.toJson(conflictJourney())(JourneyDC.conflictWrites) mustBe Json.parse(conflictJourneyJsonString())
    }
  }

  "optInFormat" should {
    import JourneyDC.optInFormat

    "read the json correctly" in new Setup {
      Json.parse(optInJourneyJsonString).as[OptInJourney] mustBe optInJourney
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(optInJourneyInvalidJsonString).as[OptInJourney]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(optInJourney) mustBe Json.parse(optInJourneyJsonString)
      Json.toJson(optInJourney)(JourneyDC.optIntWrites) mustBe Json.parse(optInJourneyJsonString)
    }
  }

  "emailVerificationFormat" should {
    import JourneyDC.emailVerificationFormat

    "read the json correctly" in new Setup {
      Json.parse(emailVerificationJourneyJsonString).as[EmailVerificationJourney] mustBe emailVerificationJourney
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(emailVerificationJourneyInvalidJsonString).as[EmailVerificationJourney]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(emailVerificationJourney) mustBe Json.parse(emailVerificationJourneyJsonString)
      Json.toJson(emailVerificationJourney)(JourneyDC.emailVerificationWrites) mustBe Json.parse(
        emailVerificationJourneyJsonString
      )
    }
  }

  "bounceEmailFormat" should {
    import JourneyDC.bounceEmailFormat

    "read the json correctly" in new Setup {
      Json.parse(bounceEmailJourneyJsonString).as[BounceEmailJourney] mustBe bounceEmailJourney
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(bounceEmailJourneyInvalidJsonString).as[BounceEmailJourney]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(bounceEmailJourney) mustBe Json.parse(bounceEmailJourneyJsonString)
      Json.toJson(bounceEmailJourney)(JourneyDC.bounceEmailWrites) mustBe Json.parse(bounceEmailJourneyJsonString)
    }
  }

  "reOptInModifiedFormat" should {
    import JourneyDC.reOptInModifiedFormat

    "read the json correctly" in new Setup {
      Json.parse(reOptInModifiedJourneyJsonString).as[ReOptInModifiedJourney] mustBe reOptInModifiedJourney
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(reOptInModifiedJourneyInvalidJsonString).as[ReOptInModifiedJourney]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(reOptInModifiedJourney) mustBe Json.parse(reOptInModifiedJourneyJsonString)
      Json.toJson(reOptInModifiedJourney)(JourneyDC.reOptInModifiedWrites) mustBe Json.parse(
        reOptInModifiedJourneyJsonString
      )
    }
  }

  "reOptInFormat" should {
    import JourneyDC.reOptInFormat

    "read the json correctly" in new Setup {
      Json.parse(reOptInJourneyJsonString).as[ReOptInJourney] mustBe reOptInJourney
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(reOptInJourneyInvalidJsonString).as[ReOptInJourney]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(reOptInJourney) mustBe Json.parse(reOptInJourneyJsonString)
      Json.toJson(reOptInJourney)(JourneyDC.reOptInWrites) mustBe Json.parse(reOptInJourneyJsonString)
    }
  }

  "formats" should {
    import JourneyDC.formats

    "read the json correctly" in new Setup {
      Json.parse(journeyDCJsonString).as[JourneyDC] mustBe journeyDC
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(journeyDCInvalidJsonString).as[JourneyDC]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(journeyDC) mustBe Json.parse(journeyDCJsonString)
    }
  }

  trait Setup {
    def silentRedirectJourney(journeyType: JourneyTypeDC = SilentRedirect): SilentRedirectJourney =
      SilentRedirectJourney(entityId = TEST_ENTITY_ID, reason = TEST_REASON, journeyType = journeyType)

    def conflictJourney(journeyType: JourneyTypeDC = Conflict): ConflictJourney =
      ConflictJourney(reason = TEST_REASON, journeyType = journeyType)

    val optInJourney: OptInJourney = OptInJourney(reason = TEST_REASON)

    val emailVerificationJourney: EmailVerificationJourney =
      EmailVerificationJourney(reason = TEST_REASON, email = TEST_EMAIL_VALUE)

    val bounceEmailJourney: BounceEmailJourney = BounceEmailJourney(reason = TEST_REASON, email = TEST_EMAIL_VALUE)

    val reOptInModifiedJourney: ReOptInModifiedJourney =
      ReOptInModifiedJourney(reason = TEST_REASON, email = Some(TEST_EMAIL_PREFERENCE))

    val reOptInJourney: ReOptInJourney = ReOptInJourney(reason = TEST_REASON, email = Some(TEST_EMAIL_PREFERENCE))

    val journeyDC: JourneyDC =
      SilentRedirectJourney(entityId = TEST_ENTITY_ID, reason = TEST_REASON, journeyType = SilentRedirect)

    def silentRedirectJourneyJsonString1(
      journeyType: String = "SILENT_REDIRECT",
      _type: String = "SilentRedirectJourney"
    ): String =
      s"""{
         |"entityId":"test_entity_id",
         |"reason":"test_reason",
         |"journeyType":"$journeyType",
         |"_type":"model.$_type"
         |}""".stripMargin

    val silentRedirectJourneyJsonString2: String =
      """{
        |"entityId":"test_entity_id",
        |"reason":"test_reason",
        |"journeyType":"SILENT_REDIRECT"
        |}""".stripMargin

    val silentRedirectJourneyInvalidJsonString: String =
      """{
        |"reason":"test_reason",
        |"journeyType":"SILENT_REDIRECT",
        |"_type":"model.SilentRedirectJourney"
        |}""".stripMargin

    def conflictJourneyJsonString(journeyType: String = "CONFLICT"): String =
      s"""{"reason":"test_reason","journeyType":"$journeyType"}""".stripMargin

    val conflictJourneyInvalidJsonString: String = """{"journeyType":"CONFLICT"}""".stripMargin

    val optInJourneyJsonString: String = """{"reason":"test_reason","journeyType":"OPT_IN"}""".stripMargin
    val optInJourneyInvalidJsonString: String = """{"journeyType":"OPT_IN"}""".stripMargin

    val emailVerificationJourneyJsonString: String =
      """{"reason":"test_reason","email":"test@test.com","journeyType":"EMAIL_VERIFICATION"}""".stripMargin

    val emailVerificationJourneyInvalidJsonString: String =
      """{"email":"test@test.com","journeyType":"EMAIL_VERIFICATION"}""".stripMargin

    val bounceEmailJourneyJsonString: String =
      """{"reason":"test_reason","email":"test@test.com","journeyType":"BOUNCE_EMAIL"}""".stripMargin

    val bounceEmailJourneyInvalidJsonString: String =
      """{"email":"test@test.com","journeyType":"BOUNCE_EMAIL"}""".stripMargin

    val reOptInModifiedJourneyJsonString: String =
      """{
        |"reason":"test_reason",
        |"email":{
        |"email":"test@test.com",
        |"isVerified":true,
        |"hasBounces":false,
        |"mailboxFull":false,
        |"linkSent":"2026-02-22",
        |"language":"en"
        |},
        |"journeyType":"RE_OPT_IN_MODIFIED"
        |}""".stripMargin

    val reOptInModifiedJourneyInvalidJsonString: String =
      """{
        |"email":{
        |"email":"test@test.com",
        |"isVerified":true,
        |"hasBounces":false,
        |"mailboxFull":false,
        |"linkSent":"2026-02-22",
        |"language":"en"
        |},
        |"journeyType":"RE_OPT_IN_MODIFIED"
        |}""".stripMargin

    val reOptInJourneyJsonString: String =
      """{
        |"reason":"test_reason",
        |"email":{
        |"email":"test@test.com",
        |"isVerified":true,
        |"hasBounces":false,
        |"mailboxFull":false,
        |"linkSent":"2026-02-22",
        |"language":"en"
        |},
        |"journeyType":"RE_OPT_IN"
        |}""".stripMargin

    val reOptInJourneyInvalidJsonString: String =
      """{
        |"email":{
        |"email":"test@test.com",
        |"isVerified":true,
        |"hasBounces":false,
        |"mailboxFull":false,
        |"linkSent":"2026-02-22",
        |"language":"en"
        |},
        |"journeyType":"RE_OPT_IN"
        |}""".stripMargin

    val journeyDCJsonString: String =
      """{
        |"entityId":"test_entity_id",
        |"reason":"test_reason",
        |"journeyType":"SILENT_REDIRECT",
        |"_type":"model.SilentRedirectJourney"
        |}""".stripMargin

    val journeyDCInvalidJsonString: String =
      """{
        |"reason":"test_reason",
        |"journeyType":"SILENT_REDIRECT",
        |"_type":"model.SilentRedirectJourney"
        |}""".stripMargin
  }
}
