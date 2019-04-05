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
package org.nordix.simplepki.persist;

import org.nordix.simplepki.crypto.RevocationEntry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface EndEntityRepository extends CrudRepository<EndEntity, Long> {
    default List<RevocationEntry> getAllRevocations() {
        return StreamSupport.stream(findAll().spliterator(), false)
            .flatMap(entry -> streamOfOptional(entry.revocationEntry()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Stream<T> streamOfOptional(Optional<T> opt) {
        // In Java 9 this would be replaced by the expression
        // opt.stream()
        return opt.map(Stream::of)
            .orElseGet(Stream::empty);
    }
}
