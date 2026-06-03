/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package config

import com.google.inject.name.Names
import com.typesafe.config.ConfigException
import com.google.inject.{ Guice, Key }
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import play.api.{ Configuration, Environment }

class PreferencesFrontendModuleSpec extends AnyWordSpec with Matchers {
  private val environment: Environment = Environment.simple()

  private class TestPreferencesFrontendModule(
    environment: Environment,
    configuration: Configuration,
    path: String,
    name: String
  ) extends PreferencesFrontendModule(environment, configuration) {

    override def configure(): Unit =
      bindString(path, name)
  }

  "PreferencesFrontendModule" should {

    "throw ConfigException.Missing when config is absent" in {
      val module = new PreferencesFrontendModule(
        environment,
        Configuration.empty
      )

      val ex = the[com.google.inject.CreationException] thrownBy {
        Guice.createInjector(module)
      }

      ex.getCause shouldBe a[ConfigException.Missing]
    }

    "use the path as the annotation name when name is empty" in {
      val module = new TestPreferencesFrontendModule(
        environment,
        Configuration("some.path" -> "some-value"),
        "some.path",
        ""
      )

      val injector = Guice.createInjector(module)

      injector.getInstance(Key.get(classOf[String], Names.named("some.path"))) shouldBe "some-value"
    }
  }
}
