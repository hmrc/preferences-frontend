/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package utils

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer

trait TestActorSystem {
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: Materializer = Materializer(system)
}
