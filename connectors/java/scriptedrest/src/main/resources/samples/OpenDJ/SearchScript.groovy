/*
 *
 * Copyright (c) 2013 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 * 
 */
// Parameters:
// The connector sends the following:
// connection: handler to the REST Client 
// (see: http://groovy.codehaus.org/modules/http-builder/apidocs/groovyx/net/http/RESTClient.html)
// configuration : handler to the connector's configuration object
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SEARCH" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
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

log.info("Entering "+action+" Script");

// We can use Groovy template engine to generate our queries
engine = new groovy.text.SimpleTemplateEngine();
searchTemplates = [
    EQUALS:'${not ? "!" : ""}$left+eq+%22$right%22',
    CONTAINS:'${not ? "!" : ""}$left+co+%22$right%22',
    STARTSWITH:'${not ? "!" : ""}$left+sw+%22$right%22',
    LESSTHAN:'${not ? "!" : ""}$left+lt+%22$right%22',
    LESSTHANOREQUAL:'${not ? "!" : ""}$left+le+%22$right%22',
    GREATERTHAN:'${not ? "!" : ""}$left+gt+%22$right%22',
    GREATERTHANOREQUAL:'${not ? "!" : ""}$left+ge+%22$right%22',
    ENDSWITH:'${not ? "!" : ""}$left+ew+%22$right%22',
]

def genQuery (queryObj) {
    if (queryObj.get("left").equalsIgnoreCase("__UID__")) queryObj.put("left","_id");
    if (queryObj.get("left").equalsIgnoreCase("__NAME__")) queryObj.put("left","_id");
    def wt = searchTemplates.get(queryObj.operation);
    def binding = [left:queryObj.left, right:queryObj.right, not:queryObj.not];
    def template = engine.createTemplate(wt).make(binding);
    return template.toString();
}   

def result = []
basePath = "";
switch ( objectClass ) {
case "__ACCOUNT__":
    basePath = "/users/";
    break
case "__GROUP__":
    basePath = "/groups/";
}


if (query != null){
    if (query.operation == "AND" || query.operation == "OR"){
        log.ok("Complex filter is: "+ query.operation)
        search = "("+genQuery(query.left)+"+"+query.operation+"+"+genQuery(query.right)+")"
        log.ok("Search filter is: "+ search)
        connection.get( path : basePath, queryString : "_queryFilter="+search){
            resp, json -> json.result.each(){result.add([__UID__:it._id, __NAME__:it._id])}
        }
        // Exact query on the _id
    } else if (query.get("left").equalsIgnoreCase("__UID__") && query.get("operation").equalsIgnoreCase("EQUALS")){
        log.ok("Exact query on : "+ query.get("right"))
        response = connection.get( path : basePath +query.get("right"));
        json = response.getData();
        log.ok("JSON response:\n"+json)
        switch ( objectClass ) {
        case "__ACCOUNT__":
            result.add([__UID__ : json._id,
                    __NAME__ : json._id, 
                    userName : json.userName,
                    displayName : json.displayName,
                    givenName : (json.name == null) ? null : json.name.givenName,
                    familyName : (json.name == null) ? null : json.name.familyName,
                    emailAddress : (json.contactInformation == null) ? null : json.contactInformation.emailAddress,
                    telephoneNumber : (json.contactInformation == null) ? null : json.contactInformation.telephoneNumber,
                    managerId : (json.manager == null) ? null : json.manager._id,
                    managerDisplayName : (json.manager == null) ? null : json.manager.displayName,
                    created : (json.meta == null) ? null : json.meta.created,
                    lastModified : (json.meta == null) ? null : json.meta.lastModified
                ]);
            break
        case "__GROUP__":
            def members = [];
            if (json.members != null){
                json.members.each(){members.add(it._id)}
            }
            result.add([__UID__ : json._id,
                    __NAME__ : json._id, 
                    displayName : json.displayName,
                    created : (json.meta == null) ? null : json.meta.created,
                    lastModified : (json.meta == null) ? null : json.meta.lastModified,
                    members: members
                ]);
        }
    } else {
        search = genQuery(query)
        log.ok("Search filter is: "+ search)
        connection.get( path : basePath, queryString : "_queryFilter="+search){
            resp, json -> json.result.each(){result.add([__UID__:it._id, __NAME__:it._id])}
        }
    }
}
else{ // null query, return all ids
    log.ok("Search filter is query-all-ids: ")
    connection.get( path : basePath, query : [_queryFilter : '_id pr']){
        resp, json -> json.result.each(){result.add([__UID__:it._id, __NAME__:it._id])}
    }
}
return result;
