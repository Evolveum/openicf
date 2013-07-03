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

final class Type2ConnectionInfo {
    private String driver;
    private String aliasName;
    private String subprotocol;
    private String user;
    private GuardedString password;

    private Type2ConnectionInfo() {
    }

    String getDriver() {
        return driver;
    }

    String getAliasName() {
        return aliasName;
    }

    String getSubprotocol() {
        return subprotocol;
    }

    String getUser() {
        return user;
    }

    GuardedString getPassword() {
        return password;
    }

    static class Type2ConnectionInfoBuilder {
        private String driver;
        private String aliasName;
        private String subprotocol;
        private String user;
        private GuardedString password;

        String getDriver() {
            return driver;
        }

        Type2ConnectionInfoBuilder setDriver(String driver) {
            this.driver = driver;
            return this;
        }

        String getAliasName() {
            return aliasName;
        }

        Type2ConnectionInfoBuilder setAliasName(String aliasName) {
            this.aliasName = aliasName;
            return this;
        }

        String getSubprotocol() {
            return subprotocol;
        }

        Type2ConnectionInfoBuilder setSubprotocol(String subprotocol) {
            this.subprotocol = subprotocol;
            return this;
        }

        String getUser() {
            return user;
        }

        Type2ConnectionInfoBuilder setUser(String user) {
            this.user = user;
            return this;
        }

        GuardedString getPassword() {
            return password;
        }

        Type2ConnectionInfoBuilder setPassword(GuardedString password) {
            this.password = password;
            return this;
        }

        Type2ConnectionInfo build() {
            Type2ConnectionInfo info = new Type2ConnectionInfo();
            info.aliasName = aliasName;
            info.driver = driver;
            info.password = password;
            info.subprotocol = subprotocol;
            info.user = user;
            return info;
        }

    }
}
