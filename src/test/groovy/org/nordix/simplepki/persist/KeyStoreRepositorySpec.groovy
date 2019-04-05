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
package org.nordix.simplepki.persist

import org.nordix.simplepki.BaseSpecification
import org.nordix.simplepki.crypto.DefaultCertificateFactory
import org.nordix.simplepki.crypto.RsaKeyPairFactory
import org.springframework.core.io.WritableResource
import spock.lang.Unroll

import java.security.KeyStore
import java.security.cert.X509Certificate
import java.time.Clock

class KeyStoreRepositorySpec extends BaseSpecification {

    @Unroll
    def 'creates new PkiEntity if #description'() {
        given: 'Prepare a memory buffer to store the new KeyStore'
            def baos = Spy(new ByteArrayOutputStream())
        and: 'Create mocks for Resource and KeyStoreConfig'
            def resourceMock = Mock(WritableResource)
            def pkiKeyStoreConfig = Mock(KeyStoreConfig) {
                getResource() >> resourceMock
                getType() >> 'pkcs12'
                getAlias() >> 'ca'
                getStorepass() >> 'some_storepass'
                getKeypass() >> 'some_keypass'
            }
        and: 'Instantiates a KeyStoreRepository'
            def keyPairFactory = new RsaKeyPairFactory()
            def certificateFactory = new DefaultCertificateFactory(Clock.systemDefaultZone())
            def ksRepo = new KeyStoreRepository(pkiKeyStoreConfig, keyPairFactory, certificateFactory)

        when:
            def entity = ksRepo.loadOrCreate([CN: 'Test CA'])

        then:
            entity
        and:
            (1.._) * resourceMock.exists() >> jksExists
            _ * resourceMock.contentLength() >> 0
            (1.._) * resourceMock.getOutputStream() >> { baos.reset(); return baos }
            (1.._) * pkiKeyStoreConfig.getResource() >> resourceMock
        and: 'KeyStore was closed'
            1 * baos.close()
        and: 'Memory buffer was effectively used by the service'
            baos.size() > 0
        and: 'and it is a valid KeyStore with the expected certificate inside'
            validateKeyStore(baos.toByteArray(), 'pkcs12', 'some_storepass', 'ca', entity.certificate)

        where: 'Conditions that cause KeyStore creation'
            description     | jksExists
            'JKS is absent' | false
            'JKS is empty'  | true
    }

    void validateKeyStore(byte[] content, String ksType, String storePass, String alias, X509Certificate expectedCert) {
        def ks = KeyStore.getInstance(ksType)
        new ByteArrayInputStream(content).with { input ->
            ks.load(input, storePass.toCharArray())
        }
        def cert = ks.getCertificate(alias) as X509Certificate
        assert cert == expectedCert
    }
}
