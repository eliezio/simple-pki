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
package org.nordix.simplepki.application.port.`in`

import org.bouncycastle.pkcs.PKCS10CertificationRequest
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.util.*

interface Pki {

    val caCert: X509Certificate

    fun crlBuilder(): CrlBuilder

    fun sign(csr: PKCS10CertificationRequest): X509Certificate

    fun getCertificate(serialNumber: String): X509Certificate

    fun getCertificate(serialNumber: Long): X509Certificate

    fun revoke(serialNumber: String, date: Date): Boolean

    fun revoke(serialNumber: Long, date: Date): Boolean

    interface CrlBuilder {
        fun editionTime(): Long

        fun build(): X509CRL
    }
}
