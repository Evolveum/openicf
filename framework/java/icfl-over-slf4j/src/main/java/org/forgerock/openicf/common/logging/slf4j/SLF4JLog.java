/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 */
package org.forgerock.openicf.common.logging.slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.identityconnectors.common.logging.Log.Level;
import org.identityconnectors.common.logging.LogSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * 
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class SLF4JLog implements LogSpi {

    private volatile ConcurrentMap<String, Logger> map = new ConcurrentHashMap<String, Logger>(1);

    /**
     * Uses the SLF4J logger to log the message.
     * @see LogSpi#log(Class, Level, String, Throwable)
     */
    public void log(Class<?> clazz, String methodName, Level level, String message, Throwable ex) {
        String clazzName = clazz.getName();
        Logger logger = getSLF4JLogger(clazzName);

        if (logger instanceof LocationAwareLogger) {
            //StringBuilder sb = new StringBuilder("Method: {}\t")
            StringBuilder sb = new StringBuilder("Method: ").append(methodName).append("\t");
            sb.append(message);
            ( (LocationAwareLogger) logger ).log(null, clazz.getName(), getLogLevel(level), sb.toString(), null /*new Object[]{methodName}*/, ex);
        } else {
            StringBuilder sb = new StringBuilder("Class: {}\tMethod: {}\tMessage: ");
            sb.append(message);
            // uses different call if the exception is not null..
            if (Level.OK.equals(level)) {
                if (ex == null) {
                    logger.debug(sb.toString(), new Object[]{clazz, methodName});
                } else {
                    logger.debug(sb.toString(), new Object[]{clazz, methodName}, ex);
                }
            } else if (Level.INFO.equals(level)) {
                if (ex == null) {
                    logger.info(sb.toString(), new Object[]{clazz, methodName});
                } else {
                    logger.info(sb.toString(), new Object[]{clazz, methodName}, ex);
                }
            } else if (Level.WARN.equals(level)) {
                if (ex == null) {
                    logger.warn(sb.toString(), new Object[]{clazz, methodName});
                } else {
                    logger.warn(sb.toString(), new Object[]{clazz, methodName}, ex);
                }
            } else if (Level.ERROR.equals(level)) {
                if (ex == null) {
                    logger.error(sb.toString(), new Object[]{clazz, methodName});
                } else {
                    logger.error(sb.toString(), new Object[]{clazz, methodName}, ex);
                }
            }
        }
    }

    /**
     * Use the internal SLF4J logger to determine if the level is worthy of logging.
     */
    public boolean isLoggable(Class<?> clazz, Level level) {
        Logger logger = getSLF4JLogger(clazz.getName());
        boolean ret = true;
        if (Level.OK.equals(level)) {
            ret = logger.isDebugEnabled();
        } else if (Level.INFO.equals(level)) {
            ret = logger.isInfoEnabled();
        } else if (Level.WARN.equals(level)) {
            ret = logger.isWarnEnabled();
        } else if (Level.ERROR.equals(level)) {
            ret = logger.isErrorEnabled();
        }
        return ret;
    }

    private int getLogLevel(Level level) {
        int ret = LocationAwareLogger.TRACE_INT;
        if (Level.OK.equals(level)) {
            ret = LocationAwareLogger.DEBUG_INT;
        } else if (Level.INFO.equals(level)) {
            ret = LocationAwareLogger.INFO_INT;
        } else if (Level.WARN.equals(level)) {
            ret = LocationAwareLogger.WARN_INT;
        } else if (Level.ERROR.equals(level)) {
            ret = LocationAwareLogger.ERROR_INT;
        }
        return ret;
    }

    Logger getSLF4JLogger(String key) {
        Logger aLogger = map.get(key);
        if (aLogger == null) {
            aLogger = LoggerFactory.getLogger(key);
            Logger old = map.putIfAbsent(key, aLogger);
            aLogger = old != null ? old : aLogger;
        }
        return aLogger;
    }

    // Method for tests
    ConcurrentMap<String, Logger> getMap() {
        return map;
    }
}
