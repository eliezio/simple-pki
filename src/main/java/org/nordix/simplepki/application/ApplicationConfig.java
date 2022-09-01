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
package org.nordix.simplepki.application;

import lombok.RequiredArgsConstructor;
import org.nordix.simplepki.common.ClockTimer;
import org.nordix.simplepki.common.Timer;
import org.nordix.simplepki.domain.model.BasicPkiOperations;
import org.nordix.simplepki.domain.model.Pki;
import org.nordix.simplepki.domain.model.PkiEntity;
import org.nordix.simplepki.domain.model.PkiOperations;
import org.nordix.simplepki.domain.ports.EndEntityRepository;
import org.nordix.simplepki.domain.ports.SingleEntityRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import javax.inject.Provider;
import java.time.Clock;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
@RequiredArgsConstructor
@SuppressWarnings("squid:S00112")   // Generic exceptions should never be thrown
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public Timer timer(Clock clock) {
        return new ClockTimer(clock);
    }

    @Bean(name = "ca")
    @Lazy
    public PkiEntity createCa(SingleEntityRepository caRepository)
        throws Exception {
        return caRepository.load();
    }

    @Bean
    public PkiOperations pkiOperations(Clock clock) {
        return new BasicPkiOperations(clock);
    }

    @Bean
    public Pki pki(Provider<PkiEntity> caProvider, EndEntityRepository endEntityRepository, PkiOperations pkiOperations) {
        return new Pki(caProvider, endEntityRepository, pkiOperations);
    }
}