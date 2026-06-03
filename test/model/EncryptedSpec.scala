/*
 * Copyright 2026 HM Revenue & Customs
 *
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
