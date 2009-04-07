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
package org.identityconnectors.rw3270;

import java.lang.reflect.Constructor;

import org.identityconnectors.framework.common.exceptions.ConnectorException;


public class RW3270ConnectionFactory {
    private RW3270Configuration                 _config;
    private Constructor<RW3270Connection>       _constructor;
    

    public RW3270ConnectionFactory(RW3270Configuration     config) {
        try {
            _config = config;
            _constructor = (Constructor<RW3270Connection>)Class.forName(config.getConnectionClassName()).getConstructor(RW3270Configuration.class);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public RW3270Connection newConnection() throws Exception {
        try {
            RW3270Connection connection = _constructor.newInstance(_config);
            return connection;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
}
