/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * @author Gael Allioux <gael.allioux@forgerock.com>
 *
 */
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoRecordFieldIterator;
import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoParameterList
import com.sap.conn.jco.JCoParameterFieldIterator
import com.sap.conn.jco.JCoRecordField

// Parameters:
// The connector sends the following:
// destination: handler to the SAP Jco destination
// repository: handler to the SAP functions repository
// action: a string describing the action ("TEST" here)
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// log: a handler to the Log facility
// options: a handler to the OperationOptions object
// infotypeAttrs: handler to the Map containing the attributes to get per INFOTYPE table.
// ex: {"PERSONAL_DATA":["LAST_NAME","FIRST_NAME"], "ORG_ASSIGNMENT":["COMP_CODE","COSTCENTER","ORG_UNIT"]}
// query: a handler to the Query Map
//
// The Query map describes the filter used.
//
// query = [ operation: "CONTAINS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "ENDSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "STARTSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "EQUALS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = null : then we assume we fetch everything
//
// AND and OR filter just embed a left/right couple of queries.
// query = [ operation: "AND", left: query1, right: query2 ]
// query = [ operation: "OR", left: query1, right: query2 ]

// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

log.info("Entering "+action+" Script");

// If needed, check the connection to SAP is alive
// Will throw a JCoException if connection down
//destination.ping();

// Query results that are passed back to the connector
def result = []
def entry = [:]

switch ( objectClass ) {
case "__ACCOUNT__":
    def username = query.get("right")
    // User existence could be checked first with BAPI_USER_EXISTENCE_CHECK
    def function = repository.getFunction("BAPI_USER_GET_DETAIL");
    JCoStructure importStructure = function.getImportParameterList().setValue("USERNAME",username);
    function.execute(destination);
    
    // Check the STATUS
    // MESSAGE: User XXXX does not exist => user absent
    // TYPE: E (error)  I(ok)
    log.info("BAPI RETURN MESSAGE: "+ function.getTableParameterList().getTable("RETURN").getString("MESSAGE"));
    log.info("BAPI RETURN TYPE: "+ function.getTableParameterList().getTable("RETURN").getString("TYPE"));
    if ("E".equalsIgnoreCase(function.getTableParameterList().getTable("RETURN").getString("TYPE"))){
        return result
    }

    entry = ["__UID__":username,"__NAME__":username]
    
    // STRUCTURES
    JCoRecordFieldIterator iter = function.getExportParameterList().getStructure("ADDRESS").getRecordFieldIterator();
    while (iter.hasNextField()){
        JCoRecordField jrf = iter.nextRecordField();
        log.info("ADDRESS field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
        entry.put("ADDRESS::"+jrf.getName(),jrf.getString())
    }
    
    iter = function.getExportParameterList().getStructure("LOGONDATA").getRecordFieldIterator();
    while (iter.hasNextField()){
        JCoRecordField jrf = iter.nextRecordField();
        log.info("LOGON field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
        entry.put("LOGON::"+jrf.getName(),jrf.getString())
    }
    
    iter = function.getExportParameterList().getStructure("DEFAULTS").getRecordFieldIterator();
    while (iter.hasNextField()){
        JCoRecordField jrf = iter.nextRecordField();
        log.info("DEFAULT field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
        entry.put("DEFAULT::"+jrf.getName(),jrf.getString())
    }
    
    iter = function.getExportParameterList().getStructure("COMPANY").getRecordFieldIterator();
    while (iter.hasNextField()){
        JCoRecordField jrf = iter.nextRecordField();
        log.info("COMPANY field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
        entry.put("COMPANY::"+jrf.getName(),jrf.getString())
    }
    
    iter = function.getExportParameterList().getStructure("ISLOCKED").getRecordFieldIterator();
    while (iter.hasNextField()){
        JCoRecordField jrf = iter.nextRecordField();
        log.info("ISLOCKED field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
        entry.put("ISLOCKED::"+jrf.getName(),jrf.getString())
    }
    
    iter = function.getExportParameterList().getStructure("ALIAS").getRecordFieldIterator();
    while (iter.hasNextField()){
        JCoRecordField jrf = iter.nextRecordField();
        log.info("ALIAS field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
        entry.put("ALIAS::"+jrf.getName(),jrf.getString())
    }
    
    iter = function.getExportParameterList().getStructure("LASTMODIFIED").getRecordFieldIterator();
    while (iter.hasNextField()){
        JCoRecordField jrf = iter.nextRecordField();
        log.info("LAST MODIFIED field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
        entry.put("ALIAS::"+jrf.getName(),jrf.getString())
    }
    
    //TABLES
    JCoTable profiles = function.getTableParameterList().getTable("PROFILES");
    def profs = []
    for (int i = 0; i < profiles.getNumRows(); i++)
    {
        profiles.setRow(i);
        iter = profiles.getRecordFieldIterator();
        def prof = "[";
        while (iter.hasNextField()){
            JCoRecordField jrf = iter.nextRecordField();
            log.info("PROFILES field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
            //prof.put(jrf.getName(),jrf.getString());
            prof += jrf.getName()+"::"+jrf.getString();
            if (iter.hasNextField()){
                prof += "||";
            }
        }
        profs.add(prof);
    }
    entry.put("PROFILES",profs)
    
    JCoTable activity = function.getTableParameterList().getTable("ACTIVITYGROUPS");
    def agrps = []
    for (int i = 0; i < activity.getNumRows(); i++)
    {
        activity.setRow(i);
        iter = activity.getRecordFieldIterator();
        def agrp = ""
        while (iter.hasNextField()){
            JCoRecordField jrf = iter.nextRecordField();
            log.info("ACTIVITYGROUPS field name: "+jrf.getName()+",TYPE: "+jrf.getTypeAsString()+", VALUE: "+jrf.getString());
            //agrp.put(jrf.getName(),jrf.getString());
            agrp += jrf.getName()+"::"+jrf.getString();
            if (iter.hasNextField()){
                agrp += "||";
            }
        }
        agrps.add(agrp)
    }
    entry.put("ACTIVITYGROUPS",agrps)
    result.add(entry);
    
    
default: 
    result;
}

log.info("Exiting "+action+" Script");
result;