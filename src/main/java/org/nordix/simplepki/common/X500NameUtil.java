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
package org.nordix.simplepki.common;

import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@UtilityClass
public class X500NameUtil {

    private static final List<ASN1ObjectIdentifier> DN_ORDER = Arrays.asList(
        BCStyle.CN,
        BCStyle.ST,
        BCStyle.OU,
        BCStyle.O,
        BCStyle.L,
        BCStyle.C
    );

    public static String canonicalSubjectName(X509Certificate certificate) {
        RDN[] rdNs = JcaX500NameUtil.getSubject(certificate).getRDNs();
        Arrays.sort(rdNs, Comparator.comparing(rdn -> rdn.getFirst().getType(),
            Comparator.comparing(X500NameUtil::dnOrderOf)
                .thenComparing(ASN1ObjectIdentifier::getId))
        );
        return new X500Name(rdNs).toString();
    }

    private static int dnOrderOf(ASN1ObjectIdentifier oid) {
        return DN_ORDER.indexOf(oid);
    }
}
