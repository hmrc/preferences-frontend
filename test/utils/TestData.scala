/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package utils

/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

import connectors.EmailPreference
import model.Language
import model.Language.English

import java.time.{ Instant, LocalDate }

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

object TestData {
  val EMPTY_STRING = ""

  val TEST_ENTITY_ID = "test_entity_id"
  val TEST_REASON = "test_reason"
  val TEST_EMAIL_VALUE = "test@test.com"
  val TEST_DESCRIPTION = "test_desc"
  val TEST_LINK_TEXT = "test_link"
  val TEST_MSG = "test_msg"
  val TEST_URL = "http://localhost:9088/test"
  val TEST_STATUS = "pending"
  val TEST_KEY = "test_key"
  val TEST_LANG_VALUE: String = English.entryName

  val TEST_JOURNEY_VALUE = "OPT_IN"
  val TEST_TOKEN = "14578hggdss908"
  val TEST_UTR = "UTR-456"
  val TEST_NINO = "NA000914D"

  val TEST_DAY = 22
  val TEST_MONTH = 2
  val TEST_YEAR_2026 = 2026
  val TEST_LOCAL_DATE: LocalDate = LocalDate.of(TEST_YEAR_2026, TEST_MONTH, TEST_DAY)

  val TEST_EPOCH_MILLI_SECONDS = 3467288L
  val TEST_TIME_INSTANT: Instant = Instant.ofEpochMilli(TEST_EPOCH_MILLI_SECONDS)

  val TEST_EMAIL_PREFERENCE: EmailPreference = EmailPreference(
    email = TEST_EMAIL_VALUE,
    isVerified = true,
    hasBounces = false,
    mailboxFull = false,
    linkSent = Some(TEST_LOCAL_DATE),
    language = Some(English)
  )
}
