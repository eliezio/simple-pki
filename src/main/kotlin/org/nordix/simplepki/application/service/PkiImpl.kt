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

import org.bouncycastle.asn1.x509.CRLReason
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.nordix.simplepki.application.port.`in`.Pki
import org.nordix.simplepki.application.port.out.EndEntityRepository
import org.nordix.simplepki.common.PemConverter
import org.nordix.simplepki.common.X500NameUtil
import org.nordix.simplepki.domain.model.*
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.StringReader
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.util.*
import java.util.function.LongPredicate
import javax.inject.Provider

@Component
internal class PkiImpl(
    // Using Provider<> to allow lazily fetching the CA singleton
    private val ca: Provider<PkiEntity>,
    private val endEntityRepository: EndEntityRepository,
    private val pkiOperations: PkiOperations,
) : Pki {

    override val caCert: X509Certificate
        get() = ca.get().certificate

    override fun crlBuilder(): Pki.CrlBuilder {
        val revocations: List<RevocationEntry> = endEntityRepository.allRevocations()
        return CrlBuilderImpl(revocations)
    }

    @Throws(Exception::class)
    override fun sign(csr: PKCS10CertificationRequest): X509Certificate {
        val serialNumber = secureRandom.nextLong() and Long.MAX_VALUE
        val cert: X509Certificate = pkiOperations.signCsr(csr, serialNumber, ca.get())
        val entity = EndEntity(
            serialNumber = serialNumber,
            version = 0,
            subject = X500NameUtil.canonicalSubjectName(cert),
            notValidBefore = cert.notBefore,
            notValidAfter = cert.notAfter,
            certificate = PemConverter.toPem(cert),
        )
        endEntityRepository.save(entity)
        return cert
    }

    @Throws(CertificateException::class, IOException::class)
    override fun getCertificate(serialNumber: String): X509Certificate {
        return getCertificate(SerialNumberConverter.fromString(serialNumber))
    }

    @Throws(CertificateException::class, IOException::class)
    override fun getCertificate(serialNumber: Long): X509Certificate {
        val entity: EndEntity = endEntityRepository.findById(serialNumber)
            .orElseThrow { NoSuchElementException() }
        return PemConverter.fromPem(StringReader(entity.certificate), X509Certificate::class.java)
    }

    override fun revoke(serialNumber: String, date: Date): Boolean {
        return revoke(SerialNumberConverter.fromString(serialNumber), date)
    }

    override fun revoke(serialNumber: Long, date: Date): Boolean {
        val entity: EndEntity = endEntityRepository.findById(serialNumber)
            .orElseThrow {
                NoSuchElementException(
                    String.format(
                        "No certificate with serialNumber=%d was found",
                        serialNumber
                    )
                )
            }
        if (entity.isRevoked) {
            return false
        }
        val revokedEntity = entity.revoke(date, CRLReason.privilegeWithdrawn)
        endEntityRepository.save(revokedEntity)
        return true
    }

    /**
     * A lazy CRL builder that allows the caller to avoid the CRL actual generation in case a filter criteria is met.
     */
    private inner class CrlBuilderImpl(revocations: List<RevocationEntry>) : Pki.CrlBuilder {
        private val revocations: List<RevocationEntry>
        private val editionDate: Date
        private var skip = false

        init {
            this.revocations = revocations
            editionDate = revocations
                .mapNotNull { it.date }
                .maxOrNull()
                ?: ca.get().certificate.notBefore
        }

        /**
         * Applies the given predicate, and disable the CRL generation in case it returns true.
         *
         * @param predicate The predicate to be applied to the CRL.thisUpdateTime property.
         * @return This builder.
         */
        override fun filterByUpdateTime(predicate: LongPredicate): Pki.CrlBuilder {
            skip = predicate.test(editionDate.time)
            return this
        }

        /**
         * Eventually builds the CRL if no skipping condition was met.
         *
         * @return The optional CRL.
         * @throws Exception Thrown if the generation fails for any reason.
         */
        @Throws(Exception::class)
        override fun build(): Optional<X509CRL> {
            return if (skip) Optional.empty<X509CRL>() else Optional.of<X509CRL>(
                pkiOperations.generateCrl(
                    revocations,
                    editionDate,
                    ca.get()
                )
            )
        }
    }

    companion object {
        private val secureRandom: Random = SecureRandom()
    }
}
