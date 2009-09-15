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
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.constants.SolarisAttribute;
import org.identityconnectors.solaris.operation.AbstractOp;
import org.identityconnectors.solaris.operation.search.nodes.AttributeNode;
import org.identityconnectors.solaris.operation.search.nodes.BinaryOpNode;
import org.identityconnectors.solaris.operation.search.nodes.Node;
import org.identityconnectors.solaris.operation.search.nodes.UniversalNode;


public class OpSearchImpl extends AbstractOp {
    
    private OperationOptions options;
    private ObjectClass oclass;
    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT, ObjectClass.GROUP};
    /** names of returned by default attributes (given by schema, it is static during lifetime of the connector */
    private static String[] returnedByDefaultAttributeNames; // todo possibly this could be acquired right from connector attribute structures.
    
    public OpSearchImpl(Log log, SolarisConnector conn) {
        super(log, conn);
    }
    
    /**
     * Search operation
     * 
     * @param query contains the filters. Is created by {@link SolarisFilterTranslator}
     */
    public void executeQuery(ObjectClass oclass, Node query,
            ResultsHandler handler, OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(oclass, acceptOC, getClass());
        
        if (oclass.is(ObjectClass.GROUP_NAME)) {
            // TODO
            throw new UnsupportedOperationException();
        }
        
        this.options = options;
        this.oclass = oclass;
        
        if (query == null) {
            // NULL indicates that we should return all results.
            query = new UniversalNode();
        }
        
//        query = traverseAndTranslate(query, oclass);
    }

//    /** calls handler with given string 
//     * @param sp TODO*/
//    private void notifyHandler(ResultsHandler handler, String uid, SearchPerformer sp) {
//        // TODO this could be more complicated, when other than Name attributes arrive.
//        ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
//                .addAttribute(AttributeBuilder.build(Name.NAME, uid))
//                .addAttribute(new Uid(uid));
//        /*
//         * return RETURNED_BY_DEFAULT attributes + attrsToGet
//         */
//        /** attributes to get */
//        String[] attrsToGet = options.getAttributesToGet();
//        if (attrsToGet == null) {
//            // if no attributes to get, return all RETURNED_BY_DEFAULT attributes
//            attrsToGet = getReturnedByDefaultAttrs(getSchema());
//        }
//        
//        for (String attrName : attrsToGet) {
//            // acquire the attribute's value:
//            final List<String> attrValue = getValueForUid(uid, attrName, sp);
//            
//            // set it in the returned connector object:
//            if (attrValue != null) {
//                builder.addAttribute(AttributeBuilder.build(attrName, attrValue));
//            }
//        }
//        
//        ConnectorObject co = builder.build();
//        
//        handler.handle(co);
//    }

//    /**
//     * Acquire a given attribute for a uid.
//     * 
//     * @param uid
//     *            the uid which is queried for all attributes
//     * @param attrName
//     *            the selected attribute, whose value is returned
//     * @param sp 
//     * @return the attribute's value
//     */
//    private List<String> getValueForUid(String uid, String attrName, SearchPerformer sp) {
//        final SolarisAttribute attrType = SolarisUtil.getAttributeBasedOnName(attrName);
//        if (attrType == null) {
//            return null;
//        }
//        //final List<String> result = sp.performValueSearchForUid(attrType, attrType.getRegExpForUidAndAttribute(), uid);
//        throw new UnsupportedOperationException();
//        
////        return result;
//    }

//    /**
//     * TODO this can be done in two ways: 
//     * either you have a register of returned by default attributes in the connector, 
//     * or you find it in the schema.
//     * 
//     * @param schema
//     * @return set of attribute names that are returned by default
//     */
//    private String[] getReturnedByDefaultAttrs(Schema schema) {
//        // return the cached names
//        if (returnedByDefaultAttributeNames != null)
//            return returnedByDefaultAttributeNames;
//        
//        List<String> result = new ArrayList<String>();
//        
//        ObjectClassInfo ocinfo = schema.findObjectClassInfo(oclass.getObjectClassValue());
//        Set<AttributeInfo> attrInfo = ocinfo.getAttributeInfo();
//        for (AttributeInfo attributeInfo : attrInfo) {
//            if (attributeInfo.isReturnedByDefault()) {
//                result.add(attributeInfo.getName());
//            }
//        }
//        
//        //cache the names
//        returnedByDefaultAttributeNames = result.toArray(new String[0]);
//        
//        return returnedByDefaultAttributeNames;
//    }
}
