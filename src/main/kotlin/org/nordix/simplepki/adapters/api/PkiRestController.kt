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
package org.nordix.simplepki.adapters.api

import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.nordix.simplepki.application.port.`in`.Pki
import org.nordix.simplepki.common.PemConverter
import org.nordix.simplepki.domain.model.SerialNumberConverter
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.ServletWebRequest
import java.io.Reader
import java.time.Clock
import java.util.*
import javax.validation.constraints.Pattern

@RestController
@RequestMapping("/pki/v1")
internal class PkiRestController(
    private val pki: Pki,
    private val clock: Clock,
) {

    @GetMapping("/cacert")
    fun caCertificate(): ResponseEntity<String> {
        val caCert = pki.caCert
        return attachmentBuilder("cacert.pem", APPLICATION_X_PEM_FILE)
            .lastModified(caCert.notBefore.toInstant())
            .body(PemConverter.toPem(caCert))
    }

    @GetMapping("/crl")
    fun getCrl(swr: ServletWebRequest): ResponseEntity<String> {
        val crlBuilder = pki.crlBuilder()
        if (swr.checkNotModified(crlBuilder.editionTime())) {
            throw ResourceNotModifiedException()
        }
        val crl = crlBuilder.build()
        return attachmentBuilder("crl.pem", APPLICATION_X_PEM_FILE)
            .lastModified(crl.thisUpdate.toInstant())
            .body(PemConverter.toPem(crl))
    }

    @PostMapping("/certificates")
    fun issueCertificate(csrReader: Reader): ResponseEntity<String> {
        val request =
            PemConverter.fromPem(csrReader, PKCS10CertificationRequest::class.java)
        val cert = pki.sign(request)
        return attachmentBuilder("cert.pem", APPLICATION_X_PEM_FILE)
            .header("X-Cert-Serial-Number", SerialNumberConverter.toString(cert.serialNumber))
            .body(PemConverter.toPem(cert))
    }

    @GetMapping("/certificates/{serialNumber}")
    fun getCertificate(
        @PathVariable serialNumber: @Pattern(regexp = SerialNumberConverter.REGEXP) String
    ): ResponseEntity<String> {
        val cert = pki.getCertificate(serialNumber)
        return attachmentBuilder("cert.pem", APPLICATION_X_PEM_FILE)
            .body(PemConverter.toPem(cert))
    }

    @DeleteMapping("/certificates/{serialNumber}")
    fun revokeCertificate(
        @PathVariable serialNumber: @Pattern(regexp = SerialNumberConverter.REGEXP) String
    ): ResponseEntity<Void> {
        if (!pki.revoke(serialNumber, Date.from(clock.instant()))) {
            throw ResourceNotModifiedException()
        }
        return ResponseEntity.ok().build()
    }

    companion object {
        private val APPLICATION_X_PEM_FILE = MediaType.valueOf("application/x-pem-file")

        private fun attachmentBuilder(filename: String, contentType: MediaType): ResponseEntity.BodyBuilder {
            val contentDisposition = ContentDisposition.builder("attachment")
                .filename(filename)
                .build()
            return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        }
    }
}
