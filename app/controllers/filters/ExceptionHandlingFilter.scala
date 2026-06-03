/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.filters

import org.apache.pekko.stream.Materializer
import model.Encrypted
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ExceptionHandlingFilter @Inject() (
  val mat: Materializer
)(implicit ec: ExecutionContext)
    extends Filter with Results {

  val logger: Logger = Logger(this.getClass)

  override def apply(action: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    action(rh) recoverWith {
      case unhealthyService: UnhealthyServiceException => Future.failed(unhealthyService)
      case e =>
        val urlBinder = implicitly[QueryStringBindable[Encrypted[String]]]
        urlBinder.bind("returnUrl", rh.queryString) match {
          case Some(Right(encryptedUrl)) =>
            logger.logger
              .error(s"An error occurred when calling entity-resolver, redirecting to returnUrl. ${e.getMessage}")
            Future.successful(Results.Redirect(encryptedUrl.decryptedValue))
          case _ => Future.failed(e)
        }
    }
}
