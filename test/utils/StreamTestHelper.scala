/*
 * Copyright 2026 HM Revenue & Customs
 *
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
