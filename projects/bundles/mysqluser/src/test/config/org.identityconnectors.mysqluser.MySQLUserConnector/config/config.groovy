// -- START LICENSE
// Copyright 2008 Sun Microsystems, Inc. All rights reserved.
// 
// U.S. Government Rights - Commercial software. Government users 
// are subject to the Sun Microsystems, Inc. standard license agreement
// and applicable provisions of the FAR and its supplements.
// 
// Use is subject to license terms.
// 
// This distribution may include materials developed by third parties.
// Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
// Connectors are trademarks or registered trademarks of Sun 
// Microsystems, Inc. or its subsidiaries in the U.S. and other
// countries.
// 
// UNIX is a registered trademark in the U.S. and other countries,
// exclusively licensed through X/Open Company, Ltd. 
// 
// -----------
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// 
// Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
// 
// The contents of this file are subject to the terms of the Common Development
// and Distribution License(CDDL) (the License).  You may not use this file
// except in  compliance with the License. 
// 
// You can obtain a copy of the License at
// http://identityconnectors.dev.java.net/CDDLv1.0.html
// See the License for the specific language governing permissions and 
// limitations under the License.  
// 
// When distributing the Covered Code, include this CDDL Header Notice in each
// file and include the License file at identityconnectors/legal/license.txt.
// If applicable, add the following below this CDDL Header, with the fields 
// enclosed by brackets [] replaced by your own identifying information: 
// "Portions Copyrighted [year] [name of copyright owner]"
// -----------
// -- END LICENSE
//
// @author Zdenek Louzensky, David Adam


/*  +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+ 
 */
import org.identityconnectors.contract.data.groovy.Lazy;
import org.identityconnectors.common.security.GuardedString

/* Junit Connector configuration */    
configuration{
    host="__configureme__"
    user="__configureme__"
    password=__configureme__
    port="3306"
    driver="com.mysql.jdbc.Driver"
    usermodel="BasicModel"
    testpassword="testpwd"  
  
}


/* Connector configuration */    
connector{
    host="__configureme__"
    user="__configureme__"
    password=new GuardedString("__configureme__".toCharArray())
    port="3306"
    driver="com.mysql.jdbc.Driver"
    usermodel="BasicModel"
    testpassword="testpwd"	
  
}

testsuite {

    /* path to bundle jar - property 'connector-jar' is set by ant */
    bundleJar=System.getProperty("bundleJar")
    bundleName=System.getProperty("bundleName")
    bundleVersion=System.getProperty("bundleVersion")
    connectorName="org.identityconnectors.mysqluser.MySQLUserConnector"
  
    Search.disable.caseinsensitive=true // skip insensitive test
  
    /* ValidateApiOpTests: */
    Validate.iterations="3"
    
    /* AuthenticationApiOpTests: */
    Authentication.__ACCOUNT__.username=Lazy.get("i0.Authentication.__ACCOUNT__.__NAME__")
    Authentication.__ACCOUNT__.wrong.password="bogus"
    
    // Connector WRONG configuration for ValidateApiOpTests    
    Test.invalidConfig = [
        [ password : "NonExistingPassword_foo_bar_boo" ]
    ]

    // Connector WRONG configuration for TestApiOpTests    
    Validate.invalidConfig = [
        [ host : "" ],
        [ user : "" ],
        [ password : null ]
    ]
  
    /* SchemaApiOpTests: */      
    /* declared object classes */
    Schema.oclasses=[ "__ACCOUNT__" ]
    
    /* list of attributes which contains object class "__ACCOUNT__" */
    Schema.attributes.__ACCOUNT__.oclasses=[ "__NAME__", "__PASSWORD__" ]
    
    /* attributes of "__NAME__" */
    Schema.__NAME__.attribute.__ACCOUNT__.oclasses=[type:java.lang.String.class, readable:true, updateable:true, createable:true,   
                                                    required:true, multiValue:false, returnedByDefault:true]
                                                                                                            
    /* attributes of "__PASSWORD__" */                                                        
    Schema.__PASSWORD__.attribute.__ACCOUNT__.oclasses=[type:org.identityconnectors.common.security.GuardedString.class, readable:false,   updateable:true, 
                                                        createable:true, required:false, multiValue:false, returnedByDefault:false]
                                                                                                                    
    /* object classes supported by operation */
    Schema.operations=[
                        GetApiOp:["__ACCOUNT__"], 
                        SchemaApiOp:["__ACCOUNT__"], 
                        ValidateApiOp:["__ACCOUNT__"], 
                        TestApiOp:["__ACCOUNT__"],   
                        CreateApiOp:["__ACCOUNT__"], 
                        SearchApiOp:["__ACCOUNT__"], 
                        DeleteApiOp:["__ACCOUNT__"], 
                        ScriptOnConnectorApiOp:["__ACCOUNT__"], 
                        UpdateApiOp:["__ACCOUNT__"],   
                        AuthenticationApiOp:["__ACCOUNT__"],
                        ResolveUsernameApiOp: ["__ACCOUNT__"]
                        ]
}
 

/* Attribute properties */
// none required right now 
