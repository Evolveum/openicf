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
 * $Id$
 */
package org.forgerock.openicf.webtimesheet;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;


/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the WebTimeSheet Connector.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class WebTimeSheetConfiguration extends AbstractConfiguration {

private String wtsHost = null;
    private String wtsPort = "443";
    private String wtsURI = null;
    
    //TODO modify to use the FetchRemoteApiUrl with company key to be more dynamic
    
    private String adminUid = null;
    private String adminPassword = null;
    //TODO change to a guarded string




    /**
     * Constructor
     */
    public WebTimeSheetConfiguration() {
    }

    /**
     * Accessor for the wtsURI property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_URL_PROPERTY_DISPLAY", helpMessageKey = "WTS_URL_PROPERTY_HELP", confidential = false)
    public String getWtsURI() {
        return wtsURI;
    }

    /**
     * Accessor for the adminUid property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_ADMIN_UID_PROPERTY_DISPLAY", helpMessageKey = "WTS_ADMIN_UID_PROPERTY_HELP", confidential = false)
    public String getAdminUid() {
        return adminUid;
    }

    /**
     * Accessor for the adminPassword property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_ADMIN_PWD_PROPERTY_DISPLAY", helpMessageKey = "WTS_ADMIN_PWD_PROPERTY_HELP", confidential = false)
    public String getAdminPassword() {
        return adminPassword;
    }

    /**
     * Accessor for the wtsHost property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_APP_PWD_PROPERTY_DISPLAY", helpMessageKey = "WTS_APP_PWD_PROPERTY_HELP", confidential = false)
    public String getWtsHost() {
        return wtsHost;
    }

    /**
     * Accessor for the wtsPort property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_APP_NAME_PROPERTY_DISPLAY", helpMessageKey = "WTS_APP_NAME_PROPERTY_HELP", confidential = false)
    public String getWtsPort() {
        return wtsPort;
    }

    /**
     * Setter for the wtsURI property.
     */
    public void setWtsURI(String wtsURI) {
        this.wtsURI = wtsURI;
    }

    /**
     * Setter for the wtsHost property.
     */
    public void setWtsHost(String wtsHost) {
        this.wtsHost = wtsHost;
    }

    /**
     * Setter for the wtsPort property.
     */
    public void setWtsPort(String wtsPort) {
        this.wtsPort = wtsPort;
    }

    /**
     * Setter for the adminPassword property.
     */
    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    /**
     * Setter for the adminUid property.
     */
    public void setAdminUid(String adminUid) {
        this.adminUid = adminUid;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtil.isBlank(wtsURI)) {
            throw new IllegalArgumentException("URI cannot be null or empty.");
        }
        if (StringUtil.isBlank(wtsHost)) {
            throw new IllegalArgumentException("Host cannot be null or empty.");
        }
        if (StringUtil.isBlank(wtsPort)) {
            throw new IllegalArgumentException("Port number cannot be null.");
        }
        if (StringUtil.isBlank(adminPassword)) {
            throw new IllegalArgumentException("Admin Password cannot be null or empty.");
        }
        if (StringUtil.isBlank(adminUid)) {
            throw new IllegalArgumentException("Admin UID cannot be null or empty.");
        }
    }

}
