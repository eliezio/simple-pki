/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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
package org.nordix.simplepki.common

import org.bouncycastle.asn1.x500.X500Name
import spock.lang.Specification

class X500NameUtilSpec extends Specification {

    def 'must compute canonical name of RDNs with multiple OUs'() {
        given:
            def rdNs = new X500Name(originalSubject).getRDNs()

        when:
            def actual = X500NameUtil.INSTANCE.canonicalSubjectName(rdNs)

        then:
            assert actual == canonicalSubject

        where:
            originalSubject = "C=DE, ST=BY, L=MUNICH, O=MyCompany, OU=Organizations, OU=MyOU2, OU=MyOU1, OU=MyOU3, CN=vcsa"
            canonicalSubject = "CN=vcsa,ST=BY,OU=MyOU1,OU=MyOU2,OU=MyOU3,OU=Organizations,O=MyCompany,L=MUNICH,C=DE"
    }
}
