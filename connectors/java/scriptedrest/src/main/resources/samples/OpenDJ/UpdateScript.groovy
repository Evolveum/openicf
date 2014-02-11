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
 */

// Parameters:
// The connector sends us the following:
// connection : handler to the REST Client 
// (see: http://groovy.codehaus.org/modules/http-builder/apidocs/groovyx/net/http/RESTClient.html)
// configuration : handler to the connector's configuration object
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.

// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// uid: a String representing the entry uid
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text (only for UPDATE)
// options: a handler to the OperationOptions Map

log.info("Entering "+action+" Script");

// we assume UPDATE action

// We can use Groovy template engine to generate the JSON body
def engine = new groovy.text.SimpleTemplateEngine();

def userTemplate = '''
{
  "_id": "$id",
  "contactInformation": {
    "telephoneNumber": "$telephoneNumber",
    "emailAddress": "$emailAddress"
  },
  "name": {
    "familyName": "$familyName",
    "givenName": "$givenName"
  },
  "displayName": "$displayName"
 }
 '''

def groupTemplate = '''
{
    "_id": "$id",
    "members": [
    <% members.each { %>{ "_id": "$it"},<% } %>
    {"_id": "$last"}
    ]
}
'''

switch ( objectClass ) {
case "__ACCOUNT__":
    def binding = [
        id: uid,
        familyName: (attributes.get("familyName") == null)? "": attributes.get("familyName").get(0),
        givenName: (attributes.get("givenName") == null)? "": attributes.get("givenName").get(0),
        emailAddress: (attributes.get("emailAddress") == null)? "": attributes.get("emailAddress").get(0),
        telephoneNumber: (attributes.get("telephoneNumber") == null)? "": attributes.get("telephoneNumber").get(0),
        displayName: (attributes.get("displayName") == null)? "": attributes.get("displayName").get(0),
    ];
    connection.put( path : '/users/'+uid, body: engine.createTemplate(userTemplate).make(binding).toString());
    break

case "__GROUP__":
    if (attributes.get("members") != null){
        members = []
        members.addAll(attributes.get("members"));
        last = members.pop();
        def binding = [id: uid,members: members,last: last];
        connection.put( path : '/groups/'+uid, body: engine.createTemplate(groupTemplate).make(binding).toString());
    }
}
return uid;
