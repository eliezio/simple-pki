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
package org.nordix.simplepki.common

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

object OpenSSLDateTimeParser {

    private val OPENSSL_DATE_TIME1: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM  d HH:mm:ss yyyy z")

    private val OPENSSL_DATE_TIME2: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss yyyy z")

    @JvmStatic
    fun parse(value: String): Long? {
        return try {
            val formatter: DateTimeFormatter = if (value[4] == ' ') OPENSSL_DATE_TIME1 else OPENSSL_DATE_TIME2
            val temporal: TemporalAccessor = formatter.parse(value)
            Instant.from(temporal).toEpochMilli()
        } catch (e: RuntimeException) {
            null
        }
    }
}
