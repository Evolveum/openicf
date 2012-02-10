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
package org.forgerock.openicf.salesforce;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.restlet.data.Form;


/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Salesforce Connector.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a herf="http://wiki.developerforce.com/page/Digging_Deeper_into_OAuth_2.0_on_Force.com">
 *      Digging Deeper into OAuth 2.0 on Force.com</a>
 */
public class SalesforceConfiguration extends AbstractConfiguration {


    public final static String LOGIN_URL = "https://login.salesforce.com/services/oauth2/token";

    // Exposed configuration properties.

    /**
     * The Consumer Key
     */
    private String clientId = null;

    /**
     * The Callback URL
     */
    private String redirect_uri = null;

    /**
     * The Consumer Secret
     */
    private GuardedString clientSecret = null;

    /**
     * The Username to authenticate with..
     */
    private String username;

    /**
     * The Password to authenticate with.
     */
    private GuardedString password = null;

    /**
     *
     */
    private int threadPoolSize = 50;


    /**
     * The Password to authenticate with.
     * <p/>
     * When accessing salesforce.com from outside of your company’s trusted networks, you must add a security token
     * to your password to log in to a desktop client, such as Connect for Outlook, Connect Offline, Connect for Office,
     * Connect for Lotus Notes, or the Data Loader.
     */
    private GuardedString security_token = null;

    /**
     * Constructor
     */
    public SalesforceConfiguration() {

    }

    @ConfigurationProperty(order = 1, displayMessageKey = "CLIENTID_PROPERTY_DISPLAY",
            helpMessageKey = "CLIENTID_PROPERTY_HELP", required = true, confidential = false)
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String remoteUser) {
        this.clientId = remoteUser;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "CLIENTSECRET_PROPERTY_DISPLAY",
            helpMessageKey = "CLIENTSECRET_PROPERTY_HELP", required = true, confidential = true)
    public GuardedString getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(GuardedString password) {
        this.clientSecret = password;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "USERNAME_PROPERTY_DISPLAY",
            helpMessageKey = "USERNAME_PROPERTY_HELP", required = true)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @ConfigurationProperty(order = 4, displayMessageKey = "PASSWORD_PROPERTY_DISPLAY",
            helpMessageKey = "PASSWORD_PROPERTY_HELP", required = true, confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(order = 5, displayMessageKey = "SECURITY_TOKEN_PROPERTY_DISPLAY",
            helpMessageKey = "SECURITY_TOKEN_PROPERTY_HELP", confidential = true)
    public GuardedString getSecurityToken() {
        return security_token;
    }

    public void setSecurityToken(GuardedString security_token) {
        this.security_token = security_token;
    }

    @ConfigurationProperty(order = 6, displayMessageKey = "THREAD_POOL_SIZE_DISPLAY", helpMessageKey = "THREAD_POOL_SIZE_HELP")
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /*@ConfigurationProperty(order = 6, displayMessageKey = "REDIRECT_URI_PROPERTY_DISPLAY",
            helpMessageKey = "REDIRECT_URI_PROPERTY_HELP", required = true)
    public String getRedirectUri() {
        return redirect_uri;
    }

    public void setRedirectUri(String host) {
        this.redirect_uri = host;
    }*/

    /**
     * {@inheritDoc}
     */
    public void validate() {
        Assertions.blankCheck(clientId, "clientId");
        Assertions.nullCheck(clientSecret, "clientSecret");
        Assertions.blankCheck(username, "username");
        Assertions.nullCheck(password, "password");
    }

    public Form getAuthenticationForm() {
        final StringBuilder clear = new StringBuilder();
        GuardedString.Accessor accessor = new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                clear.append(clearChars);
            }
        };

        Form form = new Form();
        form.add(SalesforceConnection.GRANT_TYPE, SalesforceConnection.PASSWORD);
        form.add(SalesforceConnection.USERNAME, getUsername());

        getPassword().access(accessor);
        if (null != getSecurityToken()) {
            getSecurityToken().access(accessor);
        }
        form.add(SalesforceConnection.PASSWORD, clear.toString());

        clear.setLength(0);

        getClientSecret().access(accessor);
        form.add(SalesforceConnection.CLIENT_ID, getClientId());
        form.add(SalesforceConnection.CLIENT_SECRET, clear.toString());

        return form;
    }

}
