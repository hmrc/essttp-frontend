/*
 * Copyright 2022 HM Revenue & Customs
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

package crypto

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText}

import java.nio.charset.Charset

/**
 * This service is purely so we can inject an encrypter with decrypter that results in plain text
 * (we don't want to send a SensitiveString encrypted value to ttp)
 */
@Singleton
class NoOpCrypto @Inject() extends Encrypter with Decrypter {

  override def encrypt(plain: PlainContent): Crypted = Crypted(plain.toString)

  override def decrypt(encrypted: Crypted): PlainText = PlainText(encrypted.value)

  override def decryptAsBytes(encrypted: Crypted): PlainBytes =
    PlainBytes(encrypted.value.getBytes(Charset.forName("UTF-8")))
}
