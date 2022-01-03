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
package org.nordix.simplepki.adapters.api

import com.epages.restdocs.apispec.ResourceDocumentation
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.RestAssuredRestDocumentationWrapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import groovy.sql.Sql
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.RestAssuredConfig
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.bouncycastle.asn1.x509.CRLReason
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.util.test.FixedSecureRandom
import org.junit.Rule
import org.junit.jupiter.api.extension.ExtendWith
import org.nordix.simplepki.application.BaseSpecification
import org.nordix.simplepki.domain.model.EndEntity
import org.nordix.simplepki.domain.model.PkiOperations
import org.nordix.simplepki.domain.model.SerialNumberConverter
import org.nordix.simplepki.domain.ports.PkiEntityRepository
import org.spockframework.spring.SpringSpy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

import javax.sql.DataSource
import java.security.SecureRandom
import java.time.*
import java.time.format.DateTimeFormatter

import static io.restassured.RestAssured.given
import static io.restassured.config.EncoderConfig.encoderConfig
import static java.lang.System.lineSeparator
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import static javax.servlet.http.HttpServletResponse.*
import static org.springframework.http.HttpHeaders.IF_MODIFIED_SINCE
import static org.springframework.http.HttpHeaders.LAST_MODIFIED
import static org.springframework.restdocs.headers.HeaderDocumentation.*
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.documentationConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension)
@TestPropertySource(locations = [
    'classpath:test-db.properties',
    'classpath:test-ks.properties'
])
@Stepwise
class PkiRestControllerSpec extends BaseSpecification {

    // extracted with "keytool -keystore /__data__/ca.p12 -storepass changeit -list -v"
    static final Instant CACERT_ISSUE_DATE      = Instant.parse('2019-04-13T10:17:34Z')
    static final String  APPLICATION_X_PEM_FILE = 'application/x-pem-file'

    static final ZoneId            GMT               = ZoneId.of('GMT')
    static final DateTimeFormatter HTTP_DATE_FORMAT = DateTimeFormatter.ofPattern('EEE, dd MMM yyyy HH:mm:ss zzz', Locale.US)

    static final DateTimeFormatter OPENSSL_DATE_TIME = DateTimeFormatter.ofPattern('MMM dd HH:mm:ss yyyy z', Locale.US)
    static final Duration          TEN_MINUTES       = Duration.ofMinutes(10)

    static final RestAssuredConfig configPemFile     = RestAssured.config().encoderConfig(
        encoderConfig().encodeContentTypeAs(APPLICATION_X_PEM_FILE, ContentType.TEXT))

    @LocalServerPort
    int localPort

    @SpringSpy
    PkiEntityRepository spiedKeyStoreRepository

    @SpringSpy
    PkiOperations spiedPkiOperations

    @Shared
    X509CertificateHolder caCert

    @Shared
    X509CertificateHolder cert1

    @Shared
    EndEntity cert1Entity

    @Shared
    String cert1SerialNumber

    @Shared
    Instant crlLastUpdate

    @Shared
    ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    @Autowired
    DataSource dataSource

    // using a FixedClock in order to have predictable date/time on persisted data
    @Autowired
    Clock clock

    @AutoCleanup
    Sql sql

    @Rule
    EventCatcher evtCatcher = new EventCatcher()

    @Autowired
    RestDocumentationContextProvider restDocumentation

    RequestSpecification documentationSpec

    def setup() {
        RestAssured.port = localPort
        sql = new Sql(dataSource)
        documentationSpec = new RequestSpecBuilder()
            .addFilter(documentationConfiguration(restDocumentation)
                .operationPreprocessors()
                .withRequestDefaults(modifyUris().port(8080))
                .withResponseDefaults(
                    removeHeaders('Connection', 'Date'),
                    prettyPrint()
                ))
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .build()
    }

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    def 'server is unavailable caused by a failure on opening keystore'() {
        when: 'Request for the CA certificate - the simplest service available'
            def response = given(this.documentationSpec)
                .accept(APPLICATION_X_PEM_FILE)
                .when()
                .get("/pki/v1/cacert")
                .andReturn()

        then: 'Server fails, reporting a SC=500'
            response.statusCode == SC_INTERNAL_SERVER_ERROR
        and: 'KeyStore I/O failure caused this error'
            1 * spiedKeyStoreRepository.load() >> { throw new Exception('Failed to open file') }
    }

    def 'report OK for #uri service'() {
        when: 'Request the resource'
            def response = given(this.documentationSpec)
                .when()
                .get(uri)
                .andReturn()

        then: 'SC is OK'
            response.statusCode == SC_OK

        where:
            uri = '/healthz'
    }

    def 'supplies the current CA certificate in PEM format'() {
        when: 'Request the current CA certificate'
            def response = given(this.documentationSpec)
                .filter(RestAssuredRestDocumentationWrapper.document("get-cacert",
                    'Retrieve the CA certificate in PEM format'))
                .when()
                .get("/pki/v1/cacert")
                .andReturn()

        then: 'Request succeeds'
            response.statusCode == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'A Last-Modified date was informed'
            HTTP_DATE_FORMAT.parse(response.header(LAST_MODIFIED))
        and: 'and can be decoded into a certificate'
            def cert = decodeCert(response.body().asByteArray())
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
            def response = given(this.documentationSpec)
                .when()
                .get("/pki/v1/crl")
                .andReturn()

        then: 'Request succeeds'
            response.statusCode == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'A Last-Modified date was informed'
            HTTP_DATE_FORMAT.parse(response.header(LAST_MODIFIED))
        and: 'and can be decoded into a CRL'
            def crl = decodeCrl(response.body().asByteArray())
        and: 'It is a valid and empty CRL'
            validateCrl(crl, caCert, [])
        and: 'The GET event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'GET', uri: '/pki/v1/crl', sc: '200'] - event).isEmpty()

        cleanup:
            crlLastUpdate = Instant.from(HTTP_DATE_FORMAT.parse(response.header(LAST_MODIFIED)))
    }

    def 'supports conditional request of CRL #description'() {
        when: 'Request the current CRL'
            def response = given(this.documentationSpec)
                .header(IF_MODIFIED_SINCE, ifModifiedSince)
                .filters(documentId ? [document("get-cond-crl", requestHeaders(
                    headerWithName(IF_MODIFIED_SINCE).description('The date/time of a previously fetched CRL')
                ))] : [])
                .when()
                .get("/pki/v1/crl")
                .andReturn()

        then: 'Request succeeds'
            response.statusCode == expectedSc
        then: 'Should generate CRL exact #expectedCrlGenerations time'
            expectedCrlGenerations * spiedPkiOperations.generateCrl(_, _, _)
        and: 'The GET event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'GET', uri: '/pki/v1/crl', sc: expectedSc as String] - event).isEmpty()

        where:
            description               | ifModifiedSinceTime         | formatter          || expectedSc
            '10min earlier (RFC1123)' | crlLastUpdate - TEN_MINUTES | RFC_1123_DATE_TIME || SC_OK
            'same time (RFC1123)*'    | crlLastUpdate               | RFC_1123_DATE_TIME || SC_NOT_MODIFIED
            '10min later (RFC1123)'   | crlLastUpdate + TEN_MINUTES | RFC_1123_DATE_TIME || SC_NOT_MODIFIED
            '10min earlier (OpenSSL)' | crlLastUpdate - TEN_MINUTES | OPENSSL_DATE_TIME  || SC_OK
            'same time (OpenSSL)'     | crlLastUpdate               | OPENSSL_DATE_TIME  || SC_NOT_MODIFIED
            '10min later (OpenSSL)'   | crlLastUpdate + TEN_MINUTES | OPENSSL_DATE_TIME  || SC_NOT_MODIFIED

            ifModifiedSince = formatter.withZone(GMT).format(ifModifiedSinceTime)
            expectedCrlGenerations = expectedSc == SC_OK ? 1 : 0
            documentId = description.endsWith('*')
    }

    def 'report BAD REQUEST when data sent to be signed is a #description'() {
        given: 'Retrieve the source contents'
            def input = getClass().getResourceAsStream("/__data__/$filename").bytes

        when: 'Submit this content as a supposed PEM CSR data'
            def response = given(this.documentationSpec)
                .contentType(APPLICATION_X_PEM_FILE)
                .body(input)
                .when()
                .post("/pki/v1/certificates")
                .andReturn()

        then: 'A BAD_REQUEST is reported'
            response.statusCode == SC_BAD_REQUEST
        and: 'An explanation was given'
            response.jsonPath().get('message')

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

            def response = given(this.documentationSpec)
                .config(configPemFile)
                .contentType(APPLICATION_X_PEM_FILE)
                .body(csrLines.join(lineSeparator()))
                .filter(RestAssuredRestDocumentationWrapper.document("issue-cert",
                    'Issue a certificate by sending its CSR',
                    responseHeaders(
                        headerWithName('X-Cert-Serial-Number').description('Certificate serial number')
                    )
                ))
                .when()
                .post("/pki/v1/certificates")
                .andReturn()
            def notBeforeMax = clock.instant()

        then: 'Request succeeds'
            response.statusCode == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'and can be decoded into a certificate'
            def cert = decodeCert(response.body().asByteArray())
            def serialNumber = cert.getSerialNumber().toLong()
            def serialNumberStr = SerialNumberConverter.toString(serialNumber)
        and: 'It is a valid certificate signed by the CA'
            validateCertificate(cert, caCert, notBeforeMin, notBeforeMax)
        and: 'An X-Cert-Serial-Number header was sent'
            response.getHeader('X-Cert-Serial-Number') == serialNumberStr
        and: 'A corresponding EndEntity for the new certificate was correctly persisted on DB'
            def expectedEntity = new EndEntity(serialNumber: serialNumber, version: 0,
                subject: 'CN=localhost,OU=ESY,O=Nordix Foundation,L=Athlone,C=IE',
                notValidBefore: cert.notBefore, notValidAfter: cert.notAfter, certificate: response.body().asString())
            def entity = fetchCertificateEntity(serialNumber)
            entity == expectedEntity
        and: 'The POST event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'POST', uri: '/pki/v1/certificates', sc: '200'] - event).isEmpty()

        cleanup:
            // Save this Certificate and EndEntity to be used on subsequent tests
            cert1 = cert
            cert1SerialNumber = serialNumberStr
            cert1Entity = entity
    }

    def 'retrieve certificate by its serialNumber'() {
        when: 'Request issued certificate'
            def response = given(this.documentationSpec)
                .filter(RestAssuredRestDocumentationWrapper.document("get-cert",
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
                .when()
                .get("/pki/v1/certificates/{serialNumber}", cert1SerialNumber)
                .andReturn()

        then: 'Request succeeds'
            response.statusCode == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'and can be decoded into a certificate'
            def cert = decodeCert(response.body().asByteArray())
        and:
            cert == cert1
        and: 'The GET event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'GET', uri: "/pki/v1/certificates/$cert1SerialNumber", sc: '200'] - event).isEmpty()
    }

    def 'revoke certificate'() {
        given: 'Advance clock by 1h'
            def revocationDate = Date.from(clock.instant())
            def expectedEntity = updateCertificateEntity(cert1Entity, revocationDate, CRLReason.privilegeWithdrawn)

        when: 'Request revocation for the sole certificate issued so far'
            def response = given(this.documentationSpec)
                .filter(RestAssuredRestDocumentationWrapper.document("revoke-cert",
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
                .when()
                .delete("/pki/v1/certificates/{serialNumber}", cert1SerialNumber)
                .andReturn()

        then: 'Request succeeds'
            response.statusCode == SC_OK
        and: 'The corresponding EndEntity was updated with the revocation info'
            def entity = fetchCertificateEntity(cert1.serialNumber.longValue())
            entity == expectedEntity
        and: 'The DELETE event was logged'
            def event = validatedEvent(evtCatcher.getOnlyEvent())
            ([evt: 'SERVICE', method: 'DELETE', uri: "/pki/v1/certificates/$cert1SerialNumber", sc: '200'] - event).isEmpty()

        cleanup:
            // Save the updated EndEntity
            cert1Entity = entity
    }

    def 'fail to revoke unknown certificate'() {
        when: 'Request to revoke a certificate using a non-existent serial number'
            def response = given(this.documentationSpec)
                .when()
                .delete("/pki/v1/certificates/{serialNumber}", 1000)
                .andReturn()

        then: 'Service reports 404 since the resource was not found'
            response.statusCode == SC_NOT_FOUND
    }

    def 'NOP if requested to revoke a already revoked certificate'() {
        when: 'Request a second revocation on the first certificate, already revoked'
            def response = given(this.documentationSpec)
                .when()
                .delete("/pki/v1/certificates/{serialNumber}", cert1SerialNumber)
                .andReturn()

        then: 'Service reports that no change was made'
            response.statusCode == SC_NOT_MODIFIED
        and: 'no change was made on the corresponding DB certificate entity'
            def entity = fetchCertificateEntity(cert1.serialNumber.longValue())
            entity == cert1Entity
    }

    def 'issue a second certificate by sending its CSR'() {
        given: 'Extract CSR from pre-created resource file'
            def csrLines = getClass().getResourceAsStream('/__data__/client-cert.csr').readLines()

        when: 'Submit CSR in order to be signed by CA'
            def notBeforeMin = clock.instant()
            def response = given(this.documentationSpec)
                .config(configPemFile)
                .contentType(APPLICATION_X_PEM_FILE)
                .body(csrLines.join(lineSeparator()))
                .when()
                .post("/pki/v1/certificates")
                .andReturn()
            def notBeforeMax = clock.instant()

        then: 'Request succeeds'
            response.statusCode == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'Content can be decoded into a certificate'
            def cert = decodeCert(response.body().asByteArray())
            def serialNumber = cert.serialNumber.longValue()
            def serialNumberStr = SerialNumberConverter.toString(serialNumber)
        and: 'It is a valid certificate signed by the CA'
            validateCertificate(cert, caCert, notBeforeMin, notBeforeMax)
        and: 'An X-Cert-Serial-Number header was sent'
            response.getHeader('X-Cert-Serial-Number') == serialNumberStr
        and: 'The corresponding EndEntity was persisted on DB'
            def expectedEntity = new EndEntity(serialNumber: serialNumber, version: 0,
                subject: 'CN=localhost,OU=ESY,O=Nordix Foundation,L=Athlone,C=IE',
                notValidBefore: cert.notBefore, notValidAfter: cert.notAfter, certificate: response.body().asString())
            def entity = fetchCertificateEntity(serialNumber)
            entity == expectedEntity
    }

    def 'retrieves CRL with revoked certificates in PEM format'() {
        when: 'Request for the current CRL'
            def response = given(this.documentationSpec)
                .filter(RestAssuredRestDocumentationWrapper.document("get-crl",
                    'Retrieve the up-to-date CRL in PEM format'))
                .when()
                .get("/pki/v1/crl")
                .andReturn()

        then: 'Request succeeds'
            response.statusCode == SC_OK
        and: 'Content has the proper MIME type'
            response.contentType == APPLICATION_X_PEM_FILE
        and: 'Content can be converted into a CRL'
            def crl = decodeCrl(response.body().asByteArray())
        and: 'It is a valid CRL signed by the CA and with a single entry for the certificate #1'
            validateCrl(crl, caCert, [cert1.serialNumber])
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

    EndEntity fetchCertificateEntity(long serialNumber) {
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
        assert elapsed >= 0
        return evt
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
    }
}
