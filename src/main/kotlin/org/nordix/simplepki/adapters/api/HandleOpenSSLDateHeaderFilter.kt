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
package org.nordix.simplepki.adapters.api

import org.nordix.simplepki.common.OpenSSLDateTimeParser
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

@Component
internal class HandleOpenSSLDateHeaderFilter : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        chain.doFilter(HeaderMapRequestWrapper(request as HttpServletRequest), response)
    }

    private class HeaderMapRequestWrapper(request: HttpServletRequest) :
        HttpServletRequestWrapper(request) {
        override fun getDateHeader(name: String): Long {
            val value = super.getHeader(name)
            return value?.let { OpenSSLDateTimeParser.parse(it) } ?: super.getDateHeader(name)
        }
    }
}
