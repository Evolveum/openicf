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
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.JCoRecordFieldIterator
import com.sap.conn.jco.JCoField

// Parameters:
// The connector sends the following:
// destination: handler to the SAP Jco destination
// repository: handler to the SAP functions repository
// action: a string describing the action ("SEARCH" here)
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// log: a handler to the Log facility
// options: a handler to the OperationOptions object
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
// 

// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

log.info("Entering {0} script",action);

// If needed, check the connection to SAP is alive
// Will throw a JCoException if connection down
//destination.ping();

// Get today's date formated the SAP way: yyyyMMdd
def now = new Date();
def today = now.format('yyyyMMdd');

// Query results that are passed back to the connector
def result = []

switch ( objectClass ) {
case "EMPLOYEE":
    def entry = [:]
    // Let's start with BAPI_EMPLOYEE_GETDATA
    // to get INFOTYPE 0001 (ORG ASSIGNEMENT) and 0002 (PERSONAL DATA)
    def empGetData = repository.getFunction("BAPI_EMPLOYEE_GETDATA");
    empGetData.getImportParameterList().setValue("EMPLOYEE_ID",query.get("right"));
    empGetData.execute(destination);
    // Check status
    if (!"".equalsIgnoreCase(empGetData.getExportParameterList().getStructure("RETURN").getString("TYPE"))){
        log.info("BAPI_EMPLOYEE_GETDATA - Returned message: {0}",empGetData.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
        log.info("BAPI_EMPLOYEE_GETDATA - Returned type: {0}",empGetData.getExportParameterList().getStructure("RETURN").getString("TYPE"));
        // Not found... return here
        return result;
    }
        
    // PERSONAL_DATA table (INFOTYPE: 0002)
    log.info("Processing PERSONAL DATA:");
    JCoTable persData = empGetData.getTableParameterList().getTable("PERSONAL_DATA");
    if (!persData.isEmpty())
    {
        entry.put("__UID__",persData.getString("PERNO"));
        entry.put("__NAME__",persData.getString("PERNO"));
        JCoRecordFieldIterator fit = persData.getRecordFieldIterator();
        while (fit.hasNextField()) {
            JCoField  field = fit.nextField();
            if (field.getValue() != null){
                log.info("field name: {0}, value: {1}",field.getName(),field.getValue().toString());
                entry.put("PERSONAL_DATA:"+field.getName(),field.getValue().toString())
            }
        }
    }
    
    // ORG_ASSIGNMENT table (INFOTYPE: 0001)
    log.info("Processing ORG ASSIGNEMENT:");
    JCoTable orgData = empGetData.getTableParameterList().getTable("ORG_ASSIGNMENT");
    if (!orgData.isEmpty())
    {
        JCoRecordFieldIterator fit = orgData.getRecordFieldIterator();
        while (fit.hasNextField()) {
            JCoField  field = fit.nextField();
            if (field.getValue() != null){
                log.info("field name: {0}, value: {1}",field.getName(),field.getValue().toString());
                entry.put("ORG_ASSIGNMENT:"+field.getName(),field.getValue().toString())
            }
        }
    }
    // COMMUNICATION table (INFOTYPE: 0105) BAPI_EMPLCOMM_GETDETAILEDLIST
    // If we need to retrieve EMAIL, let's call another BAPI to be able to specifiy SUBTYPE
    // EMAIL: 0010
    // ACCOUNTID (System-User): 0001
    def empComData = repository.getFunction("BAPI_EMPLCOMM_GETDETAILEDLIST");
    empComData.getImportParameterList().setValue("EMPLOYEENUMBER",query.get("right"));
    empComData.getImportParameterList().setValue("SUBTYPE","0010");
    empComData.getImportParameterList().setValue("TIMEINTERVALLOW",today);
    //empComData.getImportParameterList().setValue("TIMEINTERVALHIGH","99991231");
    empComData.execute(destination);
        
    // Check status
    if ("".equalsIgnoreCase(empComData.getExportParameterList().getStructure("RETURN").getString("TYPE"))){
        log.info("COMM DATA:");
        JCoTable comData = empComData.getTableParameterList().getTable("COMMUNICATION");
        if (!comData.isEmpty())
        {
            JCoRecordFieldIterator fit = comData.getRecordFieldIterator();
            while (fit.hasNextField()) {
                JCoField  field = fit.nextField();
                if (field.getValue() != null){
                    log.info("field name: {0}, value: {1}",field.getName(),field.getValue().toString());
                    entry.put("COMMUNICATION:EMAIL:"+field.getName(),field.getValue().toString());
                }
            }
                
        }
    }
    else{
        log.info("BAPI_EMPLCOMM_GETDETAILEDLIST (email) - Returned message: {0}",empComData.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
        log.info("BAPI_EMPLCOMM_GETDETAILEDLIST (email) - Returned type: {0}",empComData.getExportParameterList().getStructure("RETURN").getString("TYPE"));
    }
    // Account
    empComData.getImportParameterList().setValue("SUBTYPE","0001");
    empComData.execute(destination);
        
    // Check status
    if ("".equalsIgnoreCase(empComData.getExportParameterList().getStructure("RETURN").getString("TYPE"))){
        comData = empComData.getTableParameterList().getTable("COMMUNICATION");
        if (!comData.isEmpty())
        {
            JCoRecordFieldIterator fit = comData.getRecordFieldIterator();
            while (fit.hasNextField()) {
                JCoField  field = fit.nextField();
                if (field.getValue() != null){
                    log.info("field name: {0}, value: {1}",field.getName(),field.getValue().toString());
                    entry.put("COMMUNICATION:ACCOUNT:"+field.getName(),field.getValue().toString());
                }
            }
        }
    }
    else{
        log.info("BAPI_EMPLCOMM_GETDETAILEDLIST (account) - Returned message: {0}",empComData.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
        log.info("BAPI_EMPLCOMM_GETDETAILEDLIST (account) - Returned type: {0}",empComData.getExportParameterList().getStructure("RETURN").getString("TYPE"));
    }
    result.add(entry);
        
default:
    result;
}
log.info("Exiting {0} script",action);

result;