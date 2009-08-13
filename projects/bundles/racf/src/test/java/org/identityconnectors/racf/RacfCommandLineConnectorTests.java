/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.racf;

import static org.identityconnectors.racf.RacfConstants.ATTR_CL_ATTRIBUTES;
import static org.identityconnectors.racf.RacfConstants.ATTR_CL_DATA;
import static org.identityconnectors.racf.RacfConstants.ATTR_CL_DFLTGRP;
import static org.identityconnectors.racf.RacfConstants.ATTR_CL_GROUPS;
import static org.identityconnectors.racf.RacfConstants.ATTR_CL_GROUP_CONN_OWNERS;
import static org.identityconnectors.racf.RacfConstants.ATTR_CL_MEMBERS;
import static org.identityconnectors.racf.RacfConstants.ATTR_CL_OWNER;
import static org.identityconnectors.racf.RacfConstants.ATTR_CL_SUPGROUP;
import static org.identityconnectors.racf.RacfConstants.ATTR_CL_TSO_SIZE;

import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.patternparser.MapTransform;
import org.identityconnectors.patternparser.Transform;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RacfCommandLineConnectorTests extends RacfConnectorTestBase {

    public static void main(String[] args) {
        RacfCommandLineConnectorTests tests = new RacfCommandLineConnectorTests();
        try {
            tests.testCreate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void beforeClass() {
        HOST_NAME         = TestHelpers.getProperty("HOST_NAME", null);
        SYSTEM_PASSWORD   = TestHelpers.getProperty("SYSTEM_PASSWORD", null);
        SUFFIX            = TestHelpers.getProperty("SUFFIX", null);
        SYSTEM_USER       = TestHelpers.getProperty("SYSTEM_USER", null);
       
        SYSTEM_USER_LDAP  = "racfid="+SYSTEM_USER+",profileType=user,"+SUFFIX;
        
        Assert.assertNotNull("HOST_NAME must be specified", HOST_NAME);
        Assert.assertNotNull("SYSTEM_PASSWORD must be specified", SYSTEM_PASSWORD);
        Assert.assertNotNull("SYSTEM_USER must be specified", SYSTEM_USER);
        Assert.assertNotNull("SUFFIX must be specified", SUFFIX);
    }

    private String makeLine(String string, int length) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(string);
        while (buffer.length()<length)
            buffer.append(" ");
        return buffer.toString()+"\n";
    }
    
    @Test
    public void testCicsParser() {
        String cicsSegment =
            makeLine(" OPCLASS= 024       023       022       021       020       019       018", 80) +
            makeLine("          017       016       015       014       013       012       011", 80) +
            makeLine("          010       009       008       007       006       005       004", 80) +
            makeLine("          003       002       001", 80) +
            makeLine(" OPIDENT=", 80) +
            makeLine(" OPPRTY= 00255", 80) +
            makeLine(" TIMEOUT= 00:00 (HH:MM)", 80) +
            makeLine(" XRFSOFF= NOFORCE", 80);
        
        try {
            String cicsParser = loadParserFromFile(CICS_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(cicsParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(cicsSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    @Test
    public void testOmvsParser() {
        String omvsSegment =
            makeLine(" UID= NONE", 80) +
            makeLine(" HOME= /u/bmurray", 80) +
            makeLine(" PROGRAM= /bin/sh", 80) +
            makeLine(" CPUTIMEMAX= NONE", 80) +
            makeLine(" ASSIZEMAX= NONE", 80) +
            makeLine(" FILEPROCMAX= NONE", 80) +
            makeLine(" PROCUSERMAX= NONE", 80) +
            makeLine(" THREADSMAX= NONE", 80) +
            makeLine(" MMAPAREAMAX= NONE", 80);
        
        try {
            String omvsParser = loadParserFromFile(OMVS_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(omvsParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(omvsSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    @Test
    public void testTsoParser() {
        String tsoSegment =
            makeLine(" ACCTNUM= ACCT#", 80) +
            makeLine(" HOLDCLASS= X", 80) +
            makeLine(" JOBCLASS= A", 80) +
            makeLine(" MSGCLASS= X", 80) +
            makeLine(" PROC= ISPFPROC", 80) +
            makeLine(" SIZE= 00006133", 80) +
            makeLine(" MAXSIZE= 00000000", 80) +
            makeLine(" SYSOUTCLASS= X", 80) +
            makeLine(" USERDATA= 0000", 80) +
            makeLine(" COMMAND= ISPF PANEL(ISR@390)", 80);
        
        try {
            String tsoParser = loadParserFromFile(TSO_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(tsoParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(tsoSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    @Test
    public void testGroupRacfParser() {
        String racfSegment =
            makeLine(" INFORMATION FOR GROUP DFPADMN", 80) +
            makeLine("     SUPERIOR GROUP=SYSADMN      OWNER=SYSADMN   CREATED=06.123 ", 80) +
            makeLine("     NO INSTALLATION DATA", 80) +
            makeLine("     NO MODEL DATA SET", 80) +
            makeLine("     TERMUACC", 80) +
            makeLine("     SUBGROUP(S)= DFPGRP1, DFPGRP2", 80) +
            makeLine("     USER(S)=      ACCESS=      ACCESS COUNT=     UNIVERSAL ACCESS=", 80) +
            makeLine("       IBMUSER         JOIN          000000              ALTER", 80) +
            makeLine("          CONNECT   ATTRIBUTES=NONE", 80) +
            makeLine("          REVOKE DATE=NONE                 RESUME DATE=NONE", 80) +
            makeLine("       DSMITH          JOIN          000002              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=NONE", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       HOTROD          CONNECT       000004              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=ADSP SPECIAL OPERATIONS", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       ESHAW           USE           000000              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=NONE", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       PROJECTB        USE           000000              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=NONE", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       ADM1            JOIN          000000              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=OPERATIONS", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       AEHALL          USE           000000              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=REVOKED", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("  DFP INFORMATION", 80) +
            makeLine("     MGMTCLAS= DFP2MGMT", 80) +
            makeLine("     STORCLAS= DFP2STOR", 80) +
            makeLine("     DATACLAS= DFP2DATA", 80) +
            makeLine("     DATAAPPL= DFP2APPL", 80);
            
        try {
            String tsoParser = loadParserFromFile(GROUP_RACF_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(tsoParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(racfSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    @Test
    public void testNetviewParser() {
        String netviewSegment =
            makeLine(" IC= START", 80) +
            makeLine(" CONSNAME= DJONES1", 80) +
            makeLine(" CTL= GLOBAL", 80) +
            makeLine(" MSGRECVR= YES", 80) +
            makeLine(" OPCLASS= 1,2", 80) +
            makeLine(" DOMAINS= D1,D2", 80) +
            makeLine(" MAXSIZE= 00000000", 80) +
            makeLine(" NGMFADMN= YES", 80) +
            makeLine(" NGMFVSPN= VNNN", 80);
        
        try {
            String netviewParser = loadParserFromFile(NETVIEW_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(netviewParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(netviewSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test//@Ignore
    public void testDumpSchema() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            Schema schema = connector.schema();
            System.out.print("Schema.oclasses = ");
            char separator = '[';
            for (ObjectClassInfo ocInfo : schema.getObjectClassInfo()) {
                System.out.print(separator+" \""+ocInfo.getType()+"\"");
                separator = ',';
            }
            System.out.println("]");
    
            for (ObjectClassInfo ocInfo : schema.getObjectClassInfo()) {
                System.out.print("Schema.attributes."+ocInfo.getType()+".oclasses = ");
                separator = '[';
                for (AttributeInfo aInfo : ocInfo.getAttributeInfo()) {
                    System.out.print(separator+" \""+aInfo.getName()+"\"");
                    separator = ',';
                }
                System.out.println("]");
            }
    
            for (ObjectClassInfo ocInfo : schema.getObjectClassInfo()) {
                for (AttributeInfo aInfo : ocInfo.getAttributeInfo()) {
                    System.out.println("Schema.\""+aInfo.getName()+"\".attribute."+ocInfo.getType()+".oclasses = [");
                    System.out.println("\ttype              : "+aInfo.getType().getName()+".class,");
                    System.out.println("\treadable          : "+aInfo.isReadable()+",");
                    System.out.println("\tcreateable        : "+aInfo.isCreateable()+",");
                    System.out.println("\tupdateable        : "+aInfo.isUpdateable()+",");
                    System.out.println("\trequired          : "+aInfo.isRequired()+",");
                    System.out.println("\tmultiValue        : "+aInfo.isMultiValued()+",");
                    System.out.println("\treturnedByDefault : "+aInfo.isReturnedByDefault());
                    System.out.println("]\n");
                }
            }
        } finally {
            connector.dispose();
        }
    }

    private String getLoginScript() {
        String script =
            "connection.connect();\n" +
            "connection.waitFor(\"====> \", 20000);\n" +
            "connection.send(\"TSO[enter]\");\n" +
            "connection.waitFor(\"ENTER USERID -\", 20000);\n" +
            "connection.send(USERNAME+\"[enter]\");\n" +
            "connection.waitFor(\"Password  ===>\", 20000);\n" +
            "connection.send(PASSWORD);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\" \\\\*\\\\*\\\\* \", 20000);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\"Option ===>\", 20000);\n" +
            "connection.send(\"[pf3]\");\n" +
            "connection.waitFor(\"READY\\\\s{74}\", 20000);";
        return script;
    }

    private String getLogoffScript() {
        String script = "connection.send(\"LOGOFF[enter]\");\n";
        return script;
    }

    
    // Override these to do Ldap tests
    //
    protected void initializeCommandLineConfiguration(RacfConfiguration config) throws IOException {
        config.setHostNameOrIpAddr(HOST_NAME);
        config.setUseSsl(USE_SSL);
        config.setHostTelnetPortNumber(HOST_TELNET_PORT);
        config.setCommandTimeout(60000);
        config.setConnectScript(getLoginScript());
        config.setDisconnectScript(getLogoffScript());
        config.setUserName(SYSTEM_USER );
        config.setPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        config.setScriptingLanguage("GROOVY");
        config.setSegmentNames(new String[] { 
                "ACCOUNT.RACF",                     "ACCOUNT.TSO",                  "ACCOUNT.NETVIEW",
                "ACCOUNT.CICS",                     "ACCOUNT.OMVS",                 "ACCOUNT.CATALOG", 
                "ACCOUNT.OMVS",                     "GROUP.RACF" });
        config.setSegmentParsers(new String[] { 
                loadParserFromFile(RACF_PARSER),    loadParserFromFile(TSO_PARSER), loadParserFromFile(NETVIEW_PARSER), 
                loadParserFromFile(CICS_PARSER),    loadParserFromFile(OMVS_PARSER), loadParserFromFile(CATALOG_PARSER), 
                loadParserFromFile(OMVS_PARSER),    loadParserFromFile(GROUP_RACF_PARSER) });
        //config.setConnectionClassName("org.identityconnectors.rw3270.wrq.WrqConnection");
        config.setConnectionClassName("org.identityconnectors.rw3270.hod.HodConnection");
        //config.setConnectionClassName("org.identityconnectors.rw3270.freehost3270.FH3270Connection");
    }
    
    protected void initializeLdapConfiguration(RacfConfiguration config) {

    }

    protected String getInstallationDataAttributeName() {
        return ATTR_CL_DATA;
    }
    protected String getDefaultGroupName() {
        return ATTR_CL_DFLTGRP;
    }
    protected String getAttributesAttributeName() {
        return ATTR_CL_ATTRIBUTES;
    }
    protected String getOwnerAttributeName(){
        return ATTR_CL_OWNER;
    }
    protected String getSupgroupAttributeName(){
        return ATTR_CL_SUPGROUP;
    }
    protected String getGroupMembersAttributeName(){
        return ATTR_CL_MEMBERS;
    }
    protected String getGroupsAttributeName(){
        return ATTR_CL_GROUPS;
    }
    protected String getGroupConnOwnersAttributeName(){
        return ATTR_CL_GROUP_CONN_OWNERS;
    }
    protected String getTsoSizeName(){
        return ATTR_CL_TSO_SIZE;
    }
    protected Uid makeUid(String name, ObjectClass objectClass) {
        return new Uid(name);
    }

}
