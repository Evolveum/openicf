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

// Parameters:
// The connector sends the following:
// destination: handler to the SAP Jco destination
// repository: handler to the SAP functions repository
// action: a string describing the action ("SEARCHALL" here)
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// log: a handler to the Log facility
// options: a handler to the OperationOptions object
// infotypeAttrs: handler to the Map containing the attributes to get per INFOTYPE table.
// ex: {"PERSONAL_DATA":["LAST_NAME","FIRST_NAME"], "ORG_ASSIGNMENT":["COMP_CODE","COSTCENTER","ORG_UNIT"]}
// query: the Query - null if action == SEARCHALL

// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

log.info("Entering "+action+" Script");

// If needed, check the connection to SAP is alive
// Will throw a JCoException if connection down
destination.ping();

// Query results that are passed back to the connector
def result = []

// List All objects
switch ( objectClass ) {
case "__ACCOUNT__":
    def function = repository.getFunction("BAPI_USER_GETLIST");
    JCoStructure importStructure = function.getImportParameterList().setValue("WITH_USERNAME",'*');
    function.execute(destination);
    log.info("Number of users: "+function.getExportParameterList().getString("ROWS"));
    
    JCoTable userList = function.getTableParameterList().getTable("USERLIST");
    for (int i = 0; i < userList.getNumRows(); i++)
    {
        userList.setRow(i);
        log.ok("Username: "+userList.getString("USERNAME"));
        result.add([__UID__:userList.getString("USERNAME"),__NAME__:userList.getString("USERNAME")])
    }
default:
    result;    
}
log.info("Exiting "+action+" Script");
result;