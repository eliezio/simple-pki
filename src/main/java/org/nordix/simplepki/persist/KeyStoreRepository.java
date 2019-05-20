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
package org.nordix.simplepki.persist;

import org.nordix.simplepki.crypto.PkiEntity;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Service
@RequiredArgsConstructor
public class KeyStoreRepository implements SingleEntityRepository {

    private final KeyStoreConfig config;

    @Override
    public PkiEntity load()
        throws GeneralSecurityException, IOException {
        val ks = KeyStore.getInstance(config.getType());
        try (val is = config.getResource().getInputStream()) {
            ks.load(is, config.getStorepass().toCharArray());
        }
        val caKey = (PrivateKey) ks.getKey(config.getAlias(), config.getKeypass().toCharArray());
        val caCert = (X509Certificate) ks.getCertificate(config.getAlias());
        return new PkiEntity(caKey, caCert);
    }
}
