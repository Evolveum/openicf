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
package org.identityconnectors.spml;

import java.net.URL;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.openspml.v2.client.Spml2Client;


public class SpmlConnectionFactory {

    private static final Log log = Log.getLog(SpmlConnectionFactory.class);
    

    public static SpmlConnection newConnection(SpmlConfiguration configuration) {
        Spml2Client client = null;
        try {
            URL url = new URL(configuration.getUrl());
            GuardedStringAccessor accessor = new GuardedStringAccessor();
            configuration.getPassword().access(accessor);
            String password = new String(accessor.getArray());
            accessor.clear();
            //TODO: Spml2Client requires clear text password
            client = new Spml2Client(url.toExternalForm(), configuration.getUserName(), password);
            SpmlConnection connection = new SpmlConnection(client, configuration);
            log.info("created SPML connection to ''{0}''", configuration.getUrl());
            return connection;
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception e) {
            throw new ConnectorIOException(e);
        }
    }
}
