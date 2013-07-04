/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock AS. All Rights Reserved
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
 */

package org.forgerock.openicf.connectors.tam;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the TAM Connector.
 *
 * @since 1.1
 */
public class TAMConfiguration extends AbstractConfiguration {

    /**
     * An Tivoli Access Manager user ID with the appropriate administrative
     * authority, such as sec_master.
     */
    private String adminUserID = null;
    /**
     * The password associated with the administrator user ID.
     */
    private GuardedString adminPassword = null;
    /**
     * The uniform resource locator (URL) to the configuration file created by
     * the Java SvrSslCfg class. The URL must use the file:/// format.
     *
     * Note: Do not use the svrsslcfg command-line interface to create a
     * configuration file that is to be used by a Java application. Example:
     * file:///C:\dev\jdk\PolicyDirector\tam.conf
     *
     */
    private String configurationFileURL = "file:///";
    private boolean certificateBased = false;
    /* syncing TAM GSO passwords */
    private boolean syncGSOCredentials = false;
    private boolean deleteFromRegistry = true;
    public static final String CONNECTOR_NAME = "AccessManagerConnector";

    /**
     * Constructor.
     */
    public TAMConfiguration() {
        StringBuilder cfile = new StringBuilder("file:///");
        cfile.append(System.getProperty("java.home"));
        cfile.append(System.getProperty("file.separator")).append("PolicyDirector");
        cfile.append(System.getProperty("file.separator")).append("tam.conf");
        configurationFileURL = cfile.toString();
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "TAM_CERT_AUTH_DISPLAY",
            helpMessageKey = "TAM_CERT_AUTH_HELP")
    public boolean isCertificateBased() {
        return certificateBased;
    }

    public void setCertificateBased(boolean certificateBased) {
        this.certificateBased = certificateBased;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "TAM_ADMIN_USER_DISPLAY",
            helpMessageKey = "TAM_ADMIN_USER_HELP", required = true)
    public String getAdminUserID() {
        return adminUserID;
    }

    public void setAdminUserID(String adminUser) {
        this.adminUserID = adminUser;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "TAM_ADMIN_PASSWORD_DISPLAY",
            helpMessageKey = "TAM_ADMIN_PASSWORD_HELP", confidential = true, required = true)
    public GuardedString getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(GuardedString adminPassword) {
        this.adminPassword = adminPassword;
    }

    @ConfigurationProperty(order = 4, displayMessageKey = "TAM_CONFIG_URL_DISPLAY",
            helpMessageKey = "TAM_CONFIG_URL_HELP", required = true)
    public String getConfigurationFileURL() {
        return configurationFileURL;
    }

    public void setConfigurationFileURL(String configUrl) {
        this.configurationFileURL = configUrl;
    }

    @ConfigurationProperty(order = 5, displayMessageKey = "TAM_DELETE_FROM_RGY_DISPLAY",
            helpMessageKey = "TAM_DELETE_FROM_RGY_HELP", required = true)
    public boolean isDeleteFromRegistry() {
        return deleteFromRegistry;
    }

    public void setDeleteFromRegistry(boolean deleteFromRegistry) {
        this.deleteFromRegistry = deleteFromRegistry;
    }

    @ConfigurationProperty(order = 6, displayMessageKey = "TAM_SYNC_GSO_CREDS_DISPLAY",
            helpMessageKey = "TAM_SYNC_GSO_CREDS_HELP", required = true)
    public boolean isSyncGSOCredentials() {
        return syncGSOCredentials;
    }

    public void setSyncGSOCredentials(boolean syncGSOCredentials) {
        this.syncGSOCredentials = syncGSOCredentials;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (!certificateBased) {
            if (StringUtil.isBlank(adminUserID)) {
                throw new IllegalArgumentException("Admin User ID can not be null or empty.");
            }
            if (null == adminPassword) {
                throw new IllegalArgumentException("Password can not be null or empty.");
            }
        }
        if (StringUtil.isBlank(configurationFileURL)) {
            throw new IllegalArgumentException("Configuration URL can not be null or empty.");
        } else {
            try {
                URL configfile = new URL(configurationFileURL);
                File f = new File(configfile.toURI());
                if (!f.exists()) {
                    throw new IllegalArgumentException("Configuration file does not exist");
                }
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException(ex);
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TAMConfiguration other = (TAMConfiguration) obj;
        if ((this.adminUserID == null) ? (other.adminUserID != null) : !this.adminUserID
                .equals(other.adminUserID)) {
            return false;
        }
        if (this.adminPassword != other.adminPassword
                && (this.adminPassword == null || !this.adminPassword.equals(other.adminPassword))) {
            return false;
        }
        if (this.certificateBased != other.certificateBased) {
            return false;
        }
        if ((this.configurationFileURL == null) ? (other.configurationFileURL != null)
                : !this.configurationFileURL.equals(other.configurationFileURL)) {
            return false;
        }
        if (this.syncGSOCredentials != other.syncGSOCredentials) {
            return false;
        }
        if (this.deleteFromRegistry != other.deleteFromRegistry) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.adminUserID != null ? this.adminUserID.hashCode() : 0);
        hash = 29 * hash + (this.adminPassword != null ? this.adminPassword.hashCode() : 0);
        hash = 29 * hash + (this.certificateBased ? 1 : 0);
        hash = 29 * hash + (this.configurationFileURL != null ? this.configurationFileURL.hashCode() : 0);
        hash = 29 * hash + (this.syncGSOCredentials ? 1 : 0);
        hash = 29 * hash + (this.deleteFromRegistry ? 1 : 0);
        return hash;
    }
}
