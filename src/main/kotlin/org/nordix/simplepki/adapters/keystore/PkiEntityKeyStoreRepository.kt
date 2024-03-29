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
package org.nordix.simplepki.adapters.keystore

import org.nordix.simplepki.application.KeyStoreConfig
import org.nordix.simplepki.application.port.out.PkiEntityRepository
import org.nordix.simplepki.domain.model.PkiEntity
import org.springframework.stereotype.Service
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

@Service
internal class PkiEntityKeyStoreRepository(
    private val config: KeyStoreConfig
) : PkiEntityRepository {

    override fun load(): PkiEntity {
        val ks = KeyStore.getInstance(config.type)
        ks.load(config.resource.inputStream, config.storepass.toCharArray())
        val caKey = ks.getKey(config.alias, config.keypass.toCharArray()) as PrivateKey
        return PkiEntity(caKey, ks.getCertificate(config.alias) as X509Certificate)
    }
}
