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
package org.nordix.simplepki.application

import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509CRLEntryHolder
import org.bouncycastle.cert.X509CRLHolder
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.crypto.CryptoServicesRegistrar
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit

import static org.nordix.simplepki.common.PemConverter.fromPem

@ContextConfiguration(classes = [Application.class])
class BaseSpecification extends Specification {

    static final Date NEVER_EXPIRE_DATE = Date.from(Instant.parse('9999-12-31T23:59:59Z'))

    def setupSpec() {
        Security.addProvider(new BouncyCastleProvider())
        CryptoServicesRegistrar.setSecureRandom(new SecureRandom())
    }

    static X509CertificateHolder decodeCert(byte[] content) {
        return new ByteArrayInputStream(content).withReader { reader ->
            return fromPem(reader, X509CertificateHolder)
        } as X509CertificateHolder
    }

    void validateCertificate(X509Certificate cert, X509Certificate caCert, Instant notBeforeMin, Instant notBeforeMax) {
        validateCertificate(new JcaX509CertificateHolder(cert), new JcaX509CertificateHolder(caCert),
            notBeforeMin, notBeforeMax)
    }

    void validateCertificate(X509CertificateHolder cert, X509CertificateHolder caCert,
                             Instant notBeforeMin, Instant notBeforeMax) {
        assert cert.getVersionNumber() == 3
        // dates on certificates are serialized as UTCTIME, thus truncated to second
        assert (cert.notBefore.toInstant() >= notBeforeMin.truncatedTo(ChronoUnit.SECONDS)) &&
                (cert.notBefore.toInstant() <= notBeforeMax)
        assert cert.notAfter == NEVER_EXPIRE_DATE
        boolean isCa = caCert == null
        if (isCa) {
            assert cert.getSerialNumber() == 0
            assert cert.getIssuer() == cert.getSubject()
        } else {
            assert cert.getSerialNumber() != 0
            assert cert.getIssuer() != cert.getSubject()
        }
        def extensions = cert.getExtensions()
        assert extensions.getExtension(Extension.subjectKeyIdentifier) != null
        // validate BasicConstraints
        def bcExt = extensions.getExtension(Extension.basicConstraints)
        assert bcExt != null
        assert bcExt.isCritical()
        def bc = BasicConstraints.getInstance(bcExt.getParsedValue())
        assert isCa == bc.CA
        if (isCa && (bc.pathLenConstraint != null)) {
            assert bc.pathLenConstraint >= 1
        }
        // validate KeyUsage
        def kuExt = extensions.getExtension(Extension.keyUsage)
        assert kuExt != null
        assert kuExt.isCritical()
        def ku = KeyUsage.getInstance(kuExt.getParsedValue())
        if (isCa) {
            assert ku.hasUsages(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        } else {
            assert ku.hasUsages(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        }
        def verifierProvider = new JcaContentVerifierProviderBuilder()
                .setProvider('BC')
                .build(caCert ?: cert)
        assert cert.isSignatureValid(verifierProvider)
    }

    static X509CRLHolder decodeCrl(byte[] content) {
        return new ByteArrayInputStream(content).withReader { reader ->
            return fromPem(reader, X509CRLHolder)
        } as X509CRLHolder
    }

    void validateCrl(X509CRLHolder crl, X509CertificateHolder caCert, List<BigInteger> expectedRevokedCerts) {
        def verifierProvider = new JcaContentVerifierProviderBuilder().setProvider('BC').build(caCert)
        assert crl.isSignatureValid(verifierProvider)
        def revokedCerts = crl.revokedCertificates.collect { (it as X509CRLEntryHolder).serialNumber }
        assert revokedCerts == expectedRevokedCerts
    }
}
