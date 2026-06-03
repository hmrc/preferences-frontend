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

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import java.nio.charset.StandardCharsets
import scala.xml.NodeSeq

object StreamTestHelper extends StreamTestHelper

trait StreamTestHelper {

  def createStream(node: NodeSeq): Source[ByteString, ?] = createStream(node.mkString)

  def createStream(string: String): Source[ByteString, ?] =
    Source.single(ByteString(string, StandardCharsets.UTF_8))
}
