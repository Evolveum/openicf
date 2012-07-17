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
 *
 * Portions Copyrighted 2012 ForgeRock Inc.
 *
 */
package org.forgerock.openicf.connectors.googleapps;

import java.net.MalformedURLException;
import java.net.URL;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the GoogleApps Connector.
 * 
 * @author $author$
 * @version $Revision$ $Date$
 */
public class GoogleAppsConfiguration extends AbstractConfiguration {

    private static final Log log = Log.getLog(GoogleAppsConnector.class);
    // Exposed configuration properties.

    /**
     * The connector to connect to.
     */
    private String connectionUrl = "https://apps-apis.google.com/a/feeds/";

    /**
     * The domain to connect to.
     */
    private String domain;

    /**
     * The Remote user to authenticate with.
     */
    private String login = null;

    /**
     * The Password to authenticate with.
     */
    private GuardedString password = null;

    /**
     * Constructor
     */
    public GoogleAppsConfiguration() {

    }

    @ConfigurationProperty(order = 1, displayMessageKey = "ADMIN_URL_DISPLAY",
            helpMessageKey = "ADMIN_URL_HELP", required = true)
    public String getConnectionUrl() {
        return connectionUrl;
    }

    /**
     * Set the connection URL for the domain
     * 
     * @param url
     *            - example: https://www.google.com/a/feeds/mydomain.com/
     */
    public void setConnectionUrl(String url) {
        this.connectionUrl = url;
    }

    /**
     * Return the domain name. Example: acme.com
     * 
     * @return domain name
     */
    @ConfigurationProperty(order = 2, helpMessageKey = "DOMAIN_HELP",
            displayMessageKey = "DOMAIN_DISPLAY", required = true)
    public String getDomain() {
        return this.domain;
    }

    /**
     * Set the domain
     * 
     * @param domain
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "LOGIN_DISPLAY",
            helpMessageKey = "LOGIN_HELP", required = true)
    public String getLogin() {
        return login;
    }

    /**
     * Set the admin login id for the google apps domain
     * 
     * @param login
     *            - example: admin
     */
    public void setLogin(String login) {
        this.login = login;
    }

    @ConfigurationProperty(order = 4, displayMessageKey = "PASSWORD_PROPERTY_DISPLAY",
            helpMessageKey = "PASSWORD_PROPERTY_HELP", required = true, confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        log.info("validate url={0} login={1} domain={2}", getConnectionUrl(), getLogin(),
                getDomain());
        Assertions.blankCheck(getConnectionUrl(), "connectionUrl");
        Assertions.blankCheck(getDomain(), "domain");
        Assertions.blankCheck(getLogin(), "domain");
        if (getLogin().indexOf("@") >= 0)
            throw new IllegalArgumentException("Admin Login must not contain @domain component");
        Assertions.nullCheck(getPassword(), "domain");
        try {
            new URL(getConnectionUrl());
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Malformed URL format", ex);
        }
    }

}
