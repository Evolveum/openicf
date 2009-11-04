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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.ConnectorAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.AbstractOp;
import org.identityconnectors.solaris.operation.search.nodes.AcceptAllNode;
import org.identityconnectors.solaris.operation.search.nodes.EqualsNode;
import org.identityconnectors.solaris.operation.search.nodes.Node;


public class OpSearchImpl extends AbstractOp {
    
    private static final Log _log = Log.getLog(OpSearchImpl.class);
    
    private final ObjectClass oclass;
    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT, ObjectClass.GROUP};
    private final Node filter;
    private final ResultsHandler handler;
    
    /** original set of attributes to get, contains {@see ConnectorAttribute}-s. */
    private final String[] attrsToGet;
    /** names of attributes to get translated to {@see NativeAttribute}-s. */
    private final Set<NativeAttribute> attrsToGetNative;
    
    /** names of returned by default attributes (given by schema, it is static during lifetime of the connector */
    private static String[] returnedByDefaultAttributeNames; // todo possibly this could be acquired right from connector attribute structures.
    
    public OpSearchImpl(SolarisConnector conn, ObjectClass oclass, Node filter,
            ResultsHandler handler, OperationOptions options) {
        super(conn);
        this.oclass = oclass;
        
        if (filter == null) {
            // NULL indicates that we should return all results.
            this.filter = new AcceptAllNode();
        } else {
            this.filter = filter;
        }
        
        this.handler = handler;
        
        
        
        /** attributes to get init */
        String[] attrsToGet = options.getAttributesToGet();
        if (attrsToGet == null) {
            // if no attributes to get, return all RETURNED_BY_DEFAULT attributes
            attrsToGet = getReturnedByDefaultAttrs(getSchema());
        }
        this.attrsToGet = attrsToGet;
        
        
        // translate attrsToGet from Connector to Native attributes:
        Set<NativeAttribute> translatedAttrs = new HashSet<NativeAttribute>(attrsToGet.length);
        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            for (String accountAttrName : attrsToGet) {
                translatedAttrs.add(AccountAttribute.forAttributeName(accountAttrName).getNative());
            }
        } else if (oclass.is(ObjectClass.GROUP_NAME)) {
            for (String groupAttrName : attrsToGet) {
                translatedAttrs.add(GroupAttribute.forAttributeName(groupAttrName).getNative());
            }
        }
        attrsToGetNative = translatedAttrs;
    }
    
    /**
     * Search operation
     * 
     * @param filter contains the filters. Is created by {@link SolarisFilterTranslator}
     */
    public void executeQuery() {
        _log.info("search ({0})", filter.toString());
        SolarisUtil.controlObjectClassValidity(oclass, acceptOC, getClass());
        
        if (oclass.is(ObjectClass.GROUP_NAME)) {
            // TODO
            throw new UnsupportedOperationException();
        }
        
        /*
         * 
         *  ACCOUNT search
         *   
         */
        
        /* required attributes inside the search (!= attrsToGet) */
        Set<NativeAttribute> requiredAttrs = new HashSet<NativeAttribute>(NativeAttribute.values().length);
        // 1) retrieve native attributes from the Node
        filter.collectAttributeNames(requiredAttrs);
        // 2) make union of attributes inside filter AND attributesToGet given from client.
        requiredAttrs.addAll(attrsToGetNative);
        
        
        if (filter instanceof EqualsNode && ((EqualsNode) filter).getAttributeName().equals(NativeAttribute.NAME)) {
            simpleFilter(requiredAttrs);
        } else {
            complexFind(requiredAttrs);
        }
        _log.info("search successfully finished.");
    }

    /**
     * COMPLEX filtering, requires evaluation of the filter tree ({@see Node}).
     */
    private void complexFind(Set<NativeAttribute> requiredAttrs) {
        Iterator<SolarisEntry> entryIt = SolarisEntries.getAllAccounts(requiredAttrs, getConnection());
        while (entryIt.hasNext()) {
            final SolarisEntry entry = entryIt.next();
            if (filter.evaluate(entry)) {
                ConnectorObject connObj = convertToConnectorObject(entry, oclass);
                handler.handle(connObj);
            }
        }
    }

    /**
     * SIMPLE filtering defined as an {@see EqualsNode} with a single {@see Name} attribute.
     * For instance: userName = 'johnSmith'.
     */
    private void simpleFilter(Set<NativeAttribute> requiredAttrs) {
        SolarisEntry singleAccount = SolarisEntries.getAccount(((EqualsNode) filter).getValue(), requiredAttrs, getConnection());
        if (singleAccount != null) {
            ConnectorObject connObj = convertToConnectorObject(singleAccount, oclass);
            handler.handle(connObj);
        }
    }

    /**
     * @param account
     * @return A connector object based on attributes of given 'account', that contains the ATTRS_TO_GET.
     */
    private ConnectorObject convertToConnectorObject(SolarisEntry account, ObjectClass oclass) {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        Map<String, Attribute> indexedEntry = AttributeUtil.toMap(account.getAttributeSet());
        
        // add UID
        builder.addAttribute(new Uid(account.getName()));
        
        // and rest of the attributes
        for (String attribute : attrsToGet) {
            ConnectorAttribute connAttr = (oclass.is(ObjectClass.ACCOUNT_NAME)) ? AccountAttribute.forAttributeName(attribute) : GroupAttribute.forAttributeName(attribute);
            
            final Attribute attrToConvert = indexedEntry.get(connAttr.getNative().getName());
            List<Object> value = (attrToConvert != null) ? attrToConvert.getValue() : null;
            if (value == null) 
                value = Collections.emptyList();
            builder.addAttribute(connAttr.getName(), value);
        }
        
        return builder.build();
    }

    /**
     * TODO enhancement::: this can be done in two ways: 
     * either you have a register of returned by default attributes in the connector, 
     * or you find it in the schema.
     * 
     * @param schema
     * @return set of attribute names that are returned by default
     */
    private String[] getReturnedByDefaultAttrs(Schema schema) {
        // return the cached names
        if (returnedByDefaultAttributeNames != null)
            return returnedByDefaultAttributeNames;
        
        List<String> result = new ArrayList<String>();
        
        ObjectClassInfo ocinfo = schema.findObjectClassInfo(oclass.getObjectClassValue());
        Set<AttributeInfo> attrInfo = ocinfo.getAttributeInfo();
        for (AttributeInfo attributeInfo : attrInfo) {
            if (attributeInfo.isReturnedByDefault()) {
                result.add(attributeInfo.getName());
            }
        }
        
        //cache the names
        returnedByDefaultAttributeNames = result.toArray(new String[0]);
        
        return returnedByDefaultAttributeNames;
    }
}
