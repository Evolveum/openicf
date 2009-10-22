//  -- START LICENSE
//  ====================
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
//
//  Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
//
//  The contents of this file are subject to the terms of the Common Development
//  and Distribution License("CDDL") (the "License").  You may not use this file
//  except in compliance with the License.
//
//  You can obtain a copy of the License at
//  http://IdentityConnectors.dev.java.net/legal/license.txt
//  See the License for the specific language governing permissions and limitations
//  under the License.
//
//  When distributing the Covered Code, include this CDDL Header Notice in each file
//  and include the License file at identityconnectors/legal/license.txt.
//  If applicable, add the following below this CDDL Header, with the fields
//  enclosed by brackets [] replaced by your own identifying information:
//  "Portions Copyrighted [year] [name of copyright owner]"
//  ====================
//  -- END LICENSE
//

/*  +---------------------------------------------------+
 *  ----------- Tests configuration ------------
 *  +---------------------------------------------------+ 
 */

import java.math.BigInteger
import org.identityconnectors.contract.data.groovy.Lazy;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.contract.exceptions.ObjectNotFoundException;
import org.identityconnectors.common.script.Script

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
    tst.userActionScript=""
    
    sysadm.driver="oracle.jdbc.driver.OracleDriver"
    sysadm.url="__configureme__"
    sysadm.user="__configureme__"
    sysadm.password=new GuardedString("__configureme__".toCharArray());
    sysadm.accountsIncluded="where USER_NAME like 'JTU%'"
    sysadm.activeAccountsOnly=false
    sysadm.auditResponsibility="System Administrator"
    sysadm.manageSecuringAttrs=true
    sysadm.noSchemaId=false
    sysadm.returnSobOrgAttrs=true
    sysadm.clientEncryptionType="RC4_40"
    sysadm.clientEncryptionLevel="REJECTED" 
    
    user.driver="oracle.jdbc.driver.OracleDriver"
    user.user="__configureme__"
    user.password=new GuardedString("__configureme__".toCharArray());
    user.host="__configureme__"
    user.database="PROD"
    user.port="1521"

}

account{
    required.__NAME__="JTU-"
    required.__PASSWORD__= new GuardedString("password".toCharArray())
   // required.owner="CUST"
    required.start_date=stringDate(-10)
    
    all.__NAME__="JTU-"
    all.owner="CUST"
    all.session_number=0
    
    all.start_date=stringDate(-10)
    all.end_date=stringDate(+10)
    all.last_logon_date=stringDate(0)
    all.description="Connector test user"
    
    all.__PASSWORD__= new GuardedString("password".toCharArray())
    all.__PASSWORD_EXPIRED__=false
    all.password_date=stringDate(0)
    
    all.password_accesses_left="56"
    all.password_lifespan_accesses="5"
    all.password_lifespan_days="30"
    
    all.employee_number="5"
    all.person_fullname="Monster, Cookie"
    all.email_address="person@somewhere.com"
    all.fax="555-555-5555"
    //all.employee_id=5
    //all.npw_number=4
    //all.customer_id=11223344
    //all.supplier_id=102
    //all.person_party_id="3044"
    
    all.directResponsibilities="Cash Forecasting||Cash Management||Standard||2004-04-12||null"
    all.responsibilityKeys="Cash Forecasting||Cash Management||Standard"
    all.securingAttrs="TO_PERSON_ID||Self-Service Web Applications||114"
    
    modify.__NAME__="JTUM-"
    modify.__PASSWORD__= new GuardedString("modpasswd".toCharArray())
    modify.email_address="person1@somewhere.com"
    modify.fax="666-666-6666"
    modify.directResponsibilities=["Cash Forecasting||Cash Management||Standard||2004-04-12||null","Purchasing Receiver||Purchasing||Standard||2004-04-12||null"]
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
    
    enabled.__ENABLE__=true
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
    accountsIncluded="where USER_NAME like 'CTU-%' AND (START_DATE - SYSDATE <= 0) AND ((END_DATE IS NULL) OR (END_DATE - SYSDATE > 0))"
    activeAccountsOnly=false  // accountsIncluded clause invalidate activeAccountsOnly
    auditResponsibility="System Administrator"
    manageSecuringAttrs=true
    noSchemaId=false
    returnSobOrgAttrs=false
    userActionScript=""
    /* WRONG configuration for ValidateApiOpTests */
    i1.wrong.host=""
    i2.wrong.user=""
    i3.wrong.password=new GuardedString("".toCharArray())
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
    Authentication.__ACCOUNT__.wrong.password=new GuardedString("WRONG".toCharArray())
    
    
    /* SchemaApiOpTests: */
    Schema.strictCheck=false
    Schema.oclasses=[ "__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ]
    Schema.attributes.__ACCOUNT__.oclasses=['owner','start_date','end_date','description','password_date','password_accesses_left','password_lifespan_accesses','password_lifespan_days','employee_id','employee_number','person_fullname','npw_number','email_address','fax','customer_id','supplier_id','person_party_id','directResponsibilities','indirectResponsibilities','responsibilityKeys','securingAttrs','expirePassword','last_logon_date','session_number','userMenuNames','menuIds','userFunctionNames','functionIds','formIds','formNames','functionNames','userFormNames','readOnlyFormIds','readWriteOnlyFormIds','readOnlyFormNames','readOnlyFunctionNames','readOnlyUserFormNames','readOnlyFunctionIds','readWriteOnlyFormNames','readWriteOnlyUserFormNames','readWriteOnlyFunctionNames','readWriteOnlyFunctionIds']
    Schema.attributes.auditorResps.oclasses=['name','userMenuNames','menuIds','userFunctionNames','functionIds','formIds','formNames','functionNames','userFormNames','readOnlyFormIds','readWriteOnlyFormIds','readOnlyFormNames','readOnlyFunctionNames','readOnlyUserFormNames','readOnlyFunctionIds','readWriteOnlyFormNames','readWriteOnlyUserFormNames','readWriteOnlyFunctionNames','readWriteOnlyFunctionIds']
    Schema.attributes.responsibilityNames.oclasses=['name','userMenuNames','menuIds','userFunctionNames','functionIds','formIds','formNames','functionNames','userFormNames','readOnlyFormIds','readWriteOnlyFormIds','readOnlyFormNames','readOnlyFunctionNames','readOnlyUserFormNames','readOnlyFunctionIds','readWriteOnlyFormNames','readWriteOnlyUserFormNames','readWriteOnlyFunctionNames','readWriteOnlyFunctionIds']
    Schema.attributes.directResponsibilities.oclasses=['name']
    Schema.attributes.indirectResponsibilities.oclasses=['name']
    Schema.attributes.responsibilities.oclasses=['name']
    Schema.attributes.applications.oclasses=['name']
    Schema.attributes.securingAttrs.oclasses=['name']
    Schema.attributes.securityGroups.oclasses=['name']

    /* object classes supported by operation */
    Schema.operations=[
        GetApiOp:["__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ],
        SearchApiOp:["__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ],
        SchemaApiOp:["__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ],
        ValidateApiOp:["__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ],
        TestApiOp:["__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ],
        ScriptOnConnectorApiOp: ["__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ],
        ResolveUsernameApiOp: ["__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ],
        AuthenticationApiOp: ["__ACCOUNT__", "applications", "auditorResps", "directResponsibilities", "indirectResponsibilities", "responsibilityNames", "responsibilities", "securingAttrs", "securityGroups" ],
        CreateApiOp:["__ACCOUNT__"],
        DeleteApiOp:["__ACCOUNT__"],
        UpdateApiOp:["__ACCOUNT__"],
     ]
     
     
//  Connector WRONG configuration for ValidateApiOpTests
  Validate.invalidConfig = [
     [ user : "" ],
     [ driver : "" ]
  ]
  
//  Connector WRONG configuration for TestApiOpTests
  Test.invalidConfig = [
     [ password : "NonExistingPassword_foo_bar_boo" ]
  ]
}

__ACCOUNT__ {
    __NAME__="CTU-" + Lazy.random("AAAAAA######")
    __PASSWORD__= new GuardedString("password".toCharArray())
    modified.__PASSWORD__= new GuardedString("modpasswd".toCharArray())
    __ENABLE__= true
    owner="CUST"
    session_number=0
    start_date=stringDate(-10)
    modified.start_date=stringDate(-99)
    end_date=stringDate(+10)
    modified.end_date=stringDate(+99)
    last_logon_date=stringDate(0)
    description="Connector test user"
    modified.description="Connector test user mod"
    password_date=stringDate(0)
    password_accesses_left="56"
    modified.password_accesses_left="30"
    password_lifespan_accesses="5"
    modified.password_lifespan_accesses="10"
    password_lifespan_days="30"
    modified.password_lifespan_days="20"
    employee_number="5"
    person_fullname="Monster, Cookie"
    person_party_id="3044"
    modified.person_party_id="3044"
    email_address="person@somewhere.com"
    modified.email_address="person1@somewhere.com"
    fax="555-555-5555"
    modified.fax="666-666-6666"
    employee_id=empty()
    modified.employee_id=empty()
    customer_id=empty()
    modified.customer_id=empty()
    supplier_id=empty()
    modified.supplier_id=empty()
    npw_number=empty()
    modified.npw_number=empty()
    directResponsibilities=["Cash Forecasting||Cash Management||Standard||2004-04-12||null"]
    modified.directResponsibilities=["Cash Forecasting||Cash Management||Standard||2004-04-12||2010-01-01","Purchasing Receiver||Purchasing||Standard||2004-04-12||null"]             
    responsibilityKeys=["Cash Forecasting||Cash Management"]
    modified.responsibilityKeys=["Cash Forecasting||Cash Management","Purchasing Receiver||Purchasing"]
    securingAttrs=["TO_PERSON_ID||Self-Service Web Applications||114"]
    modified.securingAttrs=["ICX_HR_PERSON_ID||Self-Service Web Applications||114", "TO_PERSON_ID||Self-Service Web Applications||112"]
}


/* Define tests functions */
def currentTimeMillis(){
    return System.currentTimeMillis()
}

def stringDate( dife ){
    return new java.sql.Timestamp(System.currentTimeMillis() + dife*24*3600000 ).toString().substring(0, 10)+" 00:00:00.0"
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

