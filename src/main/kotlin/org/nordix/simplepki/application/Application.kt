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

import org.bouncycastle.crypto.CryptoServicesRegistrar
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.security.SecureRandom
import java.security.Security

@SpringBootApplication
@ComponentScan("org.nordix.simplepki")
@EnableJpaRepositories(basePackages = ["org.nordix.simplepki.adapters.db"])
@EntityScan(basePackages = ["org.nordix.simplepki.adapters.db"])
@EnableConfigurationProperties(KeyStoreConfig::class)
class Application

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    bouncyCastleInitialize()
    SpringApplication.run(Application::class.java, *args)
}

private fun bouncyCastleInitialize() {
    Security.addProvider(BouncyCastleProvider())
    CryptoServicesRegistrar.setSecureRandom(SecureRandom())
}