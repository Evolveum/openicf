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
import org.identityconnectors.framework.common.exceptions.ConnectorException

// Parameters:
// The connector sends the following:
// destination: handler to the SAP Jco destination
// repository: handler to the SAP functions repository
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.

// log: a handler to the Log facility
//
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
//
// uid: a String representing the entry uid (PERNO in HR)
//
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
//
// password: password string, clear text (only for UPDATE)
//
// options: a handler to the OperationOptions Map
//
// Returns: the __UID__ (same as uid parameter)
//
// NOTE: The Update operation is done within a JCoContext begin/end.
// It alllows the execution of stateful function calls with JCo.
// The same connection will be used for all function calls within the script

log.info("Entering {0} script",action);

// If needed, check the connection to SAP is alive
// Will throw a JCoException if connection down
destination.ping();

// Get today's date formated the SAP way: yyyyMMdd
def now = new Date();
def today = now.format('yyyyMMdd');
def endDay = "99991231";


// This example will show how to update/create an EMAIL address on the INFOTYPE 0105 SUBTYPE 0010
// as well as the SYS-NAME on the INFOTYPE 0105 SUBTYPE 0001

if ("UPDATE".equalsIgnoreCase(action)){
    switch ( objectClass ) {
    case "EMPLOYEE":
        def email = attributes.get("COMMUNICATION:EMAIL:ID");
        def account = attributes.get("COMMUNICATION:ACCOUNT:ID");
        
        if (email != null ||account  != null){
            
            def emailError = false;
            def emailErrorMessage = "";
            def accountError = false;
            def accountErrorMessage = "";
            
            // The Sequence is the following:
            // call BAPI_EMPLOYEET_ENQUEUE to lock the employee entry
            def lock = repository.getFunction("BAPI_EMPLOYEET_ENQUEUE");
            lock.getImportParameterList().setValue("VALIDITYBEGIN",today);
            lock.getImportParameterList().setValue("NUMBER",uid);
            lock.execute(destination);
            if (!"".equalsIgnoreCase(lock.getExportParameterList().getStructure("RETURN").getString("TYPE"))){
                log.ok("BAPI_EMPLOYEET_ENQUEUE Status: {0}",lock.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
                throw new ConnectorException(lock.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
            }
            
            // call BAPI_EMPLCOMM_CREATESUCCESSOR to create subsequent communication record
            def updateCom = repository.getFunction("BAPI_EMPLCOMM_CREATESUCCESSOR");
            updateCom.getImportParameterList().setValue("EMPLOYEENUMBER",uid);
            updateCom.getImportParameterList().setValue("VALIDITYBEGIN",today);
            updateCom.getImportParameterList().setValue("VALIDITYEND",endDay);
            
            if (email != null){
                updateCom.getImportParameterList().setValue("SUBTYPE","0010");
                updateCom.getImportParameterList().setValue("COMMUNICATIONID",email.get(0));
                updateCom.execute(destination);
                if (!"".equalsIgnoreCase(updateCom.getExportParameterList().getStructure("RETURN").getString("TYPE"))){
                    log.ok("BAPI_EMPLCOMM_CREATESUCCESSOR Status: {0}",updateCom.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
                    emailError = true;
                    emailErrorMessage = updateCom.getExportParameterList().getStructure("RETURN").getString("MESSAGE");
                    //throw new ConnectorException(updateCom.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
                }
            }
            if (account != null){
                updateCom.getImportParameterList().setValue("SUBTYPE","0001");
                updateCom.getImportParameterList().setValue("COMMUNICATIONID",account.get(0));
                updateCom.execute(destination);
                if (!"".equalsIgnoreCase(updateCom.getExportParameterList().getStructure("RETURN").getString("TYPE"))){
                    log.ok("BAPI_EMPLCOMM_CREATESUCCESSOR Status: {0}",updateCom.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
                    accountError = true;
                    accountErrorMessage = updateCom.getExportParameterList().getStructure("RETURN").getString("MESSAGE");
                    //throw new ConnectorException(updateCom.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
                }
            }
            
            // call BAPI_EMPLOYEET_DEQUEUE to unlock employee
            def unlock = repository.getFunction("BAPI_EMPLOYEET_DEQUEUE");
            unlock.getImportParameterList().setValue("NUMBER",uid);
            unlock.execute(destination);
            if (!"".equalsIgnoreCase(unlock.getExportParameterList().getStructure("RETURN").getString("TYPE"))){
                log.ok("BAPI_EMPLOYEET_DEQUEUE Status: {0}",unlock.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
                throw new ConnectorException(unlock.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
            }
            if (emailError == true){
                throw new ConnectorException(emailErrorMessage);
            }
            if (accountError == true){
                throw new ConnectorException(accountErrorMessage);
            }
        }
    default:
        uid;    
    }
}
log.info("Exiting {0} script",action);
uid;