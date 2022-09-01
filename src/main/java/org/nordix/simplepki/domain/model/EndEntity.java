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
package org.nordix.simplepki.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Data
@Builder
@AllArgsConstructor
public class EndEntity {
    private long serialNumber;
    private int version;
    private String subject;
    private Date notValidBefore;
    private Date notValidAfter;
    private String certificate;
    private Date revocationDate;
    private int revokedReason;

    private static final Random secureRandom = new SecureRandom();

    public EndEntity() {
        this.serialNumber = secureRandom.nextLong() & Long.MAX_VALUE;
    }

    public void revoke(Date date, int reason) {
        revocationDate = date;
        revokedReason = reason;
    }

    public boolean isRevoked() {
        return (revocationDate != null);
    }

    public Optional<RevocationEntry> revocationEntry() {
        return isRevoked()
                ? Optional.of(new RevocationEntry(serialNumber, revocationDate, revokedReason))
                : Optional.empty();
    }
}
