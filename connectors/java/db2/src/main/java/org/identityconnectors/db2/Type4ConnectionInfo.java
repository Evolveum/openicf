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
 */
package org.identityconnectors.db2;

import org.identityconnectors.common.security.GuardedString;

final class Type4ConnectionInfo {
    private String driver;
    private String host;
    private String port;
    private String subprotocol;
    private String database;
    private String user;
    private GuardedString password;

    private Type4ConnectionInfo() {
    }

    String getDriver() {
        return driver;
    }

    String getHost() {
        return host;
    }

    String getPort() {
        return port;
    }

    String getSubprotocol() {
        return subprotocol;
    }

    String getDatabase() {
        return database;
    }

    String getUser() {
        return user;
    }

    GuardedString getPassword() {
        return password;
    }

    static class Type4ConnectionInfoBuilder {
        private String driver;
        private String host;
        private String port;
        private String subprotocol;
        private String database;
        private String user;
        private GuardedString password;

        String getDriver() {
            return driver;
        }

        Type4ConnectionInfoBuilder setDriver(String driver) {
            this.driver = driver;
            return this;
        }

        String getHost() {
            return host;
        }

        Type4ConnectionInfoBuilder setHost(String host) {
            this.host = host;
            return this;
        }

        String getPort() {
            return port;
        }

        Type4ConnectionInfoBuilder setPort(String port) {
            this.port = port;
            return this;
        }

        String getSubprotocol() {
            return subprotocol;
        }

        Type4ConnectionInfoBuilder setSubprotocol(String subprotocol) {
            this.subprotocol = subprotocol;
            return this;
        }

        String getDatabase() {
            return database;
        }

        Type4ConnectionInfoBuilder setDatabase(String database) {
            this.database = database;
            return this;
        }

        String getUser() {
            return user;
        }

        Type4ConnectionInfoBuilder setUser(String user) {
            this.user = user;
            return this;
        }

        GuardedString getPassword() {
            return password;
        }

        Type4ConnectionInfoBuilder setPassword(GuardedString password) {
            this.password = password;
            return this;
        }

        Type4ConnectionInfo build() {
            Type4ConnectionInfo info = new Type4ConnectionInfo();
            info.database = database;
            info.driver = driver;
            info.host = host;
            info.password = password;
            info.port = port;
            info.subprotocol = subprotocol;
            info.user = user;
            return info;
        }
    }

}
