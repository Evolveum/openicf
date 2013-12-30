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

import groovyx.net.http.RESTClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConnection;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.apache.commons.codec.binary.Base64;

/**
 * Class to represent a ScriptedREST Connection.
 *
 * @author Gael Allioux <gael.allioux@gmail.com>
 * @version 1.1.0.0
 */
public class ScriptedRESTConnection implements ScriptedConnection {

    private ScriptedRESTConfiguration configuration;
    private RESTClient restClient;

    public enum AuthMethod {

        BASIC, BASIC_PREEMPTIVE, CERT, OAUTH
    }

    /**
     * Constructor of ScriptedRESTConnection class.
     *
     * @param configuration the actual {@link ScriptedRESTConfiguration}
     */
    public ScriptedRESTConnection(ScriptedRESTConfiguration configuration) {
        this.configuration = configuration;
        final String login = configuration.getLogin();
        try {
            restClient = new RESTClient();
            restClient.setUri(new URI(configuration.getEndPoint()));
            restClient.setContentType(groovyx.net.http.ContentType.valueOf(configuration.getDefaultContentType()));
            switch (AuthMethod.valueOf(configuration.getDefaultAuthMethod())) {
                case BASIC:
                    configuration.getPassword().access(new GuardedString.Accessor() {
                        @Override
                        public void access(char[] clearChars) {
                            restClient.getAuth().basic(login, new String(clearChars));
                        }
                    });
                    break;
                case BASIC_PREEMPTIVE:
                    final StringBuilder baseAuth = new StringBuilder();
                    final HashMap<String, String> header = new HashMap<String, String>();
                    configuration.getPassword().access(new GuardedString.Accessor() {
                        @Override
                        public void access(char[] clearChars) {
                            baseAuth.append(Base64.encodeBase64String((login + ':' + new String(clearChars)).getBytes()));
                            baseAuth.insert(0, "Basic ");
                            header.put("Authorization",baseAuth.toString());
                            restClient.setHeaders(header);
                        }
                    });
                    break;
                case CERT:
                    break;
                case OAUTH:
            }
        } catch (URISyntaxException ex) {
            throw ConfigurationException.wrap(ex);
        }
    }

    /**
     * Release internal resources.
     */
    @Override
    public void dispose() {
        restClient.shutdown();
    }

    /**
     * If internal connection is not usable, throw IllegalStateException.
     */
    @Override
    public void test() {
    }

    /**
     * The connection needs to provide a generic handler object
     * that will be used by every script to connect to the remote system
     * 
     * @return the connection handler generic Object
     */
    @Override
    public Object getConnectionHandler() {
        return restClient;
    }
}
