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

import org.nordix.simplepki.crypto.CertificateFactory;
import org.nordix.simplepki.crypto.KeyPairFactory;
import org.nordix.simplepki.crypto.PkiEntity;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeyStoreRepository implements SingleEntityRepository {

    private final KeyStoreConfig config;
    private final KeyPairFactory keyPairFactory;
    private final CertificateFactory certificateFactory;

    @Override
    public PkiEntity loadOrCreate(Map<String, String> dnMap)
        throws Exception {
        return canLoad()
            ? load()
            : create(dnMap);
    }

    private PkiEntity load()
        throws GeneralSecurityException, IOException {
        val ks = KeyStore.getInstance(config.getType());
        try (val is = config.getResource().getInputStream()) {
            ks.load(is, config.getStorepass().toCharArray());
        }
        val caKey = (PrivateKey) ks.getKey(config.getAlias(), config.getKeypass().toCharArray());
        val caCert = (X509Certificate) ks.getCertificate(config.getAlias());
        return new PkiEntity(caKey, caCert);
    }

    private PkiEntity create(Map<String, String> dnMap)
        throws Exception {
        PkiEntity entity = newEntity(dnMap);
        return save(entity);
    }

    private PkiEntity save(PkiEntity entity)
        throws GeneralSecurityException, IOException {
        val ks = KeyStore.getInstance(config.getType());
        Certificate[] certificates = {entity.getCertificate()};
        ks.load(null, null);
        ks.setKeyEntry(config.getAlias(), entity.getPrivateKey(), config.getKeypass().toCharArray(), certificates);
        try (val is = ((WritableResource) config.getResource()).getOutputStream()) {
            ks.store(is, config.getStorepass().toCharArray());
        }
        return entity;
    }

    private boolean canLoad()
        throws IOException {
        val resource = config.getResource();
        return resource.exists() && (resource.contentLength() > 0);
    }

    private PkiEntity newEntity(Map<String, String> dnMap)
        throws Exception {
        val keyPair = keyPairFactory.newKeyPair();
        X509Certificate caCert = certificateFactory.generateCaCert(dnMap, keyPair);
        return new PkiEntity(keyPair.getPrivate(), caCert);
    }
}
