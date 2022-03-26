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
package org.nordix.simplepki.adapters.db

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "END_ENTITY")
data class JpaEndEntity (
    @Id
    val serialNumber: Long,

    @Version
    val version: Int,
    val subject: String,
    val notValidBefore: Date,
    val notValidAfter: Date,

    @Column(length = 4096)
    val certificate: String,
    val revocationDate: Date? = null,
    val revokedReason: Int = 0,
)
