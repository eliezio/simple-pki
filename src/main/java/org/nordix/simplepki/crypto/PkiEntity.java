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
package org.nordix.simplepki.crypto;

import lombok.Value;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * This Simple-PKI operations deals with two basic types of entities:
 * <ol>
 *     <li>CA;</li>
 *     <li>End-Entity (aka Non-CA).</li>
 * </ol>
 */
@Value
public class PkiEntity {
    private final PrivateKey privateKey;
    private final X509Certificate certificate;
}
