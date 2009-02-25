/*  +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+ 
 */
import org.identityconnectors.contract.data.groovy.Lazy;

/* Connector configuration */    
connector{
    host="__configureme__"
    user="__configureme__"
    password="__configureme__"
    port="3306"
    driver="com.mysql.jdbc.Driver"
    usermodel="BasicModel"
    testpassword="testpwd"	
  
    /* WRONG configuration for ValidateApiOpTests */  
    i1.wrong.host=""
    i2.wrong.user=""
    i3.wrong.password=""
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
                        AuthenticationApiOp:["__ACCOUNT__"]
                        ]
}
 

/* Attribute properties */
// none required right now 
