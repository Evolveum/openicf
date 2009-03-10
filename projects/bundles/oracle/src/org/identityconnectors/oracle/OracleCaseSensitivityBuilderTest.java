/**
 * 
 */
package org.identityconnectors.oracle;

import org.identityconnectors.common.security.GuardedString;
import org.junit.*;

/**
 * Tests for creation of OracleCaseSensitivity using builder
 * @author kitko
 *
 */
public class OracleCaseSensitivityBuilderTest {
    
    /** Tests manual sensitivity with formatters */
    @Test
    public void testCreateExplicitFormatters(){
        OracleCaseSensitivity cs = new OracleCaseSensitivityBuilder().defineFormatters(new CSTokenFormatter.CSTokenFormatterBuilder().setAttribute(OracleUserAttribute.SCHEMA).setToUpper(false).build()).build();
        Assert.assertFalse("ToUpper must be false when explicitely set", cs.getAttributeFormatter(OracleUserAttribute.SCHEMA).isToUpper());
        cs = new OracleCaseSensitivityBuilder().defineFormatters(new CSTokenFormatter.CSTokenFormatterBuilder().setAttribute(OracleUserAttribute.SCHEMA).setToUpper(true).build()).build();
        Assert.assertTrue("ToUpper must be true when explicitely set", cs.getAttributeFormatter(OracleUserAttribute.SCHEMA).isToUpper());
    }
    
    /** Test create all formatters */
    @Test
    public void testCreateAllFormatters(){
        OracleCaseSensitivity cs = new OracleCaseSensitivityBuilder().defineFormatters().build();
        for(OracleUserAttribute oua : OracleUserAttribute.values()){
            Assert.assertNotNull("Formatter for attribute " + oua + " cannot be null", cs.getAttributeFormatter(oua));
        }
        //By default we have upper and quates
        Assert.assertEquals("\"MYSCHEMA\"",cs.formatDefaultTableSpace("mySchema"));
        Assert.assertEquals(new GuardedString("\"MYPASSWORD\"".toCharArray()),cs.formatPassword(new GuardedString("myPassword".toCharArray())));
    }
    
    /** Test create formatters from string pattern */
    @Test
    public void testCreateFromString(){
        OracleCaseSensitivity cs = new OracleCaseSensitivityBuilder().defineFormatters("default").build();
        for(OracleUserAttribute oua : OracleUserAttribute.values()){
            Assert.assertNotNull("Formatter for attribute " + oua + " cannot be null", cs.getAttributeFormatter(oua));
        }
        cs = new OracleCaseSensitivityBuilder().defineFormatters("all={upper=false}").build();
        for(OracleUserAttribute oua : OracleUserAttribute.values()){
            Assert.assertFalse("Upper must be set to false",cs.getAttributeFormatter(oua).isToUpper());
        }

            
        
    }
}
