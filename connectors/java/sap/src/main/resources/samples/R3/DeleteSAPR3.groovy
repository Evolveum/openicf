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
import com.sap.conn.jco.JCoRecordFieldIterator
import com.sap.conn.jco.JCoField

// Parameters:
// The connector sends the following:
// destination: handler to the SAP Jco destination
// repository: handler to the SAP functions repository
//
// action: String correponding to the action ("CREATE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// options: a handler to the OperationOptions Map
// uid: String for the unique id that specifies the object to delete

log.info("Entering "+action+" Script");

// If needed, check the connection to SAP is alive
// Will throw a JCoException if connection down
destination.ping();


switch ( objectClass ) {
case "__ACCOUNT__":
    // We're gonna use BAPI_USER_DELETE
    def deleteUser = repository.getFunction("BAPI_USER_DELETE");
    deleteUser.getImportParameterList().setValue("USERNAME",uid);
    deleteUser.execute(destination);
    
    // Check the STATUS
    // MESSAGE: Role assignment to user XXXX deleted => worked
    // MESSAGE: User XXXXX does not exist => failed
    // TYPE: E (error)  S(ok)
    log.info("BAPI RETURN MESSAGE: "+ deleteUser.getTableParameterList().getTable("RETURN").getString("MESSAGE"));
    log.info("BAPI RETURN TYPE: "+ deleteUser.getTableParameterList().getTable("RETURN").getString("TYPE"));
    if ("E".equalsIgnoreCase(deleteUser.getTableParameterList().getTable("RETURN").getString("TYPE"))){
        throw new Exception("Error deleting "+uid)
    }
    break

case "organization":
    break

default:
    break
}
log.info("Exiting "+action+" Script");