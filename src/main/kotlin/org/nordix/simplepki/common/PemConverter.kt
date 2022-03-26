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
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator
import org.bouncycastle.util.io.pem.PemWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

object PemConverter {

    // TODO: it's not really generic. Only works for X509Certificate
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    @Throws(IOException::class, CertificateException::class)
    fun <T> fromPem(reader: Reader, clazz: Class<T>): T {
        val pemParser = PEMParser(reader)
        val obj: Any = pemParser.readObject() ?: throw IllegalArgumentException("No PEM data was found")
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj)
        }
        if (clazz == X509Certificate::class.java) {
            return JcaX509CertificateConverter().getCertificate(obj as X509CertificateHolder) as T
        }
        throw IllegalArgumentException(
            String.format(
                "An %s object was expected, whereas a %s object was found",
                clazz.simpleName, obj.javaClass.simpleName
            )
        )
    }

    // TODO: is its usage really generic?
    @JvmStatic
    @Throws(IOException::class)
    fun toPem(obj: Any): String {
        val baos = ByteArrayOutputStream()
        PemWriter(OutputStreamWriter(baos, StandardCharsets.US_ASCII)).use { writer ->
            writer.writeObject(
                JcaMiscPEMGenerator(obj).generate()
            )
        }
        return baos.toString(StandardCharsets.US_ASCII)
    }
}
