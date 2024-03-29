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
package org.nordix.simplepki.application

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource
import org.springframework.validation.annotation.Validated

/**
 * All configurable parameters required to access a KeyStore.
 */
@ConfigurationProperties(prefix = "app.pki.ks")
@Validated
data class KeyStoreConfig (
    val resource: Resource,

    // Usually 'jks' or 'pkcs12' (default)
    @field:NotBlank val type: String,
    @field:NotBlank val alias: String,
    @field:NotBlank val storepass: String,
    @field:NotBlank val keypass: String,
)
