/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
package org.nordix.simplepki.crypto;

import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;

import java.time.Instant;
import java.util.Date;

@UtilityClass
class CertificateSettings {
    // See https://tools.ietf.org/html/rfc5280#section-4.1.2.5
    static final Date NEVER_EXPIRES_DATE = Date.from(Instant.parse("9999-12-31T23:59:59Z"));

    static final BasicConstraints CA_BASIC_CONSTRAINTS = new BasicConstraints(1);

    static final BasicConstraints NON_CA_BASIC_CONSTRAINTS = new BasicConstraints(false);

    // For a complete list of official Key Usage see https://tools.ietf.org/html/rfc5280#section-4.2.1.3
    static final KeyUsage CA_KEY_USAGES = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign);

    static final KeyUsage NON_CA_KEY_USAGES = new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment);

    // For a complete list of official Extended Key Usage see https://tools.ietf.org/html/rfc5280#section-4.2.1.12
    static final ExtendedKeyUsage EXTENDED_KEY_USAGES = new ExtendedKeyUsage(
        new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}
    );

    static final String SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";
}
