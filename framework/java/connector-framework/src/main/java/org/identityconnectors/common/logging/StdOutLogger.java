/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 * Portions Copyrighted 2014 ForgeRock AS.
 */
package org.identityconnectors.common.logging;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.identityconnectors.common.StringPrintWriter;
import org.identityconnectors.common.logging.Log.Level;

/**
 * Standard out logger. It logs all messages to STDOUT. The method
 * {@link LogSpi#isLoggable(Class, Level)} will always return true so currently
 * logging is not filtered.
 *
 * @author Will Droste
 * @since 1.0
 */
class StdOutLogger implements LogSpi {

    private static final String PATTERN =
            "Thread Id: {0}\tTime: {1}\tClass: {2}\tMethod: {3}\tLevel: {4}\tMessage: {5}";
    /**
     * Insures there is only one MessageFormat per thread since MessageFormat is
     * not thread safe.
     */
    private static final ThreadLocal<MessageFormat> MSG_FORMAT_HANDLER =
            new ThreadLocal<MessageFormat>() {

                @Override
                protected MessageFormat initialValue() {
                    return new MessageFormat(PATTERN);
                }
            };

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final ThreadLocal<DateFormat> DATE_FORMAT_HANDLER =
            new ThreadLocal<DateFormat>() {

                @Override
                protected DateFormat initialValue() {
                    return new SimpleDateFormat(DATE_PATTERN);
                }
            };

    /**
     * Logs the thread id, date, class, level, message, and optionally exception
     * stack trace to standard out.
     *
     * @see LogSpi#log(Class, String, Level, String, Throwable)
     */
    @Override
    public void log(final Class<?> clazz, final String methodName, final Level level,
                    final String message, final Throwable ex) {

        final String now;
        try {
            now = DATE_FORMAT_HANDLER.get().format(new Date());
        } finally {
            DATE_FORMAT_HANDLER.remove();
        }
        final Object[] args =
                new Object[]{Thread.currentThread().getId(), now, clazz.getName(), methodName,
                        level, message};

        final String msg;
        try {
            msg = MSG_FORMAT_HANDLER.get().format(args);
        } finally {
            MSG_FORMAT_HANDLER.remove();
        }

        final PrintStream out = Level.ERROR == level ? System.err : System.out;
        out.println(msg);

        if (ex != null) {
            final StringPrintWriter wrt = new StringPrintWriter();
            ex.printStackTrace(wrt);
            out.print(wrt.getString());
        }
    }

    public void log(final Class<?> clazz, final StackTraceElement caller, final Level level, final String message, final Throwable ex) {
        String methodName = null;
        if (null != caller) {
            // @formatter:off
            methodName = caller.getMethodName() +
                    (caller.isNativeMethod() ? "(Native Method)" :
                            (caller.getFileName() != null && caller.getLineNumber() >= 0 ?
                                    "(" + caller.getFileName() + ":" + caller.getLineNumber() + ")" :
                                    (caller.getFileName() != null ? "(" + caller.getFileName() + ")" : "(Unknown Source)")));
            // @formatter:on
        } else {
            methodName = "unknown";
        }
        log(clazz, methodName, level, message, ex);
    }

    /**
     * Always returns true.
     */
    @Override
    public boolean isLoggable(final Class<?> clazz, final Level level) {
        return true;
    }

    /**
     * Always returns true.
     */
    @Override
    public boolean needToInferCaller(Class<?> clazz, Level level) {
        return true;
    }
}
