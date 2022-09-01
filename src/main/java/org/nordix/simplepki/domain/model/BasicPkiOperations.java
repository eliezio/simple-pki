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
package org.nordix.simplepki.domain.model;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;

import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Date;
import java.util.List;

import static org.bouncycastle.cert.jcajce.JcaX500NameUtil.getSubject;
import static org.nordix.simplepki.domain.model.CertificateSettings.EXTENDED_KEY_USAGES;
import static org.nordix.simplepki.domain.model.CertificateSettings.NEVER_EXPIRES_DATE;
import static org.nordix.simplepki.domain.model.CertificateSettings.NON_CA_BASIC_CONSTRAINTS;
import static org.nordix.simplepki.domain.model.CertificateSettings.NON_CA_KEY_USAGES;
import static org.nordix.simplepki.domain.model.CertificateSettings.SIGNATURE_ALGORITHM;

@RequiredArgsConstructor
public class BasicPkiOperations implements PkiOperations {

    private final Clock clock;

    @Override
    public X509Certificate signCsr(PKCS10CertificationRequest request, long serialNumber, PkiEntity ca)
        throws Exception {
        val now = Date.from(clock.instant());
        val jcaRequest = new JcaPKCS10CertificationRequest(request);
        val extUtils = new JcaX509ExtensionUtils();
        val certificateBuilder = new JcaX509v3CertificateBuilder(getSubject(ca.getCertificate()),
            BigInteger.valueOf(serialNumber),
            now, NEVER_EXPIRES_DATE,
            jcaRequest.getSubject(), jcaRequest.getPublicKey());
        certificateBuilder
            .addExtension(Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(ca.getCertificate()))
            .addExtension(Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(jcaRequest.getPublicKey()))
            .addExtension(Extension.basicConstraints, true, NON_CA_BASIC_CONSTRAINTS)
            .addExtension(Extension.keyUsage, true, NON_CA_KEY_USAGES)
            .addExtension(Extension.extendedKeyUsage, true, EXTENDED_KEY_USAGES);

        val signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .build(ca.getPrivateKey());

        return new JcaX509CertificateConverter()
            .getCertificate(certificateBuilder.build(signer));
    }

    @Override
    public X509CRL generateCrl(List<RevocationEntry> revocations, Date editionDate, PkiEntity ca)
        throws OperatorCreationException, CRLException {
        val crlBuilder = new X509v2CRLBuilder(getSubject(ca.getCertificate()), editionDate);
        for (RevocationEntry entry : revocations) {
            crlBuilder.addCRLEntry(BigInteger.valueOf(entry.serialNumber()),
                entry.date(), entry.reason());
        }

        val contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .build(ca.getPrivateKey());

        X509CRLHolder crlHolder = crlBuilder.build(contentSigner);

        return new JcaX509CRLConverter()
            .getCRL(crlHolder);
    }
}
