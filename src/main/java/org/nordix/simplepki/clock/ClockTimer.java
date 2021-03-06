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
package org.nordix.simplepki.clock;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Clock;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class ClockTimer implements Timer {

    private final Clock clock;
    private final long startTime;

    public ClockTimer(Clock clock) {
        this.clock = clock;
        this.startTime = clock.millis();
    }

    @Override
    public long elapsed() {
        return clock.millis() - startTime;
    }
}
