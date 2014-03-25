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
// destination: handler to the SAP destination
// repository: handler to the SAP functions repository
// action: a string describing the action ("TEST" here)
// log: a handler to the Log facility

log.info("Entering {0} script",action);
destination.ping();

def function = repository.getFunction("BAPI_EMPLOYEE_GETDATA");
function.getImportParameterList().setValue("LASTNAME_M",'*');
function.execute(destination);

JCoTable persData = function.getTableParameterList().getTable("PERSONAL_DATA");
for (int i = 0; i < persData.getNumRows(); i++)
{
    persData.setRow(i);
    System.out.println("Emp number: "+persData.getString("PERNO"));
    System.out.println("Last Name: "+persData.getString("LAST_NAME"));
}

log.info("Test script done");
