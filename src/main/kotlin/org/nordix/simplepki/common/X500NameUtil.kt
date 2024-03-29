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
package org.nordix.simplepki.common

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.AttributeTypeAndValue
import org.bouncycastle.asn1.x500.RDN
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import java.util.*

object X500NameUtil {

    private val DN_ORDER: List<ASN1ObjectIdentifier> = listOf(
        BCStyle.CN,
        BCStyle.ST,
        BCStyle.OU,
        BCStyle.O,
        BCStyle.L,
        BCStyle.C
    )

    fun canonicalSubjectName(rdNs: Array<RDN>): String {
        Arrays.sort(
            rdNs, Comparator.comparing(
                { rdn: RDN -> rdn.first!! },
                Comparator.comparing { atv: AttributeTypeAndValue -> dnOrderOf(atv.type) }
                    .thenComparing { atv: AttributeTypeAndValue -> atv.value.toString() }
            )
        )
        return X500Name(rdNs).toString()
    }

    private fun dnOrderOf(oid: ASN1ObjectIdentifier): Int = DN_ORDER.indexOf(oid)
}
