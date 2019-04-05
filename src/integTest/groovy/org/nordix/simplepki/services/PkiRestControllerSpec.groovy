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
package org.nordix.simplepki.services

import com.epages.restdocs.apispec.ResourceDocumentation
import com.epages.restdocs.apispec.ResourceSnippetParameters
import org.nordix.simplepki.BaseSpecification
import org.nordix.simplepki.clock.Timer
import org.nordix.simplepki.crypto.PkiOperations
import org.nordix.simplepki.persist.EndEntity
import org.nordix.simplepki.persist.KeyStoreRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import groovy.sql.Sql
import org.bouncycastle.asn1.x509.CRLReason
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.util.test.FixedSecureRandom
import org.junit.Rule
import org.spockframework.spring.SpringSpy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultHandler
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

import javax.sql.DataSource
import java.security.SecureRandom
import java.time.*
import java.time.format.DateTimeFormatter

import static java.lang.System.lineSeparator
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import static javax.servlet.http.HttpServletResponse.*
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
import static org.springframework.http.HttpHeaders.IF_MODIFIED_SINCE
import static org.springframework.http.HttpHeaders.LAST_MODIFIED
import static org.springframework.restdocs.headers.HeaderDocumentation.*
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// 'printOnlyOnFailure=false' saves us from adding ".andDo(print())" to every MockMvc perform
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@AutoConfigureRestDocs
@TestPropertySource(locations = [
    'classpath:test-db.properties',
    'classpath:test-ks.properties'
])
@Stepwise
class PkiRestControllerSpec extends BaseSpecification {

    // extracted with "keytool -keystore /__data__/ca.p12 -storepass changeit -list -v"
    static final Instant CACERT_ISSUE_DATE      = Instant.parse('2019-04-13T10:17:34Z')
    static final String  APPLICATION_X_PEM_FILE = 'application/x-pem-file'

    static final DateTimeFormatter OPENSSL_DATE_TIME = DateTimeFormatter.ofPattern('MMM dd HH:mm:ss yyyy z')
    static final ZoneId            GMT               = ZoneId.of('GMT')
    static final long              MILLIS_PER_HOUR   = 3_600_000
    static final long              FIXED_ELAPSED     = 47

    @Autowired
    MockMvc mockMvc

    @SpringSpy
    KeyStoreRepository spiedKeyStoreRepository

    @SpringSpy
    PkiOperations spiedPkiOperations

    @Shared
    X509CertificateHolder caCert

    @Shared
    X509CertificateHolder cert1

    @Shared
    EndEntity cert1Entity

    @Shared
    long crlLastUpdate

    @Shared
    ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    @Autowired
    DataSource dataSource

    // using a FixedClock in order to have predictable date/time on persisted data
    @Autowired
    Clock clock

    @AutoCleanup
    Sql sql

    @Rule
    EventCatcher evtCatcher = new EventCatcher()

    def setup() {
        sql = new Sql(dataSource)
    }

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    def 'server is unavailable caused by a failure on opening keystore'() {
        when: 'Request for the CA certificate - the simplest service available'
            def response = mockMvc.perform(get("/pki/v1/cacert"))
                .andReturn().response

        then: 'Server fails, reporting a SC=500'
            response.status == SC_INTERNAL_SERVER_ERROR
        and: 'KeyStore I/O failure caused this error'
            1 * spiedKeyStoreRepository.loadOrCreate(_) >> { throw new Exception('Failed to open file') }
    }

    def 'supplies the current CA certificate in PEM format'() {
        when: 'Request the current CA certificate'
            def response = mockMvc.perform(get("/pki/v1/cacert"))
                .andDo(document("get-cacert",
                    ResourceDocumentation.resource('Retrieve the CA certificate in PEM format')))
                .andReturn().response

        then: 'Request succeeds'
            response.status == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'A Last-Modified date was informed'
            response.getDateHeader(LAST_MODIFIED) > 0
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        and: 'and can be decoded into a certificate'
            def cert = decodeCert(response.getContentAsByteArray())
        and: 'It is a valid CA certificate'
            validateCertificate(cert, null, CACERT_ISSUE_DATE, CACERT_ISSUE_DATE)
        and: 'The GET event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'GET', uri: '/pki/v1/cacert', sc: '200'] - event).isEmpty()

        cleanup:
            // Save CA certificate for use on subsequent tests
            caCert = cert
    }

    def 'supplies the initial empty CRL in PEM format'() {
        when: 'Request the current CRL'
            def response = mockMvc.perform(get("/pki/v1/crl"))
                .andReturn().response

        then: 'Request succeeds'
            response.status == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'A Last-Modified date was informed'
            response.getDateHeader(LAST_MODIFIED) > 0
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        and: 'and can be decoded into a CRL'
            def crl = decodeCrl(response.getContentAsByteArray())
        and: 'It is a valid and empty CRL'
            validateCrl(crl, caCert, [])
        and: 'The GET event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'GET', uri: '/pki/v1/crl', sc: '200'] - event).isEmpty()

        cleanup:
            crlLastUpdate = response.getDateHeader(LAST_MODIFIED)
    }

    @Unroll
    def 'supports conditional request of CRL #description'() {
        when: 'Request the current CRL'
            def response = mockMvc.perform(get("/pki/v1/crl")
                .header(IF_MODIFIED_SINCE, ifModifiedSince))
                .andDo(documentId ? document("get-cond-crl", requestHeaders(
                    headerWithName(IF_MODIFIED_SINCE).description('The date/time of a previously fetched CRL')
                )) : NoopResultHandler.instance)
                .andReturn().response

        then: 'Request succeeds'
            response.status == expectedSc
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        then: 'Should generate CRL exact #expectedCrlGenerations time'
            expectedCrlGenerations * spiedPkiOperations.generateCrl(_, _, _)
        and: 'The GET event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'GET', uri: '/pki/v1/crl', sc: expectedSc as String] - event).isEmpty()

        where:
            description            | ifModifiedSinceTime             | formatter          || expectedSc
            '1h earlier (RFC1123)' | crlLastUpdate - MILLIS_PER_HOUR | RFC_1123_DATE_TIME || SC_OK
            'same time (RFC1123)*' | crlLastUpdate                   | RFC_1123_DATE_TIME || SC_NOT_MODIFIED
            '1h later (RFC1123)'   | crlLastUpdate + MILLIS_PER_HOUR | RFC_1123_DATE_TIME || SC_NOT_MODIFIED
            '1h earlier (OpenSSL)' | crlLastUpdate - MILLIS_PER_HOUR | OPENSSL_DATE_TIME  || SC_OK
            'same time (OpenSSL)'  | crlLastUpdate                   | OPENSSL_DATE_TIME  || SC_NOT_MODIFIED
            '1h later (OpenSSL)'   | crlLastUpdate + MILLIS_PER_HOUR | OPENSSL_DATE_TIME  || SC_NOT_MODIFIED

            ifModifiedSince = formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(ifModifiedSinceTime), GMT))
            expectedCrlGenerations = expectedSc == SC_OK ? 1 : 0
            documentId = description.endsWith('*')
    }

    @Unroll
    def 'report BAD REQUEST when data sent to be signed is a #description'() {
        given: 'Retrieve the source contents'
            def input = getClass().getResourceAsStream("/__data__/$filename").bytes

        when: 'Submit this content as a supposed PEM CSR data'
            def response = mockMvc.perform(post("/pki/v1/certificates")
                .contentType(APPLICATION_X_PEM_FILE)
                .content(input))
                .andReturn().response

        then: 'A BAD_REQUEST is reported'
            response.status == SC_BAD_REQUEST
        and: 'An explanation was given'
            response.errorMessage

        where:
            filename           | description
            'client-cert.pem'  | 'PEM CERTIFICATE'
            'client-cert.der'  | 'DER CERTIFICATE'
            'client-cert.zero' | 'empty file'
    }

    def 'issue a certificate by sending its CSR'() {
        given: 'Extract CSR from pre-created resource file'
            def csrLines = getClass().getResourceAsStream('/__data__/client-cert.csr').readLines()
        and: 'Assert that no certificate was issued so far'
            //noinspection SqlDialectInspection,SqlNoDataSourceInspection
            sql.firstRow('SELECT count(1) AS numberOfRows FROM END_ENTITY').numberOfRows == 0

        when: 'Submit CSR in order to be signed by CA'
            def notBeforeMin = clock.instant()
            def response = mockMvc.perform(post("/pki/v1/certificates")
                .contentType(APPLICATION_X_PEM_FILE)
                .content(csrLines.join(lineSeparator())))
                .andDo(document("issue-cert",
                    ResourceDocumentation.resource('Issue a certificate by sending its CSR'),
                    responseHeaders(
                        headerWithName('X-Cert-Serial-Number').description('Certificate serial number')
                    )
                ))
                .andReturn().response
            def notBeforeMax = clock.instant()

        then: 'Request succeeds'
            response.status == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        and: 'and can be decoded into a certificate'
            def cert = decodeCert(response.getContentAsByteArray())
        and: 'It is a valid certificate signed by the CA'
            validateCertificate(cert, caCert, notBeforeMin, notBeforeMax)
        and: 'An X-Cert-Serial-Number header was sent'
            response.getHeader('X-Cert-Serial-Number') as long == cert.getSerialNumber().toLong()
        and: 'A corresponding EndEntity for the new certificate was correctly persisted on DB'
            def expectedEntity = new EndEntity(serialNumber: 1, version: 1, subject: 'CN=localhost,OU=ESY,O=Nordix Foundation,L=Athlone,C=IE',
                notValidBefore: cert.notBefore, notValidAfter: cert.notAfter, certificate: response.getContentAsString())
            def entity = fetchCertificateEntity(1)
            entity == expectedEntity
        and: 'The POST event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'POST', uri: '/pki/v1/certificates', sc: '200'] - event).isEmpty()

        cleanup:
            // Save this Certificate and EndEntity to be used on subsequent tests
            cert1 = cert
            cert1Entity = entity
    }

    def 'retrieve certificate by its serialNumber'() {
        when: 'Request issued certificate'
            def response = mockMvc.perform(get("/pki/v1/certificates/{serialNumber}", 1))
                .andDo(document("get-cert",
                    ResourceDocumentation.resource(ResourceSnippetParameters.builder()
                    .description('Retrieve a certificate previously issued by this CA')
                    .pathParameters(
                        ResourceDocumentation.parameterWithName('serialNumber')
                            .description('Serial number of wanted certificate')
                    )
                    .build()),
                    // redundant definition :-( Couldn't find a way to avoid this duplication.
                    pathParameters(parameterWithName('serialNumber')
                        .description('Serial number of wanted certificate'))
                ))
                .andReturn().response

        then: 'Request succeeds'
            response.status == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        and: 'and can be decoded into a certificate'
            def cert = decodeCert(response.getContentAsByteArray())
        and:
            cert == cert1
        and: 'The GET event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'GET', uri: '/pki/v1/certificates/1', sc: '200'] - event).isEmpty()
    }

    def 'revoke certificate'() {
        given: 'Advance clock by 1h'
            def revocationDate = Date.from(clock.instant())
            def expectedEntity = updateCertificateEntity(cert1Entity, revocationDate, CRLReason.privilegeWithdrawn)

        when: 'Request revocation for the sole certificate issued so far'
            def response = mockMvc.perform(delete("/pki/v1/certificates/{serialNumber}", 1))
                .andDo(document("revoke-cert",
                    ResourceDocumentation.resource(ResourceSnippetParameters.builder()
                    .description('Revokes a certificate previously issued by this CA')
                    .pathParameters(
                        ResourceDocumentation.parameterWithName('serialNumber')
                            .description('Serial number of certificate to be revoked')
                    )
                    .build()),
                    // redundant definition :-( Couldn't find a way to avoid this duplication.
                    pathParameters(parameterWithName('serialNumber')
                        .description('Serial number of certificate to be revoked'))
                ))
                .andReturn().response

        then: 'Request succeeds'
            response.status == SC_OK
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        and: 'The corresponding EndEntity was updated with the revocation info'
            def entity = fetchCertificateEntity(1)
            entity == expectedEntity
        and: 'The DELETE event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'DELETE', uri: '/pki/v1/certificates/1', sc: '200'] - event).isEmpty()

        cleanup:
            // Save the updated EndEntity
            cert1Entity = entity
    }

    def 'fail to revoke unknown certificate'() {
        when: 'Request to revoke a certificate using a non-existent serial number'
            def response = mockMvc.perform(delete("/pki/v1/certificates/{serialNumber}", 1000))
                .andReturn().response

        then: 'Service reports 404 since the resource was not found'
            response.status == SC_NOT_FOUND
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
    }

    def 'NOP if requested to revoke a already revoked certificate'() {
        when: 'Request a second revocation on the first certificate, already revoked'
            def response = mockMvc.perform(delete("/pki/v1/certificates/{serialNumber}", 1))
                .andReturn().response

        then: 'Service reports that no change was made'
            response.status == SC_NOT_MODIFIED
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        and: 'no change was made on the corresponding DB certificate entity'
            def entity = fetchCertificateEntity(1)
            entity == cert1Entity
    }

    def 'issue a second certificate by sending its CSR'() {
        given: 'Extract CSR from pre-created resource file'
            def csrLines = getClass().getResourceAsStream('/__data__/client-cert.csr').readLines()

        when: 'Submit CSR in order to be signed by CA'
            def notBeforeMin = clock.instant()
            def response = mockMvc.perform(post("/pki/v1/certificates")
                .contentType(APPLICATION_X_PEM_FILE)
                .content(csrLines.join(lineSeparator())))
                .andReturn().response
            def notBeforeMax = clock.instant()

        then: 'Request succeeds'
            response.status == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        and: 'Content can be decoded into a certificate'
            def cert = decodeCert(response.getContentAsByteArray())
        and: 'It is a valid certificate signed by the CA'
            validateCertificate(cert, caCert, notBeforeMin, notBeforeMax)
        and: 'An X-Cert-Serial-Number header was sent'
            response.getHeader('X-Cert-Serial-Number') as long == cert.getSerialNumber().toLong()
        and: 'The corresponding EndEntity was persisted on DB'
            def expectedEntity = new EndEntity(serialNumber: 2, version: 1, subject: 'CN=localhost,OU=ESY,O=Nordix Foundation,L=Athlone,C=IE',
                notValidBefore: cert.notBefore, notValidAfter: cert.notAfter, certificate: response.getContentAsString())
            def entity = fetchCertificateEntity(2)
            entity == expectedEntity
    }

    def 'retrieves CRL with revoked certificates in PEM format'() {
        when: 'Request for the current CRL'
            def response = mockMvc.perform(get("/pki/v1/crl"))
                .andDo(document("get-crl",
                    ResourceDocumentation.resource('Retrieve the up-to-date CRL in PEM format')))
                .andReturn().response

        then: 'Request succeeds'
            response.status == SC_OK
        and: 'An Elapsed-Time was properly informed'
            validateElapsedTime(response)
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'Content can be converted into a CRL'
            def crl = decodeCrl(response.getContentAsByteArray())
        and: 'It is a valid CRL signed by the CA and with a single entry for the certificate #1'
            validateCrl(crl, caCert, [BigInteger.ONE])
    }

    EndEntity updateCertificateEntity(EndEntity source, Date revocationDate, int revokedReason) {
        return new EndEntity(serialNumber: source.serialNumber,
            version: source.version + 1,
            subject: source.subject,
            notValidBefore: source.notValidBefore,
            notValidAfter: source.notValidAfter,
            certificate: source.certificate,
            revocationDate: revocationDate,
            revokedReason: revokedReason
        )
    }

    void validateElapsedTime(MockHttpServletResponse response) {
        if (response.status == SC_OK) {
            def elapsedTime = response.getHeader('Elapsed-Time') as long
            assert elapsedTime == FIXED_ELAPSED
        }
    }

    EndEntity fetchCertificateEntity(int serialNumber) {
        def rows = sql.rows("""
                SELECT *
                  FROM END_ENTITY
                 WHERE serial_number = $serialNumber
""")
        assert rows.size() == 1
        return mapper.convertValue(lowerKeyMap(rows.first()), EndEntity)
    }

    Map lowerKeyMap(Map source) {
        return source.collectEntries { k, v -> [(k as String).toLowerCase(), v] }
    }

    static Map validatedEvent(Map evt) {
        assert 'elapsed' in evt
        def elapsed = evt['elapsed'] as int
        assert elapsed == FIXED_ELAPSED
        return evt
    }

    @Singleton
    static class NoopResultHandler implements ResultHandler {
        @Override
        void handle(MvcResult result) {}
    }

    @TestConfiguration
    static class RepeatableBuildConfig {

        @Bean
        SecureRandom fakeRandom() {
            return new FixedSecureRandom((0..255) as byte[])
        }

        @Bean
        @Primary
        Clock fixedClock() {
            def myZoneId = ZoneId.of('America/Sao_Paulo')
            def myDepartureDateTime = LocalDateTime.parse("2019-01-08T14:30:00").atZone(myZoneId)
            return Clock.fixed(myDepartureDateTime.toInstant(), myZoneId)
        }

        @Bean
        @Primary
        @Scope(SCOPE_PROTOTYPE)
        Timer fixedTimer() {
            return new Timer() {
                @Override
                long elapsed() {
                    return FIXED_ELAPSED
                }
            }
        }
    }
}
