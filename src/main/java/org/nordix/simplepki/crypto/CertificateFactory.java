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

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.stream.Collectors;

public interface CertificateFactory {

    default X509Certificate generateCaCert(Map<String, String> dnMap, KeyPair keyPair)
        throws Exception {
        String dn = dnMap.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", "));
        return generateCaCert(dn, keyPair);
    }

    X509Certificate generateCaCert(String dn, KeyPair keyPair)
        throws Exception;
}
