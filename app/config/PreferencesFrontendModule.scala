/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package config

import com.google.inject.{ AbstractModule, Provides }
import com.google.inject.name.Names.named
import com.typesafe.config.ConfigException
import play.api.{ Configuration, Environment }

import javax.inject.{ Named, Singleton }
import scala.annotation.unused

class PreferencesFrontendModule(@unused env: Environment, configuration: Configuration) extends AbstractModule {

  protected def bindString(path: String, name: String): Unit =
    bindConstant()
      .annotatedWith(named(resolveAnnotationName(path, name)))
      .to(configuration.getOptional[String](path).getOrElse(configException(path)))

  override def configure(): Unit =
    bindString(s"CPFUrl", "CPFUrl")

  private def resolveAnnotationName(path: String, name: String): String =
    name match {
      case "" => path
      case _  => name
    }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Throw"))
  private def configException(path: String) = throw new ConfigException.Missing(path)

}
