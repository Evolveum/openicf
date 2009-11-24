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

package org.identityconnectors.peoplesoft;

import org.identityconnectors.common.security.*;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.peoplesoft.mapping.*;
import org.identityconnectors.peoplesoft.wrappers.*;

import psft.pt8.joa.*;


/**
 * @author kitko
 *
 */
public abstract class PeopleSoftAbstractConfiguration extends AbstractConfiguration {
    private String xmlMapping;
    private String host;
    private String port;
    private String user;
    private GuardedString password;
    private ComponentInterfaces mapping;
    private String mappingFactoryClassName;
    
    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public PeopleSoftAbstractConfiguration setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public PeopleSoftAbstractConfiguration setPort(String port) {
        this.port = port;
        return this;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public PeopleSoftAbstractConfiguration setUser(String user) {
        this.user = user;
        return this;
    }

    /**
     * @return the password
     */
    public GuardedString getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public PeopleSoftAbstractConfiguration setPassword(GuardedString password) {
        this.password = password;
        return this;
    }


    @Override
    public void validate() {
    }
    
    public String getXMLMapping(){
        return xmlMapping;
    }
    
    public void setXMLMapping(String mapping){
        this.xmlMapping = mapping;
    }
    
    public ComponentInterfaces getMapping(){
        if(mapping == null){
            try{
                ComponentInterfacesFactory factory = (ComponentInterfacesFactory)Class.forName(mappingFactoryClassName).newInstance();
                mapping = factory.createMapping(this);
            }
            catch(Exception e){
                throw new ConnectorException("Cannot createing peoplesoft mapping interface", e);
            }
        }
        return mapping;
    }
    
    public ISession craeteAdminConnection(){
        final ISession session = new SessionWrapper(API.createSession(false, 0));
        final String strPath = host + ":" + port;
        password.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                if (!session.connect(1, strPath, user, new String(clearChars), null)) {
                }
            }
        });
        return session;
    }

}
