/*  +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+ 
 */
import org.identityconnectors.contract.data.groovy.Lazy;
import org.identityconnectors.common.security.GuardedString;




/* Connector configuration */    
connector{
    driver="oracle.jdbc.driver.OracleDriver"    
    hostName="idm149.central.sun.com"
    port="1521"
    databaseName="PROD"
    user="APPS"
    password=new GuardedString("APPS".toCharArray());
    accountsIncluded=false
    activeAccountsOnly=false
    auditResponsibility="test"
    manageSecuringAttrs=true
    noSchemaId=true
    returnSobOrgAttrs=false
    userActions=""
    
  
    /* WRONG configuration for ValidateApiOpTests */  
    i1.wrong.host=""
    i2.wrong.login=""
    i3.wrong.password=""
    i4.wrong.databaseName=""
    i53.wrong.driver=""
}


/* account configurations */  
configuration{
    init.driver="oracle.jdbc.driver.OracleDriver"    
    init.url="java:oracle:thin:@idm149.central.sun.com:1521:PROD"
    init.user="APPS"
    init.password=new GuardedString("APPS".toCharArray());
    init.accountsIncluded=""
    init.activeAccountsOnly=false
    init.auditResponsibility=""
    init.manageSecuringAttrs=true
    init.noSchemaId=false
    init.returnSobOrgAttrs=false
    init.userActions=""
}

account{
    required.__NAME__="TST-REG-USER-" + currentTimeMillis()
    required.__PASSWORD__=new GuardedString("passwd".toCharArray());
    required.owner="CUST"
    
    all.__NAME__="TST-USER-" + currentTimeMillis()
    all.__PASSWORD__=new GuardedString("passwd".toCharArray());
    all.__PASSWORD_EXPIRED__=false
    all.start_date=stringDate(-10*24*3600000)
    all.end_date=stringDate(+10*24*3600000)
    all.responsibilities="Cash Forecasting||Cash Management||Standard||2004-04-12 00:00:00.0||null"
    all.password_accesses_left=56
    all.password_lifespan_accesses=5
    all.password_lifespan_days=30
    // all.employee_id=5
    all.description="Test Description"
    all.owner="CUST"
    all.email_address="person@somewhere.com"
    all.fax="555-555-5555"
    //all.customer_id=11223344
    //all.supplier_id=102
    all.securingAttrs="TO_PERSON_ID||Self-Service Web Applications||110"
        

    modify.__NAME__="TST-MOD-USER-" + currentTimeMillis()
    modify.__PASSWORD__=new GuardedString("modpasswd".toCharArray());
    modify.email_address="person1@somewhere.com"
    modify.fax="666-666-6666"
    modify.responsibilities="Cash Forecasting||Oracle Cash Management||Standard||Test Description||2004-04-12 00:00:00.0||null"
    modify.responsibilities1="Purchasing Receiver||Oracle Purchasing||Standard||Test Description1||2004-04-12 00:00:00.0||null"
    modify.password_lifespan_days=31
    modify.description="New Test Description"
    modify.owner="CUST"
    modify.securingAttrs="TO_PERSON_ID||Oracle Self-Service Web Applications||112"    
 /*   modify.securingAttrs1="ICX_HR_PERSON_ID||Oracle Self-Service Web Applications||114"    */
}


testsuite {

    /* path to bundle jar - property 'connector-jar' is set by ant */
    bundleJar=System.getProperty("bundleJar")
    bundleName=System.getProperty("bundleName")
    bundleVersion=System.getProperty("bundleVersion")
    connectorName="org.identityconnectors.oracleerp.OracleERPConnector"
  
    Search.disable.caseinsensitive=true // skip insensitive test
  
    /* AuthenticationApiOpTests: */
    Authentication.__ACCOUNT__.username=Lazy.get("i0.Authentication.__ACCOUNT__.__NAME__")
    Authentication.__ACCOUNT__.wrong.password="bogus"
  
    /* SchemaApiOpTests: */      
    /* declared object classes */
    Schema.oclasses=[ "__ACCOUNT__" ]
    
    /* list of attributes which contains object class "__ACCOUNT__" */
    Schema.attributes.__ACCOUNT__.oclasses=[ "__NAME__", "__PASSWORD__" ]
    
    /* attributes of "__NAME__" */
    Schema.__NAME__.attribute.__ACCOUNT__.oclasses=[type:"java.lang.String", readable:"true", updateable:"true", createable:"true",   
                                                    required:"true", multiValue:"false", returnedByDefault:"true"]
                                                                                                            
    /* attributes of "__PASSWORD__" */                                                        
    Schema.__PASSWORD__.attribute.__ACCOUNT__.oclasses=[type:"org.identityconnectors.common.security.GuardedString", readable:"false",   updateable:"true", 
                                                        createable:"true", required:"true", multiValue:"false", returnedByDefault:"true"]
                                                                                                                    
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
    

//  Connector WRONG configuration for ValidateApiOpTests
  Validate.invalidConfig = [
     [ host : "" ], 
     [ login : "" ],
     [ password : "" ],
     [ databaseName : "" ],
     [ driver : "" ]
  ]

//  Connector WRONG configuration for TestApiOpTests
  Test.invalidConfig = [
     [ password : "NonExistingPassword_foo_bar_boo" ]
  ]
}

def currentTimeMillis(){
    return System.currentTimeMillis()
}

def stringDate( dife ){
    return new java.sql.Timestamp(System.currentTimeMillis() - dife ).toString()
}    

