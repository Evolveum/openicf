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

import static org.identityconnectors.common.StringUtil.isBlank;



import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import java.net.MalformedURLException;
import java.net.URL;
import org.identityconnectors.common.logging.Log;

/**
 * Implements the {@link Configuration} interface to provide all the necessary
 * parameters to initialize the Google Apps Connector.
 * 
 * @author Warren Strange
 */
public class GoogleAppsConfiguration extends AbstractConfiguration {

    Log log = Log.getLog(GoogleAppsConnector.class);
    // =======================================================================
    // Connect  URL  
    // =======================================================================
    private String url;

    @ConfigurationProperty(order = 1, helpMessageKey = "ADMIN_URL_HELP", displayMessageKey = "ADMIN_URL_DISPLAY")
    public String getConnectionUrl() {
        return url;
    }

    /**
     * Set the connection URL for the domain
     * @param url - example: https://www.google.com/a/feeds/mydomain.com/
     */
    public void setConnectionUrl(String url) {
        this.url = url;
    }

    private String domain;

    /**
     * Return the domain name. Example: acme.com
     * @return domain name
     */
    @ConfigurationProperty(order = 2, helpMessageKey = "DOMAIN_HELP", displayMessageKey = "DOMAIN_DISPLAY")
    public String getDomain() {
        return this.domain;
    }

    /**
     * Set the domain
     * @param domain
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    // =======================================================================
    // Login - example: admin
    // =======================================================================
    private String login;

    @ConfigurationProperty(order = 3, helpMessageKey = "LOGIN_HELP", displayMessageKey = "LOGIN_DISPLAY")
    public String getLogin() {
        return this.login;
    }

    /**
     * Set the admin login id for the google apps domain
     * @param login - example: admin@mycompany.com
     */
    public void setLogin(String login) {
        this.login = login;
    }    // =======================================================================
    // Password
    // =======================================================================
    private String password;

    @ConfigurationProperty(order = 4, confidential = true)
    public String getPassword() {
        return this.password;
    }

    /**
     * Set the admin password
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
   

    // =======================================================================
    // Configuration Interface
    // =======================================================================
    /**
     * Attempt to validate the arguments added to the Configuration.
     * 
     * @see org.identityconnectors.framework.Configuration#validate()
     */
    public void validate() {
        log.info("validate url={0} login={1} domain={2}", getConnectionUrl(),
                getLogin(), getDomain());
        // determine if you can get a connection to the database..
        if (isBlank(getConnectionUrl())) 
            throw new IllegalArgumentException("Connection URL is mandatory");

        if (isBlank(getLogin())) 
            throw new IllegalArgumentException("Admin Login id is mandatory");

        if( getLogin().indexOf("@") >= 0 )
             throw new IllegalArgumentException("Admin Login must not contain @domain component");

        if( isBlank(getDomain()))
             throw new IllegalArgumentException("Domain name is mandatory");

         if( isBlank(getPassword()))
             throw new IllegalArgumentException("password is mandatory");


        try {
            new URL(getConnectionUrl());
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Malformed URL format", ex);
        }

    }

    public String toString() {
        return "GoogleAppsConfiguration( Url= " + getConnectionUrl() + " login= " + getLogin() +
                 " domain=" + getDomain() +")";
    }
}
