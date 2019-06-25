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

import org.nordix.simplepki.persist.EndEntity;
import org.nordix.simplepki.persist.EndEntityRepository;
import lombok.AllArgsConstructor;
import lombok.val;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Provider;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.LongPredicate;

@Component
@AllArgsConstructor
public class Pki {

    // Properties
    // Using Provider<> to allow lazily fetching the CA singleton
    private final Provider<PkiEntity> ca;

    // Collaborators
    private final EndEntityRepository endEntityRepository;
    private final PkiOperations pkiOperations;

    public X509Certificate getCaCert() {
        return ca.get().getCertificate();
    }

    public CrlBuilder crlBuilder() {
        val revocations = endEntityRepository.getAllRevocations();
        return new CrlBuilder(revocations);
    }

    @Transactional(rollbackFor = Exception.class)
    public X509Certificate sign(PKCS10CertificationRequest csr)
        throws Exception {
        val entity = new EndEntity();
        endEntityRepository.save(entity);
        X509Certificate cert = pkiOperations.signCsr(csr, entity.getSerialNumber(), ca.get());
        entity.setSubject(X500NameUtil.canonicalSubjectName(cert));
        entity.setNotValidBefore(cert.getNotBefore());
        entity.setNotValidAfter(cert.getNotAfter());
        entity.setCertificate(PemConverter.toPem(cert));
        endEntityRepository.save(entity);
        return cert;
    }

    public X509Certificate getCertificate(long serialNumber)
        throws CertificateException, IOException {
        val entity = endEntityRepository.findById(serialNumber)
            .orElseThrow(NoSuchElementException::new);
        return PemConverter.fromPem(new StringReader(entity.getCertificate()), X509Certificate.class);
    }

    public boolean revoke(long serialNumber, Date date) {
        val entity = endEntityRepository.findById(serialNumber)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("No certificate with serialNumber=%d was found", serialNumber)));
        if (entity.isRevoked()) {
            return false;
        }
        entity.revoke(date, CRLReason.privilegeWithdrawn);
        endEntityRepository.save(entity);
        return true;
    }

    /**
     * A lazy CRL builder that allows the caller to avoid the CRL actual generation in case a filter criteria is met.
     */
    public class CrlBuilder {
        private final List<RevocationEntry> revocations;
        private final Date editionDate;
        private boolean skip = false;

        CrlBuilder(List<RevocationEntry> revocations) {
            this.revocations = revocations;
            this.editionDate = revocations.stream()
                .map(RevocationEntry::date)
                .max(Comparator.naturalOrder())
                .orElse(ca.get().getCertificate().getNotBefore());
        }

        /**
         * Applies the given predicate, and disable the CRL generation in case it returns true.
         *
         * @param predicate The predicate to be applied to the CRL.thisUpdateTime property.
         * @return This builder.
         */
        public CrlBuilder filterByUpdateTime(LongPredicate predicate) {
            skip = predicate.test(editionDate.getTime());
            return this;
        }

        /**
         * Eventually builds the CRL if no skipping condition was met.
         *
         * @return The optional CRL.
         * @throws Exception Thrown if the generation fails for any reason.
         */
        public Optional<X509CRL> build()
            throws Exception {
            return skip
                ? Optional.empty()
                : Optional.of(pkiOperations.generateCrl(revocations, editionDate, ca.get()));
        }
    }
}
