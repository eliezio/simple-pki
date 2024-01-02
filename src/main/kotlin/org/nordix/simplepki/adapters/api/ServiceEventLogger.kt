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

import mu.KotlinLogging
import org.springframework.stereotype.Component
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val logger = KotlinLogging.logger {}

@Component
@WebFilter("/pki/**")
internal class ServiceEventLogger : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val startTime = System.currentTimeMillis()
        try {
            chain.doFilter(request, response)
        } finally {
            logRequest(request, response, startTime)
        }
    }

    private fun logRequest(
        request: ServletRequest,
        response: ServletResponse,
        startTime: Long
    ) {
        val req = request as HttpServletRequest
        if (!req.requestURI.startsWith("/actuator")) {
            val resp = response as HttpServletResponse
            logger.info(
                "evt=SERVICE method={} uri={} sc={} elapsed={}",
                req.method, req.requestURI, resp.status, System.currentTimeMillis() - startTime
            )
        }
    }
}
