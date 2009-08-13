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

import static org.identityconnectors.framework.common.objects.OperationalAttributeInfos.*;
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
    static final EnumSet<Flags> NRD = EnumSet.of(Flags.NOT_READABLE, Flags.NOT_RETURNED_BY_DEFAULT);

    static final EnumSet<Flags> NCU = EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE);

    
    static final EnumSet<Flags> MNCUD = EnumSet.of(Flags.MULTIVALUED, Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE,
            Flags.NOT_RETURNED_BY_DEFAULT);
    
    static final EnumSet<Flags> NCUD = EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE,
            Flags.NOT_RETURNED_BY_DEFAULT);
    
    static final EnumSet<Flags> MNCU = EnumSet.of(Flags.MULTIVALUED, Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE);
    
    static final EnumSet<Flags> M = EnumSet.of(Flags.MULTIVALUED);
    
	OracleERPOperationSchema(OracleERPConnection conn, OracleERPConfiguration cfg) {
		super(conn, cfg);
	}

	public Schema schema() {
        log.info("schema");

        // Use SchemaBuilder to build the schema.
        SchemaBuilder schemaBld = new SchemaBuilder(OracleERPConnector.class);
        
        final ObjectClassInfo accountOci = getAccountObjectClassInfo();
        schemaBld.defineObjectClass(accountOci);

        // The ResponsibilityNames
        final ObjectClassInfo respNamesOci = getRespNamesObjectClassInfo();
        addSearchableOnlyOC(schemaBld, respNamesOci);

        // The Responsibilities for listing 
        // TODO we can join ResponsibilityNames and Responsibilities
        final ObjectClassInfo respOci = getResponsibilitiesObjectClassInfo();
        addSearchableOnlyOC(schemaBld, respOci);

        // The Applications for listing 
        final ObjectClassInfo appOci = getApplicationsObjectClassInfo();
        addSearchableOnlyOC(schemaBld, appOci);

        // The Auditor for listing 
        final ObjectClassInfo auditOci = getAuditorResponsibilitiesObjectClassInfo();
        addSearchableOnlyOC(schemaBld, auditOci);
        
        // The DirectResponsibilities for listing 
        final ObjectClassInfo directOci = getDirectResponsibilitiesObjectClassInfo();
        addSearchableOnlyOC(schemaBld, directOci);
                
        // The IndirectResponsibilities for listing 
        final ObjectClassInfo indirectOci = getIndirectResponsibilitiesObjectClassInfo();
        addSearchableOnlyOC(schemaBld, indirectOci);
        
        // The IndirectResponsibilities for listing 
        final ObjectClassInfo secAttrOci = getSecuringAttrsGroupObjectClassInfo();
        addSearchableOnlyOC(schemaBld, secAttrOci);    
        
        // The IndirectResponsibilities for listing 
        final ObjectClassInfo secGrpsOci = getSecurngGroupsObjectClassInfo();
        addSearchableOnlyOC(schemaBld, secGrpsOci);  

        final Schema schema = schemaBld.build();
        log.info("schema done");
        return schema;
	}

    /**
     * Add only searchable object class
     * @param schemaBld
     * @param oci
     */
    private void addSearchableOnlyOC(SchemaBuilder schemaBld, final ObjectClassInfo oci) {
        schemaBld.defineObjectClass(oci);
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oci);
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oci);
        schemaBld.removeSupportedObjectClass(CreateOp.class, oci);
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oci);
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oci);
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oci);
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
        ocib.addAttributeInfo(AttributeInfoBuilder.build(OWNER, String.class, OracleERPOperationSchema.NRD));
        // name='session_number' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(SESS_NUM, String.class, OracleERPOperationSchema.NCU));
        // name='start_date' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(START_DATE, String.class ));
        // name='end_date' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(END_DATE, String.class));
        // name='last_logon_date' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(LAST_LOGON_DATE, String.class, OracleERPOperationSchema.NCU));
        // name='description' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(DESCR, String.class));
        // name='expirePassword' type='string' required='false' is mapped to PASSWORD_EXPIRED
        ocib.addAttributeInfo(AttributeInfoBuilder.build(EXP_PWD, Boolean.class, NCUD));            
        // name='password_accesses_left' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_DATE, String.class));
        // name='password_accesses_left' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_ACCESSES_LEFT, String.class));
        // name='password_lifespan_accesses' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_LIFESPAN_ACCESSES, String.class));
        // name='password_lifespan_days' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_LIFESPAN_DAYS, String.class));
        // name='employee_id' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(EMP_ID, String.class));
        // name='employee_number' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(EMP_NUM, String.class));
        // name='person_fullname' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PERSON_FULLNAME, String.class));
        // name='npw_number' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(NPW_NUM, String.class));
        // name='email_address' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(EMAIL, String.class));
        // name='fax' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FAX, String.class));
        // name='customer_id' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(CUST_ID, String.class));
        // name='supplier_id' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(SUPP_ID, String.class));
        // name='person_party_id' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(PERSON_PARTY_ID, String.class));
        //user_id
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_ID, String.class, NCUD));

        if (cfg.isNewResponsibilityViews()) {
            // name='DIRECT_RESPS' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(DIRECT_RESPS, String.class, M));
            // name='INDIRECT_RESPS' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(INDIRECT_RESPS, String.class, MNCU));
        } else {
            // name='RESPS' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(RESPS, String.class, M));
        }
        // name='RESPKEYS' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RESPKEYS, String.class, MNCU));
        // name='SEC_ATTRS' type='string' required='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(SEC_ATTRS, String.class, M));

        // name='userMenuNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RESP_NAMES, String.class, MNCUD));
        // name='userMenuNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(AUDITOR_RESPS, String.class, MNCUD));
        // name='userMenuNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, MNCUD));
        // name='menuIds' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, MNCUD));
        // name='userFunctionNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, MNCUD));
        // name='functionIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, MNCUD));
        // name='formIds' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, MNCUD));
        // name='formNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, MNCUD));
        // name='functionNames' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, MNCUD));
        // name='userFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, MNCUD));
        // name='readOnlyFormIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_IDS, String.class, MNCUD));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_ONLY_FORM_IDS, String.class, MNCUD));
        // name='readOnlyFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_NAMES, String.class, MNCUD));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTION_NAMES, String.class, MNCUD));
        // name='readOnlyUserFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_USER_FORM_NAMES, String.class, MNCUD));
        // name='readOnlyFunctionIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTIONS_IDS, String.class, MNCUD));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FORM_NAMES, String.class, MNCUD));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_USER_FORM_NAMES, String.class, MNCUD));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_NAMES, String.class, MNCUD));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_IDS, String.class, MNCUD));

        
        // ocib.addAttributeInfo(OperationalAttributeInfos.ENABLE_DATE);        
        // ocib.addAttributeInfo(OperationalAttributeInfos.DISABLE_DATE);        
        // ocib.addAttributeInfo(PredefinedAttributeInfos.LAST_LOGIN_DATE);        
        // ocib.addAttributeInfo(PredefinedAttributeInfos.LAST_PASSWORD_CHANGE_DATE);                
        // <Views><String>Enable</String></Views>
        ocib.addAttributeInfo(ENABLE);
        ocib.addAttributeInfo(OperationalAttributeInfos.PASSWORD_EXPIRED);
        // reset is implemented as change password
        // name='Password',  Password is mapped to operationalAttribute
        ocib.addAttributeInfo(OperationalAttributeInfos.PASSWORD);

        
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
        // The Name is supported attribute
        ocib.addAttributeInfo(AttributeInfoBuilder.build(NAME, String.class, NCUD));
        // name='userMenuNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, MNCU));
        // name='menuIds' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, MNCU));
        // name='userFunctionNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, MNCU));
        // name='functionIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, MNCU));
        // name='formIds' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, MNCU));
        // name='formNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, MNCU));
        // name='functionNames' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, MNCU));
        // name='userFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, MNCU));
        // name='readOnlyFormIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_IDS, String.class, MNCU));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_ONLY_FORM_IDS, String.class, MNCU));
        // name='readOnlyFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_NAMES, String.class, MNCU));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTION_NAMES, String.class, MNCU));
        // name='readOnlyUserFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_USER_FORM_NAMES, String.class, MNCU));
        // name='readOnlyFunctionIds' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTIONS_IDS, String.class, MNCU));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FORM_NAMES, String.class, MNCU));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_USER_FORM_NAMES, String.class, MNCU));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_NAMES, String.class, MNCU));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        ocib.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_IDS, String.class, MNCU));

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

        oc.addAttributeInfo(Name.INFO);
        // The Name is supported attribute
        oc.addAttributeInfo(AttributeInfoBuilder.build(NAME, String.class, NCUD));
        // name='userMenuNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, MNCU));
        // name='menuIds' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, MNCU));
        // name='userFunctionNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, MNCU));
        // name='functionIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, MNCU));
        // name='formIds' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, MNCU));
        // name='formNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, MNCU));
        // name='functionNames' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, MNCU));
        // name='userFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, MNCU));
        // name='readOnlyFormIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_IDS, String.class, MNCU));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_ONLY_FORM_IDS, String.class, MNCU));
        // name='readOnlyFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FORM_NAMES, String.class, MNCU));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTION_NAMES, String.class, MNCU));
        // name='readOnlyUserFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_USER_FORM_NAMES, String.class, MNCU));
        // name='readOnlyFunctionIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RO_FUNCTIONS_IDS, String.class, MNCU));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_FORM_NAMES, String.class, MNCU));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_USER_FORM_NAMES, String.class, MNCU));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_NAMES, String.class, MNCU));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        oc.addAttributeInfo(AttributeInfoBuilder.build(RW_FUNCTION_IDS, String.class, MNCU));
        return oc.build();
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
        // The Name is supported attribute
        oc.addAttributeInfo(Name.INFO);
        oc.addAttributeInfo(AttributeInfoBuilder.build(NAME, String.class, NCUD));
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
        // The Name is supported attribute
        oc.addAttributeInfo(Name.INFO);
        oc.addAttributeInfo(AttributeInfoBuilder.build(NAME, String.class, NCUD));
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
        // The Name is supported attribute
        oc.addAttributeInfo(Name.INFO);
        oc.addAttributeInfo(AttributeInfoBuilder.build(NAME, String.class, NCUD));
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
        // The Name is supported attribute
        oc.addAttributeInfo(Name.INFO);
        oc.addAttributeInfo(AttributeInfoBuilder.build(NAME, String.class, NCUD));
        return oc.build();
    }

    //Seems to be hidden object class, no contract tests 
    /**
     * The object class info
     * 
     * @return the info class
     */
    public ObjectClassInfo getSecurngGroupsObjectClassInfo() {
        //securityGroups object class
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();
        oc.setType(SEC_GROUPS_OC.getObjectClassValue());
        // The Name is supported attribute
        oc.addAttributeInfo(Name.INFO);
        oc.addAttributeInfo(AttributeInfoBuilder.build(NAME, String.class, NCUD));
        return oc.build();
    }

    /**
     * The object class info
     * 
     * @return the info class
     */
    public ObjectClassInfo getSecuringAttrsGroupObjectClassInfo() {
        //securingAttrs object class
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();
        oc.setType(SEC_ATTRS_OC.getObjectClassValue());
        // The Name is supported attribute
        oc.addAttributeInfo(Name.INFO);
        oc.addAttributeInfo(AttributeInfoBuilder.build(NAME, String.class, NCUD));
        return oc.build();
    }
}
