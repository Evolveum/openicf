/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
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
package org.forgerock.openicf.connectors.scriptedrest;

import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the ScriptedREST Connector.
 *
 * @author Gael Allioux <gael.allioux@gmail.com>
 * @version 1.1.0.0
 */
public class ScriptedRESTConfiguration extends ScriptedConfiguration {

    // Exposed configuration properties.
    /**
     * The Remote user to authenticate with.
     */
    private String login = null;
    /**
     * The Password to authenticate with.
     */
    private GuardedString password = null;

    /**
     * Constructor.
     */
    public ScriptedRESTConfiguration() {
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "remoteUser.display",
            groupMessageKey = "basic.group", helpMessageKey = "remoteUser.help",
            required = true, confidential = false)
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "password.display",
            groupMessageKey = "basic.group", helpMessageKey = "password.help",
            confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }
    // ===============================================
    // HTTP connection
    // ===============================================
    /*
     * Endpoint
     */
    private String endPoint = "http://localhost:8080/openidm";

    /**
     * Return the REST endpoint
     *
     * @return endpoint value
     */
    public String getEndPoint() {
        return endPoint;
    }

    /**
     * Set the REST endpoint
     *
     * @param endPoint
     */
    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }
    /*
     * Base path
     */
    private String basePath = "/openidm";

    /**
     * Return the REST base path
     *
     * @return endpoint value
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * Set the REST base path
     *
     * @param basePath
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    /*
     * default content type
     * Can be:
     *  ANY
     *  TEXT("text/plain")
     *  JSON("application/json","application/javascript","text/javascript")
     *  XML("application/xml","text/xml","application/xhtml+xml","application/atom+xml")
     *  HTML("text/html")
     *  URLENC("application/x-www-form-urlencoded")
     *  BINARY("application/octet-stream")
     */
    private String defaultContentType = "JSON";

    public void setDefaultContentType(String defaultContentType) {
        this.defaultContentType = defaultContentType;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }
    /*
     * authMethod
     * Can be:
     *  BASIC
     *  BASIC_PREEMPTIVE
     *  CERT
     *  OAUTH
     */
    private String defaultAuthMethod = "BASIC";

    public String getDefaultAuthMethod() {
        return defaultAuthMethod;
    }

    public void setDefaultAuthMethod(String defaultAuthMethod) {
        this.defaultAuthMethod = defaultAuthMethod;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        if (StringUtil.isBlank(login)) {
            throw new IllegalArgumentException("Remote User cannot be null or empty.");
        }
    }
}
