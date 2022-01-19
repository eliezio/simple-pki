/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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
package org.nordix.simplepki.application.port.in;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;
import java.util.function.LongPredicate;

public interface Pki {
    X509Certificate getCaCert();

    Pki.CrlBuilder crlBuilder();

    X509Certificate sign(PKCS10CertificationRequest csr)
        throws Exception;

    X509Certificate getCertificate(String serialNumber)
        throws CertificateException, IOException;

    X509Certificate getCertificate(long serialNumber)
        throws CertificateException, IOException;

    boolean revoke(String serialNumber, Date date);

    boolean revoke(long serialNumber, Date date);

    interface CrlBuilder {
        CrlBuilder filterByUpdateTime(LongPredicate predicate);

        Optional<X509CRL> build()
            throws Exception;
    }
}
