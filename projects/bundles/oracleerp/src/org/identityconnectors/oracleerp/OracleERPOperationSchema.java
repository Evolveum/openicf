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

import static org.identityconnectors.oracleerp.OracleERPUtil.*;

import java.util.EnumSet;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;


/**
 * The schema implementation of the SPI
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class OracleERPOperationSchema extends Operation implements SchemaOp {

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(OracleERPOperationSchema.class);
    
    //Optional aggregated user attributes
    static final EnumSet<Flags> STD_NRD = EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE,
            Flags.NOT_RETURNED_BY_DEFAULT);
    
    static final EnumSet<Flags> STD_RNA = EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE);
    
	OracleERPOperationSchema(OracleERPConnection conn, OracleERPConfiguration cfg) {
		super(conn, cfg);
	}

	public Schema schema() {

        // Use SchemaBuilder to build the schema.
        SchemaBuilder schemaBld = new SchemaBuilder(OracleERPConnector.class);
        schemaBld.defineObjectClass(getAccountObjectClassInfo());

        // The Responsibilities
        final ObjectClassInfo respNamesOc = getRespNamesObjectClassInfo();
        addSearchableOC(schemaBld, respNamesOc);

        return schemaBld.build();
	}

    /**
     * Add only searchable object class
     * @param schemaBld
     * @param respNamesOc
     */
    private void addSearchableOC(SchemaBuilder schemaBld, final ObjectClassInfo respNamesOc) {
        schemaBld.defineObjectClass(respNamesOc);
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, respNamesOc);
        schemaBld.removeSupportedObjectClass(DeleteOp.class, respNamesOc);
        schemaBld.removeSupportedObjectClass(CreateOp.class, respNamesOc);
        schemaBld.removeSupportedObjectClass(UpdateOp.class, respNamesOc);
        schemaBld.removeSupportedObjectClass(SchemaOp.class, respNamesOc);
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, respNamesOc);
    }
	

    /**
     * Get the Account Object Class Info
     * 
     * @param schemaBld
     * @return The cached object class info
     */
    public ObjectClassInfo getAccountObjectClassInfo() {

        ObjectClassInfoBuilder ocib = new ObjectClassInfoBuilder();
        ocib.setType(ObjectClass.ACCOUNT_NAME);

        // The Name is supported attribute
        ocib.addAttributeInfo(Name.INFO);
        // name='owner' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(OWNER, String.class, EnumSet.of(Flags.NOT_READABLE, 
                Flags.NOT_RETURNED_BY_DEFAULT)));
        // name='session_number' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(SESS_NUM, String.class, EnumSet.of(Flags.NOT_UPDATEABLE,
                Flags.NOT_CREATABLE)));
        // name='start_date' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(START_DATE, String.class ));
        // name='end_date' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(END_DATE, String.class));
        // name='last_logon_date' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(LAST_LOGON_DATE, String.class, EnumSet.of(
                Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE)));
        // name='description' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(DESCR, String.class));
        // <Views><String>Enable</String></Views>
        ocib.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        // name='expirePassword' type='string' required='false' is mapped to PASSWORD_EXPIRED
        ocib.addAttributeInfo(OperationalAttributeInfos.PASSWORD_EXPIRED);
        // reset is implemented as change password
        // name='Password',  Password is mapped to operationalAttribute
        ocib.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
        // name='password_accesses_left' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_DATE, String.class));
        // name='password_accesses_left' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_ACCESSES_LEFT, String.class));
        // name='password_lifespan_accesses' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_LIFE_ACCESSES, String.class));
        // name='password_lifespan_days' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_LIFE_DAYS, String.class));
        // name='employee_id' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(EMP_ID, Integer.class));
        // name='employee_number' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(EMP_NUM, Integer.class));
        // name='person_fullname' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PERSON_FULLNAME, String.class));
        // name='npw_number' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(NPW_NUM, Integer.class));
        // name='email_address' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(EMAIL, String.class));
        // name='fax' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FAX, String.class));
        // name='customer_id' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(CUST_ID, Integer.class));
        // name='supplier_id' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(SUPP_ID, Integer.class));
        // name='person_party_id' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PERSON_PARTY_ID, Integer.class));
        //user_id
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_ID, String.class, EnumSet.of(
                Flags.NOT_RETURNED_BY_DEFAULT, Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE)));

        if (cfg.isNewResponsibilityViews()) {
            // name='DIRECT_RESPS' type='string' required='false'
            ocib
                    .addAttributeInfo(AttributeInfoBuilder.build(DIRECT_RESPS, String.class, EnumSet
                            .of(Flags.MULTIVALUED)));
            // name='INDIRECT_RESPS' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(INDIRECT_RESPS, String.class, EnumSet
                    .of(Flags.MULTIVALUED, Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE)));
        } else {
            // name='RESPS' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(RESPS, String.class, EnumSet.of(Flags.MULTIVALUED)));
        }
        // name='RESPKEYS' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RESPKEYS, String.class, EnumSet.of(Flags.MULTIVALUED, Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE)));
        // name='SEC_ATTRS' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(SEC_ATTRS, String.class, EnumSet.of(Flags.MULTIVALUED)));

        // name='userMenuNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RESP_NAMES, String.class, STD_NRD));
        // name='userMenuNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(AUDITOR_RESPS, String.class, STD_NRD));
        // name='userMenuNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, STD_NRD));
        // name='menuIds' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, STD_NRD));
        // name='userFunctionNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, STD_NRD));
        // name='functionIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, STD_NRD));
        // name='formIds' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, STD_NRD));
        // name='formNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, STD_NRD));
        // name='functionNames' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, STD_NRD));
        // name='userFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, STD_NRD));
        // name='readOnlyFormIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_IDS, String.class, STD_NRD));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_ONLY_FORM_IDS, String.class, STD_NRD));
        // name='readOnlyFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_NAMES, String.class, STD_NRD));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTION_NAMES, String.class, STD_NRD));
        // name='readOnlyUserFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_USER_FORM_NAMES, String.class, STD_NRD));
        // name='readOnlyFunctionIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTIONS_IDS, String.class, STD_NRD));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FORM_NAMES, String.class, STD_NRD));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_USER_FORM_NAMES));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_NAMES, String.class, STD_NRD));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_IDS, String.class, STD_NRD));

        return ocib.build();
    }	

    /**
     * The object class info
     * @return the info class
     */
    public ObjectClassInfo getRespNamesObjectClassInfo() {
        ObjectClassInfoBuilder ocib = new ObjectClassInfoBuilder();

        ocib = new ObjectClassInfoBuilder();
        ocib.setType(RESP_NAMES_OC.getObjectClassValue());

        ocib.addAttributeInfo(Name.INFO);
        // name='userMenuNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, STD_NRD));
        // name='menuIds' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, STD_NRD));
        // name='userFunctionNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, STD_NRD));
        // name='functionIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, STD_NRD));
        // name='formIds' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, STD_NRD));
        // name='formNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, STD_NRD));
        // name='functionNames' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, STD_NRD));
        // name='userFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, STD_NRD));
        // name='readOnlyFormIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_IDS, String.class, STD_NRD));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_ONLY_FORM_IDS, String.class, STD_NRD));
        // name='readOnlyFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_NAMES, String.class, STD_NRD));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTION_NAMES, String.class, STD_NRD));
        // name='readOnlyUserFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_USER_FORM_NAMES, String.class, STD_NRD));
        // name='readOnlyFunctionIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTIONS_IDS, String.class, STD_NRD));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FORM_NAMES, String.class, STD_NRD));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_USER_FORM_NAMES));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_NAMES, String.class, STD_NRD));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_IDS, String.class, STD_NRD));

        return ocib.build();
    }
    /**
     * The object class info
     * @return the info class
     */
    public ObjectClassInfo getAuditorResponsibilitiesObjectClassInfo() {
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();

        //Auditor responsibilities
        oc = new ObjectClassInfoBuilder();
        oc.setType(AUDITOR_RESPS_OC.getObjectClassValue());

        // The Name is supported attribute
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        // name='userMenuNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, STD_RNA));
        // name='menuIds' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, STD_RNA));
        // name='userFunctionNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, STD_RNA));
        // name='functionIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, STD_RNA));
        // name='formIds' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, STD_RNA));
        // name='formNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, STD_RNA));
        // name='functionNames' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, STD_RNA));
        // name='userFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFormIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_IDS, String.class, STD_RNA));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_IDS, String.class, STD_RNA));
        // name='readOnlyFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTION_NAMES, String.class, STD_RNA));
        // name='readOnlyUserFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_USER_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFunctionIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTIONS_IDS, String.class, STD_RNA));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_FORM_NAMES, String.class, STD_RNA));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_USER_FORM_NAMES));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_NAMES, String.class, STD_RNA));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_IDS, String.class, STD_RNA));
        //Define object class
        
        return oc.build();
        /*
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());        
         */
    }
    
    /**
     * The object class info
     * @return the info class
     */
    public ObjectClassInfo getResponsibilitiesObjectClassInfo() {
    ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();    
        //Resp object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(RESP_OC.getObjectClassValue()); 
        oc.setContainer(true);
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        //Define object class
        
        
        /*
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());          
        */

        return oc.build();
    }
    
    /**
     * The object class info
     * 
     * @return the info class
     */
    public ObjectClassInfo getDirectResponsibilitiesObjectClassInfo() {
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();

        //Resp object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(DIRECT_RESP_OC.getObjectClassValue());
        oc.setContainer(true);
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));

        /*
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());          
        */
        return oc.build();
    }

    /**
     * The object class info
     * 
     * @return the info class
     */
    public ObjectClassInfo getIndirectResponsibilitiesObjectClassInfo() {
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();
        //directResponsibilities object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(INDIRECT_RESP_OC.getObjectClassValue());
        oc.setContainer(true);
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));

        /*
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());          
        */
        return oc.build();
    }

    /**
     * The object class info
     * 
     * @return the info class
     */
    public ObjectClassInfo getApplicationsObjectClassInfo() {
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();
        //Applications object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(APPS_OC.getObjectClassValue());
        oc.setContainer(true);
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        /*
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());          
        */
        return oc.build();
    }

    //Seems to be hidden object class, no contract tests 
    /**
     * The object class info
     * 
     * @return the info class
     */
    public ObjectClassInfo getSecurityGroupsObjectClassInfo() {
        //securityGroups object class
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();
        oc.setType(SEC_GROUPS_OC.getObjectClassValue());
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class));
        //Define object class
        /*
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());  
        */
        return oc.build();
    }

    /**
     * The object class info
     * 
     * @return the info class
     */
    public ObjectClassInfo getSecurityAttrsGroupObjectClassInfo() {
        //securingAttrs object class
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();
        oc.setType(SEC_ATTRS_OC.getObjectClassValue());
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class));
        //Define object class
        /*
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());
        */
        return oc.build();
    }
}
