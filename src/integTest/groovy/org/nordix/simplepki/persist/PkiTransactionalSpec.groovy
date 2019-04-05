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
import org.nordix.simplepki.crypto.PemConverter
import org.nordix.simplepki.crypto.Pki
import org.nordix.simplepki.crypto.PkiOperations
import groovy.sql.Sql
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.spockframework.spring.SpringSpy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Unroll

import javax.sql.DataSource
import java.time.Clock

@DataJpaTest
@ComponentScan(basePackages = [
    'org.nordix.simplepki.clock',
    'org.nordix.simplepki.crypto',
    'org.nordix.simplepki.persist'
])
@TestPropertySource(locations = [
    // In case you need to debug DB-related activities, just define the env var SPRING_PROFILES_ACTIVE=debug
    'classpath:test-db.properties',
    'classpath:test-ks.properties'
])
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PkiTransactionalSpec extends BaseSpecification {

    @Autowired
    Clock clock

    @Autowired
    DataSource dataSource

    @Autowired
    Pki pki

    @SpringSpy
    PkiOperations spiedPkiOperations

    @AutoCleanup
    Sql sql

    @Shared
    PKCS10CertificationRequest csr

    def setupSpec() {
        csr = loadCsr('/__data__/client-cert.csr')
    }

    def setup() {
        sql = new Sql(dataSource)
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

    @Unroll
    def 'must rollback the inclusion a new EndEntity when a Pki collaborator fails with #description'() {
        when: 'Attempt to sign the certificate in a transactional context'
            pki.sign(csr)

        then: 'Expect an exception to be thrown'
            thrown(exceptionClass)
        and: 'No EndEntity was persisted'
            currentNumberOfEndEntities() == old(currentNumberOfEndEntities())
        and: 'The signCsr() is invoked once and throws an throwable'
            1 * spiedPkiOperations.signCsr(_, _, _) >> { throw exceptionClass.newInstance() }

        where:
            exceptionClass << [Exception, RuntimeException]
            description = exceptionClass.getName()
    }

    @Unroll
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

    int currentNumberOfEndEntities() {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        return sql.firstRow('SELECT count(1) AS numberOfRows FROM END_ENTITY').numberOfRows
    }

    PKCS10CertificationRequest loadCsr(String resourcePath) {
        return getClass().getResourceAsStream(resourcePath).withReader { reader ->
            return PemConverter.fromPem(reader, PKCS10CertificationRequest)
        }
    }
}
