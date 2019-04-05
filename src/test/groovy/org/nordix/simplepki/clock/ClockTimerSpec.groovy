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
package org.nordix.simplepki.clock


import spock.lang.Specification

import java.time.Clock

class ClockTimerSpec extends Specification {

    def 'Timer::elapsed() reflects the Clock::millis() return sequence'() {
        given:
            def fixedClock = Stub(Clock) {
                millis() >>> [10, 27, 33]
            }
            def timer = new ClockTimer(fixedClock)

        expect:
            timer.elapsed() == 17
            timer.elapsed() == 23
    }
}
