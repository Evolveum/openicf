/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.patternparser;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Assert;

import org.identityconnectors.patternparser.MapTransform.PatternNode;
import org.identityconnectors.patternparser.test.SubstringTransform;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;



public class PatternParserTest {
	static final String pattern =
	    "<MapTransform>\n" +
	    "  <PatternNode key='username' pattern='Username:\\s+(.*?)\\s*(?=Owner)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='owner' pattern='Owner:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='account' pattern='Account:\\s+(.*?)\\s*(?=UIC)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='uic' pattern='UIC:\\s+(\\[[^]]+\\]).*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='cli' pattern='CLI:\\s+(.*?)\\s*(?=Tables)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='clitables' pattern='Tables:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='default' pattern='Default:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='lgicmd' pattern='LGICMD:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='Flags' pattern='Flags:\\s+(.*?)\\s*(?=Primary days)' optional='false' reset='false'>\n" +
	    "    <SplitTransform splitPattern='\\s+'/>\n" +
	    "  </PatternNode>\n" +
	    "  <PatternNode key='Primary days' pattern='Primary days:\\s+(.*?)\\s*\\n' optional='false' reset='false'>\n" +
	    "    <SplitTransform splitPattern='\\s+'/>\n" +
	    "  </PatternNode>\n" +
	    "  <PatternNode key='Secondary days' pattern='Secondary days:\\s+(.*?)\\s*\\n' optional='false' reset='false'>\n" +
	    "    <SplitTransform splitPattern='\\s+'/>\n" +
	    "  </PatternNode>\n" +
	    "  <PatternNode key='expiration' pattern='Expiration:\\s+(.*?)\\s*(?=Pwdminimum)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='pwdminimum' pattern='Pwdminimum:\\s+(.*?)\\s*(?=Login Fails)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='loginfails' pattern='Login Fails:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='pwdlifetime' pattern='Pwdlifetime:\\s+(.*?)\\s*(?=Pwdchange)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='pwdchange' pattern='Pwdchange:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='lastlogin' pattern='Last Login:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='maxjobs' pattern='Maxjobs:\\s+(.*?)\\s*(?=Fillm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='fillm' pattern='Fillm:\\s+(.*?)\\s*(?=Bytlm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='bytlm' pattern='Bytlm:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='maxacctjobs' pattern='Maxacctjobs:\\s+(.*?)\\s*(?=Shrfillm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='shrfillm' pattern='Shrfillm:\\s+(.*?)\\s*(?=Pbytlm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='pbytlm' pattern='Pbytlm:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='maxdetach' pattern='Maxdetach:\\s+(.*?)\\s*(?=BIOlm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='biolm' pattern='BIOlm:\\s+(.*?)\\s*(?=JTquota)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='jtquota' pattern='JTquota:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='prclm' pattern='Prclm:\\s+(.*?)\\s*(?=DIOlm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='diolm' pattern='DIOlm:\\s+(.*?)\\s*(?=WSdef)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='wsdef' pattern='WSdef:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='prio' pattern='Prio:\\s+(.*?)\\s*(?=ASTlm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='astlm' pattern='ASTlm:\\s+(.*?)\\s*(?=WSquo)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='wsquo' pattern='WSquo:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='queprio' pattern='Queprio:\\s+(.*?)\\s*(?=TQElm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='tqelm' pattern='TQElm:\\s+(.*?)\\s*(?=WSextent)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='wsextent' pattern='WSextent:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='cpu' pattern='CPU:\\s+(.*?)\\s*(?=Enqlm)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='enqlm' pattern='Enqlm:\\s+(.*?)\\s*(?=Pgflquo)' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='pgflquo' pattern='Pgflquo:\\s+(.*?)\\s*\\n' optional='false' reset='false'/>\n" +
	    "  <PatternNode key='Authorized Privileges' pattern='Authorized Privileges:\\s+\\n\\s{2}?((?:.+\\n)(?:\\s{2}.+\\n)*)' optional='false' reset='false'>\n" +
	    "    <SubstituteTransform pattern='IMPERSONAT(\\w\\w+)' substitute='IMPERSONATE $1'/>\n" +
	    "    <SplitTransform splitPattern='\\s+'/>\n" +
	    "  </PatternNode>\n" +
	    "  <PatternNode key='Default Privileges' pattern='Default Privileges:\\s+\\n\\s{2}?((?:.+\\n)(?:\\s{2}.+\\n)*)' optional='false' reset='false'>\n" +
	    "    <SubstituteTransform pattern='IMPERSONAT(\\w\\w+)' substitute='IMPERSONATE $1'/>\n" +
	    "    <SplitTransform splitPattern='\\s+'/>\n" +
	    "  </PatternNode>\n" +
	    "  <PatternNode key='Identifier' pattern='Identifier.*?\\n\\s{2}?((?:.+\\n)(?:\\s{2}.+\\n)*)' optional='false' reset='false'>\n" +
	    "    <SplitTransform splitPattern='\\n'/>\n" +
	    "    <ListTransform>\n" +
	    "      <GroupsTransform mapPattern='\\s*(\\S+)\\s+(\\S+)\\s*(\\S+)?$'/>\n" +
	    "    </ListTransform>\n" +
	    "  </PatternNode>\n" +
	    "</MapTransform>\n";

    static final String sampleUser = 
        "Username: SYSTEM                           Owner:  SYSTEM MANAGER\n" +
        "Account:  SYSTEM                           UIC:    [1,4] ([SYSTEM])\n" +
        "CLI:      DCL                              Tables: DCLTABLES\n" +
        "Default:  SYS$SYSROOT:[SYSMGR]\n" +
        "LGICMD:   LOGIN\n" +
        "Flags: \n" +
        "Primary days:   Mon Tue Wed Thu Fri        \n" +
        "Secondary days:                     Sat Sun\n" +
        "No access restrictions\n" +
        "Expiration:            (none)    Pwdminimum:  8   Login Fails:     0\n" +
        "Pwdlifetime:           (none)    Pwdchange:  21-JUL-2005 16:28 \n" +
        "Last Login: 29-OCT-2007 00:19 (interactive), 28-JUN-2007 15:23 (non-interactive)\n" +
        "Maxjobs:         0  Fillm:       300  Bytlm:        32768\n" +
        "Maxacctjobs:     0  Shrfillm:      0  Pbytlm:           0\n" +
        "Maxdetach:       0  BIOlm:        40  JTquota:       4096\n" +
        "Prclm:          10  DIOlm:        40  WSdef:          256\n" +
        "Prio:            4  ASTlm:        50  WSquo:          512\n" +
        "Queprio:         0  TQElm:        30  WSextent:      2048\n" +
        "CPU:        (none)  Enqlm:       200  Pgflquo:      40960\n" +
        "Authorized Privileges: \n" +
        "  ACNT      ALLSPOOL  ALTPRI    AUDIT     BUGCHK    BYPASS    CMEXEC    CMKRNL\n" +
        "  IMPERSONATDIAGNOSE  DOWNGRADE EXQUOTA   GROUP     GRPNAM    GRPPRV    IMPORT\n" +
        "  LOG_IO    MOUNT     NETMBX    OPER      PFNMAP    PHY_IO    PRMCEB    PRMGBL\n" +
        "  PRMMBX    PSWAPM    READALL   SECURITY  SETPRV    SHARE     SHMEM     SYSGBL\n" +
        "  SYSLCK    SYSNAM    SYSPRV    TMPMBX    UPGRADE   VOLPRO    WORLD\n" +
        "Default Privileges: \n" +
        "  ACNT      ALLSPOOL  ALTPRI    AUDIT     BUGCHK    BYPASS    CMEXEC    CMKRNL\n" +
        "  IMPERSONATDIAGNOSE  DOWNGRADE EXQUOTA   GROUP     GRPNAM    GRPPRV    IMPORT\n" +
        "  LOG_IO    MOUNT     NETMBX    OPER      PFNMAP    PHY_IO    PRMCEB    PRMGBL\n" +
        "  PRMMBX    PSWAPM    READALL   SECURITY  SETPRV    SHARE     SHMEM     SYSGBL\n" +
        "  SYSLCK    SYSNAM    SYSPRV    TMPMBX    UPGRADE   VOLPRO    WORLD\n" +
        "Identifier                         Value           Attributes\n" +
        "  GOOMBAH                          %X91F50002      Zoom\n" +
        "  NET$MANAGE                       %X91F5AAAA      \n"
        ;

    public PatternParserTest() {
    }
    
    /**
     * Parse a string from AUTHORIZE and return a Map containing the
     * user data
     * 
     * @param user -- string output from AUTHORIZE describing a user
     * @return Map&lt;String, Object&gt; -- map describing user attributes
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseAuthorizeOutput(String user) throws Exception {
        Map<String, Object> userMap = (Map<String, Object>)_parser.transform(user);
        return userMap;
    }
    
    public static MapTransform _parser;
    
    @Test
    public void testWithPatterns() throws Exception {
        _parser = (MapTransform)getTransform(pattern);
        Map<String, Object> userMap = parseAuthorizeOutput(sampleUser);
        System.out.println(userMap);
    }

    @Test
    public void testBooleanTransform() throws Exception {
    	Transform transform = new BooleanTransform();
    	Assert.assertEquals(Boolean.TRUE, transform.transform("true"));
    	Assert.assertEquals(Boolean.TRUE, transform.transform("True"));
    	Assert.assertEquals(Boolean.TRUE, transform.transform("TRUE"));
    	Assert.assertEquals(Boolean.FALSE, transform.transform("false"));
    	Assert.assertEquals(Boolean.FALSE, transform.transform("False"));
    	Assert.assertEquals(Boolean.FALSE, transform.transform("FALSE"));
    	Assert.assertEquals("Hi", transform.transform("Hi"));
    }

    @Test
    public void testClassTransform() throws Exception {
    	Transform transform = new ClassTransform(SampleTransform.class);
    	SampleTransform test = (SampleTransform)transform.transform("Okay");
    	Assert.assertEquals(test.getData(), "Okay");
    	String xml = transform.toXml(0);
    	transform = (ClassTransform)getTransform(xml);
        test = (SampleTransform)transform.transform("Okay");
        Assert.assertEquals(test.getData(), "Okay");
        test = (SampleTransform)transform.transform("Okay");
        Assert.assertEquals(test.getData(), "Okay");
    }
    
    public static class SampleTransform {
    	private String _data;
    	public SampleTransform(String data) {
    		_data = data;
    	}
    	public String getData() {
    		return _data;
    	}
    };
    
    @Test
    public void testTransformSubtype() throws Exception {
        Transform transform = new SubstringTransform(1, 3);
        String string = transform.toXml(0);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(new InputSource(new StringReader(string)));
        NodeList elements = document.getChildNodes();
        Transform newTransform = Transform.newTransform((Element)elements.item(0));
        Assert.assertEquals("bc", newTransform.transform("abcde"));
    }

    @Test
    public void testPatternFormatting() throws Exception {
        _parser = (MapTransform)getTransform(pattern);
        String formatted = _parser.toXml(0);
        Assert.assertEquals(formatted, pattern);
        formatted = _parser.toXml(2);
        Assert.assertEquals(formatted.replaceAll("\n  ", "\n").substring(2), pattern);
    }

    @Test
    public void testDumpRacf() throws Exception {
        Transform transform = new MapTransform(RacfInfo._parser);
        String xml = new MapTransform(RacfInfo._parser).toXml(0);
        transform = Transform.newTransform(xml);
        System.out.println(xml);
    }

    private static Transform getTransform(String string) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(new InputSource(new StringReader(string)));
        NodeList elements = document.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++)
            if (elements.item(i) instanceof Element) {
                return Transform.newTransform((Element) elements.item(i));
            }
        return null;
    }

    //???
    /**
     * This class is used to generate several alternate versions of a RACF output
     * parser.
     */
    public static class RacfInfo {
        private static final List<PatternNode> _parser = new LinkedList<PatternNode>();
        static {
            try {
                _parser.add(new PatternNode("USERID",           "USER=(\\w{1,8})"));
                _parser.add(new PatternNode("NAME",             "NAME=(.*?)\\s+(?=OWNER=)"));       
                _parser.add(new PatternNode("OWNER",            "OWNER=(\\w{1,8})", false, false,
                    new Transform[] {
                        new SubstituteTransform("^$", "UNKNOWN"),
                    }));
                _parser.add(new PatternNode("DFLTGRP",          "DEFAULT-GROUP=(\\w{1,8})"));       
                _parser.add(new PatternNode("PASSDATE",         "PASSDATE=(\\S{0,6})"));
                _parser.add(new PatternNode("PASSWORD INTERVAL","PASS-INTERVAL=(\\S*)"));       
                _parser.add(new PatternNode("PHRASEDATE",       "PHRASEDATE=(.*?)\\s+\\n", true, 

    false, null));      
                _parser.add(new PatternNode("ATTRIBUTES",       "((ATTRIBUTES=.*\\n\\s*)+)", true, 

    false,
                        new Transform[] {
                            new SubstituteTransform("ATTRIBUTES=(\\S+)\\s+", "$1 "),
                            new SubstituteTransform("(.*)\\s", "$1"),
                            new SubstituteTransform("^$", "NONE"),
                            new SplitTransform("\\s"),
                        }));    
                _parser.add(new PatternNode("CLAUTH", "CLASS AUTHORIZATIONS=([^\\n]*(\\s{23}.+\\n)*)", true, false,
                        new Transform[] {
                            new SubstituteTransform("(.*)\\s", "$1"),
                            new SplitTransform("\\s+"),
                    }));            
                _parser.add(new PatternNode("DATA", "INSTALLATION-DATA=([^\\n]*(\\s{20}.+\\n)*)", true, false,
                        new Transform[] {
                            new SubstituteTransform("^(.{50})[^\\n]+", "$1"),
                            new SubstituteTransform("\\n\\s{20}(.{50})[^\\n]+", "$1"),
                            new SubstituteTransform("\\n", ""),
                            new SubstituteTransform("^$", "NO-INSTALLATION-DATA"),
                    }));            
                _parser.add(new PatternNode("RACF.GROUPS", "((\\s+GROUP=\\w+\\s+AUTH=.+?CONNECT-OWNER=([^\\n]+\\n){4})+)", true, false,
                        new Transform[] {
                            new SubstituteTransform(".*?GROUP=(\\w+)\\s+AUTH=.+?CONNECT-OWNER=\\w+([^\\n]+\\n){4}", "$1 "),
                            new SplitTransform("\\s+"),
                        }));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //???
}


