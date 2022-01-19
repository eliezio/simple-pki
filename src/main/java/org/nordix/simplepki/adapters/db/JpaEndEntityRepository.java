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
package org.nordix.simplepki.adapters.db;

import lombok.AllArgsConstructor;
import org.nordix.simplepki.domain.model.EndEntity;
import org.nordix.simplepki.domain.model.RevocationEntry;
import org.nordix.simplepki.application.port.out.EndEntityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@AllArgsConstructor
class JpaEndEntityRepository implements EndEntityRepository {

    private final JpaEndEntityCrudRepository repository;

    @Override
    public Optional<EndEntity> findById(long serialNumber) {
        return repository.findById(serialNumber).map(EndEntityMapper.INSTANCE::fromJpa);
    }

    @Override
    public List<RevocationEntry> getAllRevocations() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
            .flatMap(entry -> EndEntityMapper.INSTANCE.fromJpa(entry).revocationEntry().stream())
                .collect(Collectors.toList());
    }

    @Override
    public void save(EndEntity entity) {
        repository.save(EndEntityMapper.INSTANCE.toJpa(entity));
    }
}
