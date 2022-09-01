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

import org.nordix.simplepki.application.port.out.EndEntityRepository
import org.nordix.simplepki.domain.model.EndEntity
import org.nordix.simplepki.domain.model.RevocationEntry
import org.springframework.stereotype.Component
import java.util.*

@Component
internal class JpaEndEntityRepository(
    private val repository: JpaEndEntityCrudRepository,
) : EndEntityRepository {

    override fun findById(serialNumber: Long): Optional<EndEntity> {
        return repository.findById(serialNumber)
            .map { jpaEndEntity: JpaEndEntity ->
                EndEntityMapper.INSTANCE.fromJpa(jpaEndEntity)
            }
    }

    override fun allRevocations(): List<RevocationEntry> {
        return repository.findAll()
            .mapNotNull { entry ->
                EndEntityMapper.INSTANCE.fromJpa(entry).revocationEntry()
            }
    }

    override fun save(entity: EndEntity) {
        repository.save(EndEntityMapper.INSTANCE.toJpa(entity))
    }
}
