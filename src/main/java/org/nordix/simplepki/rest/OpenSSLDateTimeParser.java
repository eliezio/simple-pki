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
package org.nordix.simplepki.rest;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

@UtilityClass
class OpenSSLDateTimeParser {
    private static final DateTimeFormatter OPENSSL_DATE_TIME1 = DateTimeFormatter.ofPattern("MMM  d HH:mm:ss yyyy z");
    private static final DateTimeFormatter OPENSSL_DATE_TIME2 = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss yyyy z");

    static Optional<Long> parse(String value) {
        try {
            DateTimeFormatter formatter = (value.charAt(4) == ' ') ? OPENSSL_DATE_TIME1 : OPENSSL_DATE_TIME2;
            TemporalAccessor temporal = formatter.parse(value);
            return Optional.of(Instant.from(temporal).toEpochMilli());
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }
}
