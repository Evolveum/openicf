/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
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
 * $Id$
 */
import groovyx.net.http.RESTClient;
// Parameters:
// The connector sends the following:
// client: handler to the REST Client
// action: a string describing the action ("TEST" here)
// log: a handler to the Log facility

def response = client.get( path : '/openidm/managed/user', 
    query : [_queryId : 'query-all-ids'], 
    headers : ['X-OpenIDM-Username' : 'openidm-admin', 'X-OpenIDM-Password' : 'openidm-admin']
);

println response.isSuccess();