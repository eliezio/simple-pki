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
package org.nordix.simplepki.application


import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.nordix.simplepki.application.port.in.Pki
import org.nordix.simplepki.common.PemConverter
import org.nordix.simplepki.domain.model.PkiOperations
import org.spockframework.spring.SpringSpy
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.time.Clock

class PkiTransactionalSpec extends BaseSpecification {

    @Autowired
    Clock clock

    @Autowired
    Pki pki

    @SpringSpy
    PkiOperations spiedPkiOperations

    @Shared
    PKCS10CertificationRequest csr

    def setupSpec() {
        csr = loadCsr('/__data__/client-cert.csr')
    }

    def 'generates certificate under normal circumstances'() {
        when: 'Sign the certificate in a transactional context'
            def notBeforeMin = clock.instant()
            def cert = pki.sign(csr)
            def notBeforeMax = clock.instant()

        then: 'We got a valid X509 certificate'
            validateCertificate(cert, pki.getCaCert(), notBeforeMin, notBeforeMax)
        and: 'The number of issued certificates was incremented by 1'
            currentNumberOfEndEntities() == old(currentNumberOfEndEntities()) + 1
    }

    def 'must rollback the inclusion a new EndEntity when a Pki collaborator fails with #description'() {
        when: 'Attempt to sign the certificate in a transactional context'
            pki.sign(csr)

        then: 'Expect an exception to be thrown'
            thrown(exceptionClass)
        and: 'No EndEntity was persisted'
            currentNumberOfEndEntities() == old(currentNumberOfEndEntities())
        and: 'The signCsr() is invoked once and throws an throwable'
            1 * spiedPkiOperations.signCsr(_, _, _) >> { throw exceptionClass.getDeclaredConstructor().newInstance() }

        where:
            exceptionClass << [Exception, RuntimeException]
            description = exceptionClass.getName()
    }

    def 'must commit #n concurrent inclusions of EndEntity'() {
        when: 'Invoke Pki::sign in multiple threads'
            def threads = (1..n).collect { Thread.start {
                pki.sign(csr)
            }}
            threads.each { it.join() }

        then: 'The number of new EndEntities is equals of the number of threads'
            currentNumberOfEndEntities() == old(currentNumberOfEndEntities()) + 4
        and: 'The signCsr() is invoked multiple times, and performs the normal action after a delay of 0.5s'
            n * spiedPkiOperations.signCsr(_, _, _) >> { Thread.sleep(500); callRealMethod() }

        where:
            n = 4
    }

    PKCS10CertificationRequest loadCsr(String resourcePath) {
        return getClass().getResourceAsStream(resourcePath).withReader { reader ->
            return PemConverter.fromPem(reader, PKCS10CertificationRequest)
        }
    }
}
