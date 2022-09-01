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
package org.nordix.simplepki.adapters.api;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.nordix.simplepki.common.PemConverter;
import org.nordix.simplepki.domain.model.Pki;
import org.nordix.simplepki.domain.model.SerialNumberConverter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import javax.validation.constraints.Pattern;
import java.io.Reader;
import java.security.cert.X509CRL;
import java.time.Clock;
import java.util.Date;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@RestController
@RequestMapping("/pki/v1")
@RequiredArgsConstructor
@SuppressWarnings("squid:S00112")   // Generic exceptions should never be thrown
public class PkiRestController {

    private static final MediaType APPLICATION_X_PEM_FILE = MediaType.valueOf("application/x-pem-file");

    private final Pki pki;
    private final Clock clock;

    @GetMapping("/cacert")
    public ResponseEntity<String> getCaCertificate()
        throws Exception {
        val caCert = pki.getCaCert();
        return attachmentBuilder("cacert.pem", APPLICATION_X_PEM_FILE)
            .lastModified(caCert.getNotBefore().toInstant())
            .body(PemConverter.toPem(caCert));
    }

    @GetMapping("/crl")
    public ResponseEntity<String> getCrl(ServletWebRequest swr)
        throws Exception {
        X509CRL crl = pki.crlBuilder()
            .filterByUpdateTime(swr::checkNotModified)
            .build()
            .orElseThrow(ResourceNotModifiedException::new);
        return attachmentBuilder("crl.pem", APPLICATION_X_PEM_FILE)
            .lastModified(crl.getThisUpdate().toInstant())
            .body(PemConverter.toPem(crl));
    }

    @PostMapping("/certificates")
    public ResponseEntity<String> issueCertificate(Reader csrReader)
        throws Exception {
        val request = PemConverter.fromPem(csrReader, PKCS10CertificationRequest.class);
        val cert = pki.sign(request);
        return attachmentBuilder("cert.pem", APPLICATION_X_PEM_FILE)
            .header("X-Cert-Serial-Number", SerialNumberConverter.toString( cert.getSerialNumber()))
            .body(PemConverter.toPem(cert));
    }

    @GetMapping("/certificates/{serialNumber}")
    public ResponseEntity<String> getCertificate(
        @PathVariable @Pattern(regexp = SerialNumberConverter.REGEXP) String serialNumber)
        throws Exception {
        val cert = pki.getCertificate(serialNumber);
        return attachmentBuilder("cert.pem", APPLICATION_X_PEM_FILE)
            .body(PemConverter.toPem(cert));
    }

    @DeleteMapping("/certificates/{serialNumber}")
    public ResponseEntity<Void> revokeCertificate(
        @PathVariable @Pattern(regexp = SerialNumberConverter.REGEXP) String serialNumber) {
        if (!pki.revoke(serialNumber, Date.from(clock.instant()))) {
            throw new ResourceNotModifiedException();
        }
        return ResponseEntity.ok().build();
    }

    private static ResponseEntity.BodyBuilder attachmentBuilder(String filename, MediaType contentType) {
        val contentDisposition = ContentDisposition.builder("attachment")
            .filename(filename)
            .build();
        return ResponseEntity.ok()
            .contentType(contentType)
            .header(CONTENT_DISPOSITION, contentDisposition.toString());
    }
}
