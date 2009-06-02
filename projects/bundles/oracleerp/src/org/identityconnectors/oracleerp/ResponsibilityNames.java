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
package org.identityconnectors.oracleerp;

import java.util.EnumSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Main implementation of the OracleErp Connector
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class ResponsibilityNames implements UpdateOp, CreateOp {
    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(ResponsibilityNames.class);

    static final String RESP_NAMES = "responsibilityNames";
    static final String USER_MENU_NAMES = "userMenuNames";
    static final String MENU_IDS = "menuIds";
    static final String USER_FUNCTION_NAMES = "userFunctionNames";
    static final String FUNCTION_IDS = "functionIds";
    static final String FORM_IDS = "formIds";
    static final String FORM_NAMES = "formNames";
    static final String FUNCTION_NAMES = "functionNames";
    static final String USER_FORM_NAMES = "userFormNames";
    static final String READ_ONLY_FORM_IDS = "readOnlyFormIds";
    static final String READ_WRITE_ONLY_FORM_IDS = "readWriteOnlyFormIds";
    static final String READ_ONLY_FORM_NAMES = "readOnlyFormNames";
    static final String READ_ONLY_FUNCTION_NAMES = "readOnlyFunctionNames";
    static final String READ_ONLY_USER_FORM_NAMES = "readOnlyUserFormNames";
    static final String READ_ONLY_FUNCTIONS_IDS = "readOnlyFunctionIds";
    static final String READ_WRITE_ONLY_FORM_NAMES = "readWriteOnlyFormNames";
    static final String READ_WRITE_ONLY_USER_FORM_NAMES = "readWriteOnlyUserFormNames";
    static final String READ_WRITE_ONLY_FUNCTION_NAMES = "readWriteOnlyFunctionNames";
    static final String READ_WRITE_ONLY_FUNCTION_IDS = "readWriteOnlyFunctionIds";

    
    /**
     * The get Instance method
     * @param connector parent
     * @return the account
     */
    public static ResponsibilityNames getInstance(OracleERPConnector connector) {
       return new ResponsibilityNames(connector);
    }
    
    /**
     * The account sigleton 
     * @param connector parent
     */
    private ResponsibilityNames(OracleERPConnector connector) {
        this.parent = connector;
    }
    
    /**
     * The parent connector
     */
    private OracleERPConnector parent;
    
    /**
     * Get the Account Object Class Info
     * 
     * @return ObjectClassInfo value
     */
    public ObjectClassInfo getSchema() {
        ObjectClassInfoBuilder aoc = new ObjectClassInfoBuilder();
        aoc.setType(RESP_NAMES);

        // The Name is supported attribute
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='userMenuNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='menuIds' type='string' audit='false'    
        aoc.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='userFunctionNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='functionIds' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='formIds' type='string' audit='false'    
        aoc.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='formNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='functionNames' type='string' audit='false'    
        aoc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='userFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='readOnlyFormIds' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='readOnlyFormNames' type='string' audit='false'
        aoc
                .addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_NAMES, String.class, EnumSet
                        .of(Flags.REQUIRED)));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTION_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readOnlyUserFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_USER_FORM_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readOnlyFunctionIds' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTIONS_IDS, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FORM_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_USER_FORM_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_IDS, String.class, EnumSet
                .of(Flags.REQUIRED)));
        return aoc.build();
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.UpdateOp#update(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.UpdateOp#update(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        // TODO Auto-generated method stub
        return null;
    }
    


}
