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

import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.Reader
import java.io.StringWriter
import java.nio.charset.MalformedInputException
import java.security.cert.X509Certificate
import java.util.*

object PemConverter {

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> fromPem(reader: Reader, clazz: Class<T>): T {
        val pemParser = PEMParser(reader)
        val obj: Any = try {
            pemParser.readObject()
        } catch (e: MalformedInputException) {
            null
        } ?: throw IllegalArgumentException("No PEM data was found")
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj)
        }
        if (clazz == X509Certificate::class.java) {
            return JcaX509CertificateConverter().getCertificate(obj as X509CertificateHolder) as T
        }
        throw IllegalArgumentException(
            String.format(Locale.getDefault(Locale.Category.FORMAT),
                "An %s object was expected, whereas a %s object was found",
                clazz.simpleName, obj.javaClass.simpleName
            )
        )
    }

    @JvmStatic
    fun toPem(obj: Any): String {
        val stringWriter = StringWriter()
        JcaPEMWriter(stringWriter).also {
            it.writeObject(obj)
            it.flush()
        }
        return stringWriter.toString()
    }
}
