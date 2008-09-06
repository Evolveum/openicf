/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.framework.impl.api;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.impl.api.local.ConnectorPoolManager;
import org.identityconnectors.framework.impl.api.local.LocalConnectorFacadeImpl;
import org.identityconnectors.framework.impl.api.local.LocalConnectorInfoImpl;
import org.identityconnectors.framework.impl.api.remote.RemoteConnectorFacadeImpl;


public class ConnectorFacadeFactoryImpl extends ConnectorFacadeFactory {
    /**
     * Logging..
     */
    private static Log log = Log.getLog(ConnectorFacadeFactoryImpl.class);




    /**
     * {@inheritDoc}
     */
    public ConnectorFacade newInstance(APIConfiguration config) {
        ConnectorFacade ret = null;
        APIConfigurationImpl impl = (APIConfigurationImpl) config;
        AbstractConnectorInfo connectorInfo = impl.getConnectorInfo();
        if ( connectorInfo instanceof LocalConnectorInfoImpl ) {
            LocalConnectorInfoImpl localInfo =
                (LocalConnectorInfoImpl)connectorInfo;
            try {
                // create a new Provisioner..
                ret = new LocalConnectorFacadeImpl(localInfo,impl);
                
            } catch (Exception ex) {
                String connector = impl.getConnectorInfo().getConnectorKey().toString();
                log.error(ex, "Failed to create new connector facade: {0}, {1}",
                        connector, config);
                throw ConnectorException.wrap(ex);
            }
        }
        else {
            ret = new RemoteConnectorFacadeImpl(impl);
        }
        return ret;
    }

    
    /**
     * Dispose of all object pools and other resources associated with this
     * class.
     */
    public void dispose() {
        ConnectorPoolManager.dispose();
    }

}
