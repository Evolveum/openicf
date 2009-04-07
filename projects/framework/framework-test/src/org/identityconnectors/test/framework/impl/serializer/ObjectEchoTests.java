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
package org.identityconnectors.test.framework.impl.serializer;

import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.RemoteFrameworkConnectionInfo;
import org.identityconnectors.framework.impl.api.remote.RemoteFrameworkConnection;
import org.identityconnectors.framework.impl.api.remote.messages.EchoMessage;


public class ObjectEchoTests extends ObjectSerializationTests {
    @Override
    protected Object cloneObject(Object o) {
        //TODO: figure out how to automate these tests
        if ( true ) {
            return super.cloneObject(o);
        }
        else {
            EchoMessage message = new EchoMessage(o,null);
            RemoteFrameworkConnectionInfo info = 
                new RemoteFrameworkConnectionInfo("127.0.0.1",8759,
                        new GuardedString("changeit".toCharArray()));
            RemoteFrameworkConnection conn = 
                new RemoteFrameworkConnection(info);
            try {
                conn.writeObject(CurrentLocale.get());
                conn.writeObject(info.getKey());
                conn.writeObject(message);
                EchoMessage clone = (EchoMessage)conn.readObject();
                return clone.getObject();
            }
            finally {
                conn.close();
            }
        }
    }
}
