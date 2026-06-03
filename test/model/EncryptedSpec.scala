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

package model

import utils.SpecBase

class EncryptedSpec extends SpecBase {

  "EntityIdCrypto.encryptEntityId" should {

    "return None when an exception is thrown" in {
      EntityIdCrypto.encryptEntityId(null) mustBe empty
    }

    "return the encrypted value" in {
      EntityIdCrypto.encryptEntityId("test_id") mustBe Some("Z2mQvsojFg0VPLCAPTDiSA%3D%3D")
    }
  }

  "EntityIdCrypto.decryptEntityId" should {

    "return None when an exception is thrown" in {
      EntityIdCrypto.decryptEntityId("===") mustBe empty
    }

    "return the decrypted value" in {
      EntityIdCrypto.decryptEntityId("Z2mQvsojFg0VPLCAPTDiSA%3D%3D") mustBe Some("test_id")
    }
  }
}
