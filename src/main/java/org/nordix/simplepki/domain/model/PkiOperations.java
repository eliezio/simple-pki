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
package org.nordix.simplepki.domain.model;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

@SuppressWarnings("squid:S00112")   // Generic exceptions should never be thrown
public interface PkiOperations {

    X509Certificate signCsr(PKCS10CertificationRequest csr, long serialNumber, PkiEntity ca)
        throws Exception;

    X509CRL generateCrl(List<RevocationEntry> revocations, Date editionDate, PkiEntity ca)
        throws Exception;

}
