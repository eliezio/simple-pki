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
package org.nordix.simplepki.services

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.junit.rules.ExternalResource
import org.slf4j.LoggerFactory

import static org.slf4j.Logger.ROOT_LOGGER_NAME

class EventCatcher extends ExternalResource {

    private ListAppender appender

    @Override
    protected void before() throws Throwable {
        def rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME)
        rootLogger.setLevel(Level.INFO)
        appender = new ListAppender()
        rootLogger.addAppender(appender)
        appender.start()
    }

    @Override
    protected void after() {
        appender.stop()
        def rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME)
        rootLogger.detachAppender(appender)
        rootLogger.setLevel(Level.WARN)
    }

    Map getOnlyEvent() {
        def list = appender.list
        assert list.size() == 1
        def evt = list.get(0)
        return evt
    }

    private static class ListAppender extends AppenderBase<ILoggingEvent> {

        List<Map> list = new ArrayList<>()

        protected void append(ILoggingEvent e) {
            String msg = e.getFormattedMessage()
            if (msg.startsWith('evt=')) {
                def map = msg.split(' ').collectEntries { part -> part.split('=', 2) }
                list.add(map)
            }
        }
    }
}
