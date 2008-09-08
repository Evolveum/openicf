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
package org.identityconnectors.framework.impl.api.local.operations;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.framework.api.operations.SyncApiOp;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.operations.SyncOp;


public class SyncImpl extends ConnectorAPIOperationRunner implements SyncApiOp {

    public SyncImpl(final ConnectorOperationalContext context,
            final Connector connector) {
        super(context, connector);
    }

    public void sync(ObjectClass objClass, SyncToken token,
            SyncResultsHandler handler, OperationOptions options) {
        // token is allowed to be null, objClass and handler must not be null
        Assertions.nullCheck(objClass, "objClass");
        Assertions.nullCheck(handler, "handler");
        // convert null into empty
        if (options == null) {
            options = new OperationOptionsBuilder().build();
        }
        // add a handler in the chain to remove attributes
        String[] attrsToGet = options.getAttributesToGet();
        if (attrsToGet != null && attrsToGet.length > 0) {
            handler = new AttributesToGetSyncResultsHandler(handler, attrsToGet);
        }
        //chain a normalizing results handler
        final ObjectNormalizerFacade normalizer =
            getNormalizer(objClass);
        handler = new NormalizingSyncResultsHandler(handler,normalizer);
        ((SyncOp) getConnector()).sync(objClass, token, handler, options);
    }

    /**
     * Simple handler to reduce the attributes to only the set of attribute to
     * get.
     */
    public static class AttributesToGetSyncResultsHandler extends
            AttributesToGetResultsHandler implements SyncResultsHandler {

        private final SyncResultsHandler _handler;

        public AttributesToGetSyncResultsHandler(SyncResultsHandler handler,
                String[] attrsToGet) {
            super(attrsToGet);
            _handler = handler;
        }

        public boolean handle(SyncDelta delta) {
            SyncDeltaBuilder bld = new SyncDeltaBuilder();
            bld.setUid(delta.getUid());
            bld.setToken(delta.getToken());
            bld.setDeltaType(delta.getDeltaType());
            if ( delta.getObject() != null ) {
                bld.setObject(reduceToAttrsToGet(delta.getObject()));
            }
            return _handler.handle(bld.build());
        }
    }
}
