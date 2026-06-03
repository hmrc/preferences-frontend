/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

import connectors.{ PreferenceResponse, PreferencesConnector }
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import org.scalatestplus.play.{ PlaySpec, WsScalaTestClient }
import play.api.libs.json.{ JsString, Json }
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{ WSClient, WSRequest }
import play.api.test.Helpers.*
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.domain.*
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto
import util.DateTimeUtils
import views.sa.prefs.helpers.DateFormat

import java.net.URLEncoder
import java.time.{ LocalDate, ZoneOffset }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }

trait TestUser {
  def userId = "SA0055"

  val password = "testing123"

  val utr = GenerateRandom.utr()
  val nino = GenerateRandom.nino()

}

class TestCase
    extends PlaySpec with TestUser with GuiceOneServerPerSuite with WsScalaTestClient with ScalaFutures
    with IntegrationPatience {

  private val itPatience: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(30, Seconds)),
    interval = scaled(Span(200, Millis))
  )

  implicit override val patienceConfig: PatienceConfig = itPatience

  val applicatinCrypto = app.injector.instanceOf[ApplicationCrypto]
  val preferencesConnector = app.injector.instanceOf[PreferencesConnector]

  val servicesConfig = app.injector.instanceOf[ServicesConfig]
  lazy val entityResolverUrl = servicesConfig.baseUrl("entity-resolver")
  val todayDate = DateFormat.longDateFormat(Some(LocalDate.ofInstant(DateTimeUtils.now, ZoneOffset.UTC))).get.body

  implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val myPublicAddress = s"localhost:$port"

  def uniqueEmail = s"${UUID.randomUUID().toString}@email.com"

  def changedUniqueEmail = uniqueEmail

  def encryptAndEncode(value: String) =
    URLEncoder.encode(applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(value)).value, "UTF-8")

  def urlWithHostContext(url: String, returnUrl: String = "", returnLinkText: String = "") =
    wsUrl(s"$url?returnUrl=${encryptAndEncode(returnUrl)}&returnLinkText=${encryptAndEncode(returnLinkText)}")

  def saPrintPreferencesAssets(file: String) = wsUrl(s"/sa/print-GetPreferences/assets/$file")

  val payeFormTypeBody = Json.parse(s"""{"active":true}""")

  case class GetPreferences(header: (String, String))(implicit ec: ExecutionContext, hc: HeaderCarrier) {
    def getPreference: Future[Option[PreferenceResponse]] =
      preferencesConnector.getPreferences()(hc.withExtraHeaders(header), ec)

    def putPendingEmail(email: String) =
      wsClient
        .url("http://localhost:8015/preferences/pending-email")
        .withHttpHeaders(header)
        .put(Json.parse(s"""{"email":"$email"}"""))
  }

  case object PortalPreferences {

    def getForUtr(utr: String) = wsUrl(s"http://localhost:8015/portal/preferences/sa/$utr").get().futureValue

    def getForNino(nino: String) = wsUrl(s"http://localhost:8015/portal/preferences/paye/$nino").get().futureValue
  }

  case class optinOptout(header: (String, String)) {
    def postGenericOptIn(pendingEmail: String) =
      wsClient
        .url(s"http://localhost:8015/preferences/optin")
        .withHttpHeaders(header)
        .post(Json.parse(s"""{
                            |  "generic": {
                            |    "accepted": true,
                            |    "optInPage":{
                            |      "version": {"major":2,"minor":1}, "cohort":1, "pageType":"IPage"}
                            |  },
                            |  "email":"$pendingEmail",
                            |  "language": "en"
                            |}""".stripMargin))

    def postGenericOptOut() =
      wsClient
        .url(s"http://localhost:8015/preferences/optout")
        .withHttpHeaders(header)
        .post(Json.parse(s"""{
                            |  "generic": {
                            |    "accepted": false,
                            |    "optInPage":{
                            |      "version": {"major":2,"minor":1}, "cohort":1, "pageType":"IPage"}
                            |  },
                            |  "language": "en"
                            |}""".stripMargin))

    def postGenericOptOutWithSurvey() =
      wsClient
        .url(s"http://localhost:8015/preferences/optout")
        .withHttpHeaders(header)
        .post(Json.parse(s"""{
                            |  "generic": {
                            |    "accepted": false,
                            |    "optInPage":{
                            |      "version": {"major":2,"minor":1}, "cohort":1, "pageType":"IPage"},
                            |    "surveyType": "StandardInterruptOptOut"
                            |  },
                            |  "language": "en"
                            |}""".stripMargin))
  }

  def entityForSaUtr(utr: String) = {
    val response = wsClient.url(s"http://localhost:8015/entity-resolver/sa/$utr").get().futureValue
    response.status must be(OK)
    (response.json \ "_id").as[String]
  }

  def entityForNino(nino: String) = {
    val response = wsClient.url(s"http://localhost:8015/entity-resolver/paye/$nino").get().futureValue
    response.status must be(OK)
    (response.json \ "_id").as[String]
  }

  case object PreferencesAdminSaIndividual {
    def verifyEmailFor(entityId: String) =
      wsClient.url(s"http://localhost:8025/preferences-admin/$entityId/verify-email").post(JsString(""))

    def postExpireVerificationLink(entityId: String) =
      wsClient
        .url(s"http://localhost:8025/preferences-admin/$entityId/expire-email-verification-link")
        .post(JsString(""))
  }

  case object PreferencesAdminBounceEmail {
    def post(emailAddress: String) =
      wsClient
        .url("http://localhost:8025/preferences-admin/bounce-email")
        .post(Json.parse(s"""{
                            |"emailAddress": "$emailAddress"
                            |}""".stripMargin))
  }

  case object PreferencesAdminSaBounceEmailInboxFull {
    def post(emailAddress: String) =
      wsClient
        .url("http://localhost:8025/preferences-admin/bounce-email")
        .post(Json.parse(s"""{
                            |"emailAddress": "$emailAddress",
                            |"code": 552
                            |}""".stripMargin))
  }

  def paperlessWarnings: WSRequest = urlWithHostContext("/paperless/warnings")

  def paperlessStatus: WSRequest = urlWithHostContext("/paperless/status")
}

trait TestCaseWithFrontEndAuthentication extends TestCase with SessionCookieEncryptionSupport {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val authHelper = app.injector.instanceOf[ItAuthHelper]
  def ggAuthHeaderWithUtr(customerType: Option[CustomerType]) = authHelper.authHeader(utr, customerType)
  def ggAuthHeaderWithNino = authHelper.authHeader(nino, None)

  def baseConfig: Map[String, Any] = Map(
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "play.http.router"                                  -> "legacy.Routes",
    "metrics.enabled"                                   -> false,
    "metrics.graphite.enabled"                          -> false,
    "auditing.enabled"                                  -> false
  )

  override lazy val app = new GuiceApplicationBuilder()
    .configure(baseConfig)
    .build()

  val returnUrl = "/test/return/url"
  val returnLinkText = "Continue"

  val encryptedReturnUrl =
    URLEncoder.encode(applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value, "UTF-8")
  val encryptedReturnText =
    URLEncoder.encode(applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "UTF-8")

  case class PaperlessActivate(taxIdentifiers: TaxIdentifier*) {

    def put(
      termsAndConditions: Option[String] = None,
      emailAddress: Option[String] = None,
      language: Option[String] = None
    ) = {

      val queryParamsMap: Map[String, Option[String]] = Map(
        "returnUrl"          -> Some(returnUrl),
        "returnLinkText"     -> Some(returnLinkText),
        "termsAndConditions" -> termsAndConditions,
        "email"              -> emailAddress
      )

      val header = authHelper.authorisedTokenFor(None, taxIdentifiers*).futureValue

      val url = wsUrl(s"/paperless/activate")
        .withSession(
          SessionKeys.authToken -> header._1
        )(language)
        .withQueryStringParameters(
          queryParamsMap.collect { case (key, Some(value)) =>
            key -> applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(value)).value
          }.toSeq*
        )

      url.put(Json.parse("""{"active":true}"""))
    }
  }
}

case class CustomerType(affinityGroup: String, confidenceLevel: Int)
