/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2022 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
package org.nordix.simplepki.application.service

import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import java.time.Instant
import java.util.*

internal object CertificateSettings {

    // See https://tools.ietf.org/html/rfc5280#section-4.1.2.5
    val NEVER_EXPIRES_DATE: Date = Date.from(Instant.parse("9999-12-31T23:59:59Z"))

    val NON_CA_BASIC_CONSTRAINTS = BasicConstraints(false)

    val NON_CA_KEY_USAGES = KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)

    // For a complete list of official Extended Key Usage see https://tools.ietf.org/html/rfc5280#section-4.2.1.12
    val EXTENDED_KEY_USAGES = ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth))

    const val SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption"
}
