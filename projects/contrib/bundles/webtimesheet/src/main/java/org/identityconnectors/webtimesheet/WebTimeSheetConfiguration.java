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
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.webtimesheet;

import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.common.StringUtil;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the WebTimeSheet Connector.
 *
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli Secundus Inc.</a>
 * @version 1.0
 * @since 1.0
 */
public class WebTimeSheetConfiguration extends AbstractConfiguration {

    /*
     * Set up base configuration elements
     */
    private String wtsURL = null;
    private String appPassword = null;
    private String adminUid = null;
    private String adminPassword = null;
    private String appName = null;

    /**
     * Constructor
     */
    public WebTimeSheetConfiguration() {
    }

    /**
     * Accessor for the wtsURL property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_URL_PROPERTY_DISPLAY", helpMessageKey = "WTS_URL_PROPERTY_HELP", confidential = false)
    public String getURLProperty() {
        return wtsURL;
    }

    /**
     * Accessor for the adminUid property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_ADMIN_UID_PROPERTY_DISPLAY", helpMessageKey = "WTS_ADMIN_UID_PROPERTY_HELP", confidential = false)
    public String getAdminUidProperty() {
        return adminUid;
    }

    /**
     * Accessor for the adminPassword property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_ADMIN_PWD_PROPERTY_DISPLAY", helpMessageKey = "WTS_ADMIN_PWD_PROPERTY_HELP", confidential = true)
    public String getAdminPasswordProperty() {
        return adminPassword;
    }

    /**
     * Accessor for the appPassword property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_APP_PWD_PROPERTY_DISPLAY", helpMessageKey = "WTS_APP_PWD_PROPERTY_HELP", confidential = true)
    public String getAppPasswordProperty() {
        return appPassword;
    }

    /**
     * Accessor for the appName property.
     */
    @ConfigurationProperty(displayMessageKey = "WTS_APP_NAME_PROPERTY_DISPLAY", helpMessageKey = "WTS_APP_NAME_PROPERTY_HELP", confidential = false)
    public String getAppNameProperty() {
        return appName;
    }

    /**
     * Setter for the wtsURL property.
     */
    public void setURLProperty(String url) {
        this.wtsURL = url;
    }

    /**
     * Setter for the appPassword property.
     */
    public void setAppPasswordProperty(String applicationPassword) {
        this.appPassword = applicationPassword;
    }

    /**
     * Setter for the appName property.
     */
    public void setAppNameProperty(String applicationName) {
        this.appName = applicationName;
    }

    /**
     * Setter for the adminPassword property.
     */
    public void setAdminPasswordProperty(String administratorPassword) {
        this.adminPassword = administratorPassword;
    }

    /**
     * Setter for the adminUid property.
     */
    public void setAdminUidProperty(String administratorUid) {
        this.adminUid = administratorUid;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtil.isBlank(wtsURL)) {
            throw new IllegalArgumentException("URL cannot be null or empty.");
        }
        if (StringUtil.isBlank(appPassword)) {
            throw new IllegalArgumentException("App Password cannot be null or empty.");
        }
        if (StringUtil.isBlank(appName)) {
            throw new IllegalArgumentException("App Name cannot be null or empty.");
        }
        if (StringUtil.isBlank(adminPassword)) {
            throw new IllegalArgumentException("Admin Password cannot be null or empty.");
        }
        if (StringUtil.isBlank(adminUid)) {
            throw new IllegalArgumentException("Admin UID cannot be null or empty.");
        }
    }
}
