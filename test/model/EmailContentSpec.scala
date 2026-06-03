/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package model

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsNull, JsResultException, JsValue, Json }

class EmailContentSpec extends PlaySpec {

  "EmailAddress2" should {
    "serialize to JSON correctly" in {
      val emailAddress = EmailAddress2("test@example.com")
      val json = Json.toJson(emailAddress)

      (json \ "value").as[String] mustBe "test@example.com"
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj("value" -> "test@example.com")
      val emailAddress = json.as[EmailAddress2]

      emailAddress.value mustBe "test@example.com"
    }

    "throw exception for invalid json" in {
      import EmailAddress2.format
      val invalidJson = """{}""".stripMargin

      intercept[JsResultException] {
        Json.parse(invalidJson).as[EmailAddress2]
      }
    }
  }

  "To" should {
    "serialize to JSON correctly" in {
      val to = To(List("user1@example.com", "user2@example.com"), "correlationId")
      val json = Json.toJson(to)

      (json \ "email").as[List[String]] mustBe List("user1@example.com", "user2@example.com")
      (json \ "correlationId").as[String] mustBe "correlationId"
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "email"         -> Json.arr("test@example.com"),
        "correlationId" -> "correlationId"
      )
      val to = json.as[To]

      to.email mustBe List("test@example.com")
      to.correlationId mustBe "correlationId"
    }

    "deserialize with multiple email addresses" in {
      val json = Json.obj(
        "email"         -> Json.arr("first@test.com", "second@test.com", "third@test.com"),
        "correlationId" -> "correlationId"
      )
      val to = json.as[To]

      to.email must have length 3
      to.email mustBe List("first@test.com", "second@test.com", "third@test.com")
    }

    "throw exception for invalid json" in {
      val invalidJson = """{"correlationId":"correlationId"}""".stripMargin

      intercept[JsResultException] {
        Json.parse(invalidJson).as[To]
      }
    }
  }

  "Content" should {
    "serialize to JSON correctly" in {
      val replyTo = Some(EmailAddress2("test@example.com"))
      val content = Content(
        `type` = "text/html",
        subject = "Some Subject",
        replyTo = replyTo,
        text = "some text",
        html = "<h1>HTML content</h1>"
      )
      val json = Json.toJson(content)

      (json \ "type").as[String] mustBe "text/html"
      (json \ "subject").as[String] mustBe "Some Subject"
      (json \ "replyTo" \ "value").as[String] mustBe "test@example.com"
      (json \ "text").as[String] mustBe "some text"
      (json \ "html").as[String] mustBe "<h1>HTML content</h1>"
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "type"    -> "text/plain",
        "subject" -> "Some Subject",
        "replyTo" -> Json.obj("value" -> "test@example.com"),
        "text"    -> "Sample text",
        "html"    -> "<p>HTML version</p>"
      )
      val content = json.as[Content]

      content.`type` mustBe "text/plain"
      content.subject mustBe "Some Subject"
      content.replyTo mustBe Some(EmailAddress2("test@example.com"))
      content.text mustBe "Sample text"
      content.html mustBe "<p>HTML version</p>"
    }

    "throw exception for invalid json" in {
      import Content.format

      val invalidJson =
        """{
          |"subject":"Some Subject",
          |"text":"Sample text",
          |"html":"<p>HTML version</p>"
          |}""".stripMargin

      intercept[JsResultException] {
        Json.parse(invalidJson).as[Content]
      }
    }
  }

  "Options" should {
    "serialize to JSON correctly" in {
      val options = Options(trackClicks = true, trackOpens = false, fromName = "Test Sender")
      val json = Json.toJson(options)

      (json \ "trackClicks").as[Boolean] mustBe true
      (json \ "trackOpens").as[Boolean] mustBe false
      (json \ "fromName").as[String] mustBe "Test Sender"
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "trackClicks" -> true,
        "trackOpens"  -> true,
        "fromName"    -> "Sender Name"
      )
      val options = json.as[Options]

      options.trackClicks mustBe true
      options.trackOpens mustBe true
      options.fromName mustBe "Sender Name"
    }

    "throw exception for invalid json" in {
      import Options.format

      intercept[JsResultException] {
        Json.parse("""{"trackClicks":true}""".stripMargin).as[Options]
      }
    }
  }

  "ContactPolicy" should {
    "serialize to JSON correctly" in {
      val policy = ContactPolicy(
        contactPolicyGroup = "some-group",
        channelCheckConsent = true,
        channelApplyFrequencyCap = false
      )
      val json = Json.toJson(policy)

      (json \ "contactPolicyGroup").as[String] mustBe "some-group"
      (json \ "channelCheckConsent").as[Boolean] mustBe true
      (json \ "channelApplyFrequencyCap").as[Boolean] mustBe false
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "contactPolicyGroup"       -> "some-group",
        "channelCheckConsent"      -> true,
        "channelApplyFrequencyCap" -> true
      )
      val policy = json.as[ContactPolicy]

      policy.contactPolicyGroup mustBe "some-group"
      policy.channelCheckConsent mustBe true
      policy.channelApplyFrequencyCap mustBe true
    }

    "deserialize with both flags enabled" in {
      val json = Json.obj(
        "contactPolicyGroup"       -> "some-group",
        "channelCheckConsent"      -> true,
        "channelApplyFrequencyCap" -> true
      )
      val policy = json.as[ContactPolicy]

      policy.channelCheckConsent mustBe true
      policy.channelApplyFrequencyCap mustBe true
    }

    "round-trip JSON correctly" in {
      val original = ContactPolicy("some-policy", channelCheckConsent = false, channelApplyFrequencyCap = true)
      val json = Json.toJson(original)
      val deserialized = json.as[ContactPolicy]

      deserialized mustBe original
    }

    "throw exception for invalid json" in {
      val invalidJson = """{"channelCheckConsent":true,"channelApplyFrequencyCap":true}""".stripMargin

      intercept[JsResultException] {
        Json.parse(invalidJson).as[ContactPolicy]
      }
    }
  }

  "EmailContent" should {
    "serialize to JSON correctly with complete data" in {
      val emailContent = EmailContent(
        channel = "email",
        from = "sender@example.com",
        to = List(
          To(List("recipient1@example.com"), "corr-001"),
          To(List("recipient2@example.com"), "corr-002")
        ),
        tags = Some(Map("tag1" -> "value1", "tag2" -> "value2")),
        options = Options(trackClicks = true, trackOpens = true, fromName = "Test Org"),
        contactPolicy = ContactPolicy("policy-789", channelCheckConsent = true, channelApplyFrequencyCap = false),
        requestedReceipts = Seq("read", "delivery"),
        content = Content(
          `type` = "text/html",
          subject = "Welcome",
          replyTo = Some(EmailAddress2("reply@example.com")),
          text = "Welcome message",
          html = "<h1>Welcome</h1>"
        ),
        notifyUrl = "https://example.com/notify"
      )

      val json = Json.toJson(emailContent)

      (json \ "channel").as[String] mustBe "email"
      (json \ "from").as[String] mustBe "sender@example.com"
      (json \ "to").as[List[To]].length mustBe 2
      (json \ "tags" \ "tag1").as[String] mustBe "value1"
      (json \ "notifyUrl").as[String] mustBe "https://example.com/notify"
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "channel" -> "email",
        "from"    -> "noreply@domain.co.uk",
        "to" -> Json.arr(
          Json.obj(
            "email"         -> Json.arr("user@test.com"),
            "correlationId" -> "123"
          )
        ),
        "tags" -> Json.obj("campaign" -> "spring-2025"),
        "options" -> Json.obj(
          "trackClicks" -> false,
          "trackOpens"  -> true,
          "fromName"    -> "HMRC"
        ),
        "contactPolicy" -> Json.obj(
          "contactPolicyGroup"       -> "transactional",
          "channelCheckConsent"      -> false,
          "channelApplyFrequencyCap" -> false
        ),
        "requestedReceipts" -> Json.arr("delivery"),
        "content" -> Json.obj(
          "type"    -> "text/html",
          "subject" -> "Your Tax Statement",
          "replyTo" -> Json.obj("value" -> "support@hmrc.gov.uk"),
          "text"    -> "Please see your statement",
          "html"    -> "<p>Please see your statement</p>"
        ),
        "notifyUrl" -> "https://hmrc.gov.uk/notify"
      )

      val emailContent = json.as[EmailContent]

      emailContent.channel mustBe "email"
      emailContent.from mustBe "noreply@domain.co.uk"
      emailContent.to must have length 1
      emailContent.to.head.email mustBe List("user@test.com")
      emailContent.tags mustBe Some(Map("campaign" -> "spring-2025"))
      emailContent.content.subject mustBe "Your Tax Statement"
      emailContent.notifyUrl mustBe "https://hmrc.gov.uk/notify"
    }
  }

  "Channel object" should {
    "have EMAIL constant" in {
      Channel.EMAIL mustBe "email"
    }
  }
}
