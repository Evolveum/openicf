/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openicf.os400;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;


/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the OS400 Connector.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OS400Configuration extends AbstractConfiguration {


    // Exposed configuration properties.

    /**
     * The connector to connect to.
     */
    private String host;

    /**
     * The Remote user to authenticate with.
     */
    private String remoteUser = null;

    /**
     * The Password to authenticate with.
     */
    private GuardedString password = null;

    /**
     * Select the check box to connect to the OS/400 server using SSL.
     */
    private boolean ssl = false;


    /**
     * Constructor
     */
    public OS400Configuration() {

    }


    @ConfigurationProperty(order = 1, displayMessageKey = "HOST_PROPERTY_DISPLAY", helpMessageKey = "HOST_PROPERTY_HELP",
            required = true, confidential = false)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }


    @ConfigurationProperty(order = 2, displayMessageKey = "REMOTE_USER_PROPERTY_DISPLAY",
            helpMessageKey = "REMOTE_USER_PROPERTY_HELP", required = true, confidential = false)
    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "PASSWORD_PROPERTY_DISPLAY",
            helpMessageKey = "PASSWORD_PROPERTY_HELP", confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(order = 4, displayMessageKey = "SSL_PROPERTY_DISPLAY",
            helpMessageKey = "SSL_PROPERTY_HELP", required = false, confidential = false)
    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtil.isBlank(remoteUser)) {
            throw new IllegalArgumentException("Remote User cannot be null or empty.");
        }
        if (StringUtil.isBlank(host)) {
            throw new IllegalArgumentException("Host cannot be null or empty.");
        }
    }

}
