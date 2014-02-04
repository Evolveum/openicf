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

// Parameters:
// The connector sends the following:
// destination: handler to the SAP Jco destination
// repository: handler to the SAP functions repository
// action: a string describing the action ("SEARCHALL" here)
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// log: a handler to the Log facility
// options: a handler to the OperationOptions object
// query: the Query - null if action == SEARCHALL

// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

log.info("Entering {0} script",action);


// If needed, check the connection to SAP is alive
// Will throw a JCoException if connection down
//destination.ping();

// Query results that are passed back to the connector
def result = []

// List All objects
switch ( objectClass ) {
case "EMPLOYEE":
    def function = repository.getFunction("BAPI_EMPLOYEE_GETDATA");
    function.getImportParameterList().setValue("LASTNAME_M",'*');
    function.execute(destination);
    
    // Check status
    if (!"".equalsIgnoreCase(function.getExportParameterList().getStructure("RETURN").getString("TYPE"))){
        log.info("BAPI_EMPLOYEE_GETDATA - Returned message: {0}",empGetData.getExportParameterList().getStructure("RETURN").getString("MESSAGE"));
        log.info("BAPI_EMPLOYEE_GETDATA - Returned type: {0}",empGetData.getExportParameterList().getStructure("RETURN").getString("TYPE"));
        // Nothing found or problem... return here
        return result;
    }
    JCoTable persData = function.getTableParameterList().getTable("PERSONAL_DATA");
    for (int i = 0; i < persData.getNumRows(); i++)
    {
        persData.setRow(i);
        log.ok("Employee Number: {0}",persData.getString("PERNO"));
        result.add([__UID__:persData.getString("PERNO"),__NAME__:persData.getString("PERNO")])
    }
default:
    result;    
}
log.info("Exiting {0} script",action);

result;