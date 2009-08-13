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
package org.identityconnectors.solaris.operation.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.operation.AbstractOp;


public class OpSearchImpl extends AbstractOp {
    
    private OperationOptions options;
    private ObjectClass oclass;
    
    public OpSearchImpl(Log log, SolarisConnector conn) {
        super(log, conn);
    }
    
    /**
     * Search operation
     * 
     * @param query
     *            can contain AND, OR ("&", "|") that represents and-ing and
     *            or-ing the result of matches. AND has priority over OR.
     */
    public void executeQuery(ObjectClass oclass, Node query,
            ResultsHandler handler, OperationOptions options) {
        this.options = options;
        this.oclass = oclass;
        
        // if i run the tests separately, the login info is in the expect4j's buffer
        // otherwise (when tests are run in batch), there is empty buffer, so this waitfor will timeout.
        try {
            getConnection().waitFor(
                    getConfiguration().getRootShellPrompt(),
                    SolarisConnection.WAIT);
        } catch (Exception ex) {
            // OK
        }
        
        //traverse through the tree of search query:
        Set<Uid> result = query.evaluate();
        for (Uid uid : result) {
            notifyHandler(handler, uid.getUidValue());
        }
    }

    /** calls handler with given string */
    private void notifyHandler(ResultsHandler handler, String uid) {
        // TODO this could be more complicated, when other than Name attributes arrive.
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
                .addAttribute(AttributeBuilder.build(Name.NAME, uid))
                .addAttribute(new Uid(uid));
        ConnectorObject co = builder.build();
        
        /*
         * return RETURNED_BY_DEFAULT attributes + attrsToGet
         */
        /** attributes to get */
        String[] attrsToGet = options.getAttributesToGet();
        if (attrsToGet == null) {
            attrsToGet = getReturnedByDefaultAttrs(getSchema());
        }
        
        //for (String attrName : attrsToGet) {
        //  // TODO fill the other attributes
        //}
        
        handler.handle(co);
    }

    /**
     * TODO this can be done in two ways: 
     * either you have a register of returned by default attributes in the connector, 
     * or you find it in the schema.
     * 
     * @param schema
     * @return set of attribute names that are returned by default
     */
    private String[] getReturnedByDefaultAttrs(Schema schema) {
        List<String> result = new ArrayList<String>();
        
        ObjectClassInfo ocinfo = schema.findObjectClassInfo(oclass.getObjectClassValue());
        Set<AttributeInfo> attrInfo = ocinfo.getAttributeInfo();
        for (AttributeInfo attributeInfo : attrInfo) {
            if (attributeInfo.isReturnedByDefault()) {
                result.add(attributeInfo.getName());
            }
        }
        
        return result.toArray(new String[0]);
    }
}
