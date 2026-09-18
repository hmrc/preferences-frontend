/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
