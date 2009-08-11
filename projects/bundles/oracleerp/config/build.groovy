import java.math.BigInteger
/*  +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+ 
 */
import org.identityconnectors.contract.data.groovy.Lazy;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.contract.exceptions.ObjectNotFoundException;

/* JUNIT tests configurations */  
configuration{
    tst.driver="oracle.jdbc.driver.OracleDriver"    
    tst.url="__configureme__"
    tst.user="__configureme__"
    tst.password=new GuardedString("__configureme__".toCharArray());
    tst.accountsIncluded=""
    tst.activeAccountsOnly=true
    tst.auditResponsibility=""
    tst.manageSecuringAttrs=false
    tst.noSchemaId=false
    tst.returnSobOrgAttrs=false
    tst.userActions=""
    
    sysadm.driver="oracle.jdbc.driver.OracleDriver"    
    sysadm.url="__configureme__"
    sysadm.user="__configureme__"
    sysadm.password=new GuardedString("__configureme__".toCharArray());
    sysadm.accountsIncluded=""
    sysadm.activeAccountsOnly=false
    sysadm.auditResponsibility="System Administrator"
    sysadm.manageSecuringAttrs=true
    sysadm.noSchemaId=false
    sysadm.returnSobOrgAttrs=true
    sysadm.userActions=""
    
    user.driver="oracle.jdbc.driver.OracleDriver"    
    user.user="__configureme__"
    user.password=new GuardedString("__configureme__".toCharArray());
    user.host="__configureme__"
    user.database="PROD"
    user.port="1521"    
}

account{
    required.__NAME__="JTU-"
    required.__PASSWORD__="password"
   // required.owner="CUST"
    required.start_date=stringDate(-10)
    
    all.__NAME__="JTU-"
    all.owner="CUST"
    all.session_number=0

    all.start_date=stringDate(-10)
    all.end_date=stringDate(+10)
    all.last_logon_date=stringDate(0)
    all.description="Connector test user"

    all.__PASSWORD__="password"
    all.__PASSWORD_EXPIRED__=false
    all.password_date=stringDate(0)
    
    all.password_accesses_left=56
    all.password_lifespan_accesses=5
    all.password_lifespan_days=30
    
    //all.employee_id=5 
    all.employee_number=5
    all.person_fullname="Monster, Cookie"
    //all.npw_number=4
    all.email_address="person@somewhere.com"
    all.fax="555-555-5555"
    //all.customer_id=11223344
    //all.supplier_id=102
    
    all.directResponsibilities="Cash Forecasting||Cash Management||Standard||2004-04-12||null"
    all.responsibilityKeys="Cash Forecasting||Cash Management||Standard"
    all.securingAttrs="TO_PERSON_ID||Self-Service Web Applications||114"
        

    modify.__NAME__="JTUM-"
    modify.__PASSWORD__="modpasswd"
    modify.email_address="person1@somewhere.com"
    modify.fax="666-666-6666"
    modify.directResponsibilities=["Cash Forecasting||Cash Management||Standard||2004-04-12||2010-01-01","Purchasing Receiver||Purchasing||Standard||2004-04-12||null"]
    modify.password_accesses_left=58
    modify.password_lifespan_accesses=6
    modify.password_lifespan_days=31    
    modify.description="New Test Description"
    modify.owner="CUST"
    modify.securingAttrs=["ICX_HR_PERSON_ID||Self-Service Web Applications||114", "TO_PERSON_ID||Self-Service Web Applications||112"]
 
    options.responsibility="Cash Forecasting"
    options.application="Cash Management"
    options.searchPattern="%_PERSON_ID"
    options.activeRespsOnly=true;
    
    auditor.auditorResps="Cash Forecasting||Cash Management"
    auditor.userMenuNames=["CE_OTHER", "Requests Menu - Other Responsibilities", "CE_FORECAST", "CE_FORECAST_SETUP", "CE_DESCRIPTIVE_FLEXFIELDS"]
    auditor.menuIds=["67617", "67850", "68361", "68364", "68781"]
    auditor.userFunctionNames=["Concurrent Requests: View", "Define External Forecast Sources", "Define Templates", "Descriptive Flexfield Segments", "Descriptive Flexfield Values", "Maintain Forecasts", "Profile User Values", "Request Sets (User Mode)", "Requests: Submit", "View All Concurrent Requests", "View Cash Forecasts"]
    auditor.formIds=["54952", "54572", "20423", "10397", "54570", "20648", "51615", "51614", "51614", "54571"]
    auditor.formNames=["CEFFCOPI", "CEFFCDFN", "FNDFFMDC", "FNDFFMSV", "CEFFCAST", "FNDPOMSV", "FNDRSSET", "FNDRSRUN", "FNDRSRUN", "CEFQFCST"]
    auditor.userFormNames=["Define External Forecast Sources", "Define Templates", "Define Descriptive Flexfield Segments", "Define Segment Values", "Maintain Forecasts", "Update Personal Profile Values", "Administer Report Sets", "Run Reports", "Run Reports", "Inquire Forecasts"]
    
    enabled.__PASSWORD_EXPIRED__=false
//   enabled.__ENABLE_DATE__=getDt(-10)
//   enabled.__LAST_LOGIN_DATE__=getDt(0)
//   enabled.__DISSABLE_DATE__=getDt(+10)
    enabled.__ENABLE__=true
    dissabled.__PASSWORD_EXPIRED__=false
    dissabled.__ENABLE__=false
}





/* Connector contract tests configuration */    
connector{
    driver="oracle.jdbc.driver.OracleDriver"    
    host="__configureme__"
    port="1521"
    database="PROD"
    user="__configureme__"
    password=new GuardedString("__configureme__".toCharArray());
    accountsIncluded=""
    activeAccountsOnly=true
    auditResponsibility="System Administrator"
    manageSecuringAttrs=false
    noSchemaId=false
    returnSobOrgAttrs=false
    userActions=""  
  
    /* WRONG configuration for ValidateApiOpTests */  
    i1.wrong.host=""
    i2.wrong.user=""
    i3.wrong.password=""
    i4.wrong.database=""
    i53.wrong.driver=""
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
    Authentication.__ACCOUNT__.wrong.password="__configureme__"
  
    /* SchemaApiOpTests: */      
    /* declared object classes */
    Schema.oclasses=[ "__ACCOUNT__", "responsibilityNames" ]
    
    /* list of attributes which contains object class "__ACCOUNT__" */
    Schema.attributes.__ACCOUNT__.oclasses=[ "__NAME__", "__PASSWORD__" ]
    
    // many attributes have similar values                                                
    Schema.common.attribute=[
        type: java.lang.String.class,
        readable: true,
        createable: true,
        updateable: true,
        required: false,
        multiValue: false,
        returnedByDefault: true
    ]                                        

    Schema.nrbd.attribute=[
                             type: java.lang.String.class,
                             readable: true,
                             createable: true,
                             updateable: true,
                             required: false,
                             multiValue: false,
                             returnedByDefault: false
                         ]                                        
    
    /* attributes of "__NAME__" */
    Schema.__NAME__.attribute.__ACCOUNT__.oclasses=[type:"java.lang.String", readable:"true", updateable:"true", createable:"true",   
                                                    required:"true", multiValue:"false", returnedByDefault:"true"]
                                                                                                            
    /* attributes of "__PASSWORD__" */                                                        
    Schema.__PASSWORD__.attribute.__ACCOUNT__.oclasses=[type:"org.identityconnectors.common.security.GuardedString", readable:"false",   updateable:"true", 
                                                        createable:"true", required:"true", multiValue:"false", returnedByDefault:"true"]

    Schema.MIDDLENAME.attribute.__ACCOUNT__.oclasses=testsuite.Schema.common.attribute         

    
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

__NAME__="CTU-" + Lazy.random("AAAAAA######")
__PASSWORD__="password"
owner="CUST"
session_number=0

start_date=stringDate(-10)
end_date=stringDate(+10)
last_logon_date=stringDate(0)
description="Connector test user"

password_date=stringDate(0)

password_accesses_left=56
password_lifespan_accesses=5
password_lifespan_days=30

employee_id=empty()
employee_number=5
person_fullname="Monster, Cookie"
person_party_id=3044
npw_number=empty()
email_address="person@somewhere.com"
fax="555-555-5555"
customer_id=empty()
supplier_id=empty()

directResponsibilities="Cash Forecasting||Cash Management||Standard||2004-04-12||null"
responsibilityKeys="Cash Forecasting||Cash Management||Standard"
securingAttrs="TO_PERSON_ID||Self-Service Web Applications||114"

def currentTimeMillis(){
    return System.currentTimeMillis()
}

def stringDate( dife ){
    return new java.sql.Timestamp(System.currentTimeMillis() + dife*24*3600000 ).toString()
}    

def getDt( dife ){
    Calendar cal = Calendar.getInstance()
    cal.setTimeInMillis(System.currentTimeMillis())
    cal.add(Calendar.DAY_OF_MONTH, dife)   
    return cal.getTimeInMillis()
}  

def empty() {
    return new ObjectNotFoundException()
}
