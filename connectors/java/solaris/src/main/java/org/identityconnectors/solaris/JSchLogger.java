/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package org.identityconnectors.solaris;

import org.identityconnectors.common.logging.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;

/**
 * @author semancik
 *
 */
public class JSchLogger implements Logger {

    // Note that this is using class name of the JSch library
    private final Log log = Log.getLog(JSch.class);

    @Override
    public boolean isEnabled(int level) {
        switch (level) {
        case Logger.DEBUG:
            return log.isOk();
        case Logger.INFO:
            return log.isInfo();
        case Logger.WARN:
            return log.isWarning();
        case Logger.ERROR:
            return log.isError();
        case Logger.FATAL:
            return log.isError();
        default:
            throw new IllegalArgumentException("Unknown log level " + level);
        }
    }

    @Override
    public void log(int level, String message) {
        log.log(toMyLevel(level), null, message);
    }

    private Log.Level toMyLevel(int level) {
        switch (level) {
        case Logger.DEBUG:
            return Log.Level.OK;
        case Logger.INFO:
            return Log.Level.INFO;
        case Logger.WARN:
            return Log.Level.WARN;
        case Logger.ERROR:
            return Log.Level.ERROR;
        case Logger.FATAL:
            return Log.Level.ERROR;
        default:
            throw new IllegalArgumentException("Unknown log level " + level);
        }
    }

}
