/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.googleapps;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.Configuration;

/**
 * Implements the {@link Connection} interface to wrap Google Apps client connections.
 *
 * @author Warren Strange
 */
public class GoogleAppsConnection {

    // handle to our google apps client
    private GoogleAppsClient gapps;
    /**
     * Information from the {@link Configuration} can help determine how to test
     * the viability of the {@link Connection}.
     */
    final GoogleAppsConfiguration config;

    /**
     * Use the {@link Configuration} passed in to immediately connect to a
     * database. If the {@link Connection} fails a {@link RuntimeException} will
     * be thrown.
     *
     * @param config
     *            Configuration required to obtain a valid connection.
     * @throws RuntimeException
     *             iff there is a problem creating a {@link java.sql.Connection}.
     */
    public GoogleAppsConnection(GoogleAppsConfiguration config) {
        this.config = config;
        this.gapps = getClient(config);
    }

    /**
     * Get the internal Google Apps connection
     */
    public GoogleAppsClient getConnection() {
        return this.gapps;
    }

    /**
     * Gets a {@link Connection}
     */
    private GoogleAppsClient getClient(GoogleAppsConfiguration config) {
        // create the connection base on the configuration..
        GoogleAppsClient ret = null;
        try {
            // get the  URL..
            String url = config.getConnectionUrl();
            String login = config.getLogin();
            String password = config.getPassword();
            ret = new GoogleAppsClient(login, password, url);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        return ret;
    }

    public GoogleAppsClient getGoogleAppsClient() {
        return this.gapps;
    }

    /**
     * Closes the internal connection.
     *
     * Nothing really to do here
     *
     * @see org.identityconnectors.framework.Connection#dispose()
     */
    public void dispose() {
        gapps = null;
    }

    /**
     * Determines if the underlying google apps connection is valid
     *
     * @see org.identityconnectors.framework.spi.Connection#test()
     * @throws RuntimeException
     *             iff the underlying Google Apps connection is not
     *             valid otherwise do nothing.
     */
    public void test() {
        gapps.testConnection();
    }
}