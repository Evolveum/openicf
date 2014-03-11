/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
* " Portions Copyrighted [year] [name of copyright owner]"
*
* @author Gael Allioux <gael.allioux@forgerock.com>
*/
import groovy.sql.Sql;
import groovy.sql.DataSet;

// Parameters:
// The connector sends us the following:
// connection : SQL connection
// configuration : handler to the connector's configuration object
// action: String correponding to the action ("CREATE" here)
// log: a handler to the connector's Logger facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// id: Corresponds to the OpenICF __NAME__ atribute if it is provided as part of the attribute set,
//  otherwise null
// attributes: an Attribute Map, containg the <String> attribute name as a key
//  and the <List> attribute value(s) as value.
// password: password string, clear text
// options: a handler to the OperationOptions Map
// RETURNS: Create must return the immutable identifier (OpenICF __UID__).

log.info("Entering "+action+" Script");

def sql = new Sql(connection);

switch ( objectClass ) {
    case "__ACCOUNT__":
    sql.execute("INSERT INTO Users (uid, firstname,lastname,displayName,email,employeeNumber,employeeType,description,mobilePhone) values (?,?,?,?,?,?,?,?,?)",
        [
            id,
            (attributes.get("firstname") == null) ? "": attributes.get("firstname").get(0),
            (attributes.get("lastname") == null) ? "": attributes.get("lastname").get(0),
            (attributes.get("displayName") == null) ? "": attributes.get("displayName").get(0),
            (attributes.get("email") == null) ? "": attributes.get("email").get(0),
            (attributes.get("employeeNumber") == null) ? "": attributes.get("employeeNumber").get(0),
            (attributes.get("employeeType") == null) ? "": attributes.get("employeeType").get(0),
            (attributes.get("description") == null) ? "": attributes.get("description").get(0),
            (attributes.get("mobilePhone") == null) ? "": attributes.get("mobilePhone").get(0)
        ])
    break

    case "__GROUP__":
    sql.execute("INSERT INTO Groups (gid,name,description) values (?,?,?)",
        [
            (attributes.get("gid") == null) ? "": attributes.get("gid").get(0),
            id,
            (attributes.get("description") == null) ? "": attributes.get("description").get(0)
        ])
    break

    case "organization":
    sql.execute("INSERT INTO Organizations (name,description) values (?,?)",
        [
            id,
            (attributes.get("description") == null) ? "": attributes.get("description").get(0)
        ])
    break

    default:
    id;
}
// We assume the __UID__ value is the same as the id (__NAME__) passed to the script
return id;
