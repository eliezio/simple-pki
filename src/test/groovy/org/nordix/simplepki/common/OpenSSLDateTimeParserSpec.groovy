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

import spock.lang.Specification

class OpenSSLDateTimeParserSpec extends Specification {

    def 'converts OpenSSL date/time format to Epoch millis: #dtSpec'() {
        expect:
            OpenSSLDateTimeParser.parse(dtSpec) == expectedMillis

        where:
            // expected values calculated using https://www.epochconverter.com
            dtSpec                     | expectedMillis
            'Apr 12 18:37:08 2019 GMT' | 1555094228000
            'May  1 11:43:01 2019 GMT' | 1556710981000
            'May  2 14:03:27 2019 GMT' | 1556805807000
    }

    def 'dont convert if not formatted with OpenSSL date/time format: #dtSpec'() {
        expect:
            OpenSSLDateTimeParser.parse(dtSpec) == null

        where:
            // expected values calculated using https://www.epochconverter.com
            dtSpec << [
                'Apr',
                'Thu, 02 May 2019 14:03:27 GMT'
            ]
    }
}
