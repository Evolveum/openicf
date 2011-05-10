/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright ¬© 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openicf.openportal;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.common.StringUtil;

//import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the OpenPortal Connector.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OpenPortalConfiguration extends AbstractConfiguration {


    static final int DEFAULT_PORT = 80;
    static final int SSL_PORT = 443;

    // Exposed configuration properties.

    /**
     * The portal server to connect to.
     */
    private String host;

    /**
     * The port the server is listening on.
     */
    private int port = DEFAULT_PORT;

    /**
     * Whether the port is a secure SSL port.
     */
    private boolean ssl = false;


    private String remoteUser = null;

    private GuardedString password = null;

    private String wsdlFile = null;

    /**
     * Constructor
     */
    public OpenPortalConfiguration() {

    }

    @ConfigurationProperty(displayMessageKey = "WSDL_FILE_PROPERTY_DISPLAY", helpMessageKey = "WSDL_FILE_PROPERTY_HELP", confidential = false)
    public String getWsdlFile() {
        return wsdlFile;
    }

    public void setWsdlFile(String wsdlFile) {
        this.wsdlFile = wsdlFile;
    }

    @ConfigurationProperty(displayMessageKey = "HOST_PROPERTY_DISPLAY", helpMessageKey = "HOST_PROPERTY_HELP", confidential = false)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @ConfigurationProperty(displayMessageKey = "PORT_PROPERTY_DISPLAY", helpMessageKey = "PORT_PROPERTY_HELP", confidential = false)
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @ConfigurationProperty(displayMessageKey = "IS_SSL_PROPERTY_DISPLAY", helpMessageKey = "IS_SSL_PROPERTY_HELP", confidential = false)
    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @ConfigurationProperty(displayMessageKey = "REMOTE_USER_PROPERTY_DISPLAY", helpMessageKey = "REMOTE_USER_PROPERTY_HELP", confidential = false)
    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    @ConfigurationProperty(displayMessageKey = "PASSWORD_PROPERTY_DISPLAY", helpMessageKey = "PASSWORD_PROPERTY_HELP", confidential = true)
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
        if(ssl){
            if (StringUtil.isBlank(remoteUser)) {
            throw new IllegalArgumentException("Remote User cannot be null or empty.");
            }
            if (null == password) {
            throw new IllegalArgumentException("Password cannot be null.");
            }
        }

        if (StringUtil.isBlank(host)) {
            throw new IllegalArgumentException("Host cannot be null or empty.");
        }

        if (0 > port || port > 65535) {
            throw new IllegalArgumentException("Port must be in range [0..65535]");
        }
        /*if(StringUtil.isBlank(wsdlFile)){
            throw new IllegalArgumentException("WsdlFile cannot be null or empty.");
        }*/
        try {
            getUrl();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Service URL is malformed.", e);
        }
    }

    public URL getUrl() throws MalformedURLException {
        StringBuffer sb = new StringBuffer("http://");//ssl ? new StringBuffer("https://") : new StringBuffer("http://");

        if (ssl) {
            // Authenticated url
            sb.append(remoteUser).append(":").append("1234").append("@").append(host).append(":").append(port).append("/tunnel-web/secure/axis");
        } else if(!ssl){
            // Unathenticated url
            sb.append(host).append(":").append(port).append("/tunnel-web/axis");
        }
        if(!StringUtil.isBlank(wsdlFile))
            sb.append("/").append(wsdlFile).append("?wsdl");
        return new URL(sb.toString());
    }

}
