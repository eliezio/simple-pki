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

import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.cert.X509v2CRLBuilder
import org.bouncycastle.cert.jcajce.JcaX500NameUtil
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest
import org.nordix.simplepki.domain.model.PkiEntity
import org.nordix.simplepki.domain.model.PkiOperations
import org.nordix.simplepki.domain.model.RevocationEntry
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.time.Clock
import java.util.*

@Component
internal class DefaultPkiOperations(private val clock: Clock) : PkiOperations {

    override fun signCsr(csr: PKCS10CertificationRequest, serialNumber: Long, ca: PkiEntity): X509Certificate {
        val now = Date.from(clock.instant())
        val jcaRequest = JcaPKCS10CertificationRequest(csr)
        val extUtils = JcaX509ExtensionUtils()
        val certificateBuilder = JcaX509v3CertificateBuilder(
            JcaX500NameUtil.getSubject(ca.certificate),
            BigInteger.valueOf(serialNumber),
            now, CertificateSettings.NEVER_EXPIRES_DATE,
            jcaRequest.subject, jcaRequest.publicKey
        )
        certificateBuilder
            .addExtension(
                Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(ca.certificate)
            )
            .addExtension(
                Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(jcaRequest.publicKey)
            )
            .addExtension(Extension.basicConstraints, true, CertificateSettings.NON_CA_BASIC_CONSTRAINTS)
            .addExtension(Extension.keyUsage, true, CertificateSettings.NON_CA_KEY_USAGES)
            .addExtension(Extension.extendedKeyUsage, true, CertificateSettings.EXTENDED_KEY_USAGES)
        val signer = JcaContentSignerBuilder(CertificateSettings.SIGNATURE_ALGORITHM)
            .build(ca.privateKey)
        return JcaX509CertificateConverter()
            .getCertificate(certificateBuilder.build(signer))
    }

    override fun generateCrl(revocations: List<RevocationEntry>, editionDate: Date, ca: PkiEntity): X509CRL {
        val crlBuilder = X509v2CRLBuilder(JcaX500NameUtil.getSubject(ca.certificate), editionDate)
        for (entry in revocations) {
            crlBuilder.addCRLEntry(
                BigInteger.valueOf(entry.serialNumber),
                entry.date, entry.reason
            )
        }
        val contentSigner = JcaContentSignerBuilder(CertificateSettings.SIGNATURE_ALGORITHM)
            .build(ca.privateKey)
        val crlHolder = crlBuilder.build(contentSigner)
        return JcaX509CRLConverter()
            .getCRL(crlHolder)
    }
}
