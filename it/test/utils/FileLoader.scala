/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package utils

import scala.io.Source
import scala.util.Using

object FileLoader {
  def read(fileName: String): String =
    Using(Source.fromURL(getClass.getResource("/" + fileName))) { source =>
      source.mkString
    }.get

  private def substitute(variables: Map[String, String]): String => String = contents =>
    variables.foldLeft(contents) { case (content, (key, value)) =>
      content.replace(s"{{$key}}", value)
    }

  val readAndSubstitute: (String, Map[String, String]) => String =
    (fileName, variables) => (read andThen substitute(variables))(fileName)
}
