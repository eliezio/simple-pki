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

import org.nordix.simplepki.application.port.out.PkiEntityRepository
import org.nordix.simplepki.domain.model.PkiEntity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import java.time.Clock

@Configuration
// Generic exceptions should never be thrown
class ApplicationConfig {

    @Bean
    fun clock(): Clock {
        return Clock.systemDefaultZone()
    }

    @Bean(name = ["ca"])
    @Lazy
    fun createCa(caRepository: PkiEntityRepository): PkiEntity {
        return caRepository.load()
    }
}