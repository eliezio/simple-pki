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

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Date;

import static org.nordix.simplepki.crypto.CertificateSettings.CA_BASIC_CONSTRAINTS;
import static org.nordix.simplepki.crypto.CertificateSettings.CA_KEY_USAGES;
import static org.nordix.simplepki.crypto.CertificateSettings.NEVER_EXPIRES_DATE;
import static org.nordix.simplepki.crypto.CertificateSettings.SIGNATURE_ALGORITHM;

@Component
@RequiredArgsConstructor
public class DefaultCertificateFactory implements CertificateFactory {

    private final Clock clock;

    @Override
    public X509Certificate generateCaCert(String dn, KeyPair keyPair)
        throws Exception {
        val name = new X500Principal(dn);
        val now = Date.from(clock.instant());
        val extUtils = new JcaX509ExtensionUtils();
        val certificateBuilder = new JcaX509v3CertificateBuilder(name,
            BigInteger.ZERO,
            now, NEVER_EXPIRES_DATE,
            name, keyPair.getPublic());
        certificateBuilder
            .addExtension(Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(keyPair.getPublic()))
            .addExtension(Extension.basicConstraints, true, CA_BASIC_CONSTRAINTS)
            .addExtension(Extension.keyUsage, true, CA_KEY_USAGES);

        val signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
            .getCertificate(certificateBuilder.build(signer));
    }
}
