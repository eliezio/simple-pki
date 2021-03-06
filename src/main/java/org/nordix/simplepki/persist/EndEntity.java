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
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import java.util.Date;
import java.util.Optional;

@Entity
@Data
public class EndEntity {
    @Id
    @GeneratedValue
    private long serialNumber;
    @Version
    private int version;
    private String subject;
    private Date notValidBefore;
    private Date notValidAfter;
    @Column(length = 4096)
    private String certificate;
    private Date revocationDate;
    private int revokedReason;

    public void revoke(Date date, int reason) {
        revocationDate = date;
        revokedReason = reason;
    }

    public boolean isRevoked() {
        return (revocationDate != null);
    }

    Optional<RevocationEntry> revocationEntry() {
        return isRevoked()
                ? Optional.of(new RevocationEntry(serialNumber, revocationDate, revokedReason))
                : Optional.empty();
    }
}
