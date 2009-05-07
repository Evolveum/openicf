/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for creation of OracleCaseSensitivity using builder
 * @author kitko
 *
 */
public class OracleCaseSensitivitySetupTest {
    
    /** Tests manual sensitivity with formatters */
    @Test
    public void testCreateExplicitFormatters(){
        OracleCaseSensitivitySetup cs = new OracleCaseSensitivityBuilder().defineFormatters(new CSTokenFormatter.Builder().setAttribute(OracleUserAttributeCS.SCHEMA).setQuatesChar("AAA").build()).build();
        Assert.assertEquals("AAA",cs.getAttributeFormatter(OracleUserAttributeCS.SCHEMA).getQuatesChar());
        CSTokenFormatter formatter = CSTokenFormatter.build(OracleUserAttributeCS.PROFILE, "BBB");
        assertEquals(OracleUserAttributeCS.PROFILE, formatter.getAttribute());
        assertEquals("BBB", formatter.getQuatesChar());
    }
    
    /** Tests manual sensitivity with normalizers */
    @Test
    public void testCreateExplicitNormalizers(){
        OracleCaseSensitivitySetup cs = new OracleCaseSensitivityBuilder().defineNormalizers(new CSTokenNormalizer.Builder().setAttribute(OracleUserAttributeCS.SCHEMA).setToUpper(true).build()).build();
        Assert.assertTrue(cs.getAttributeNormalizer(OracleUserAttributeCS.SCHEMA).isToUpper());
        CSTokenNormalizer normalizer = CSTokenNormalizer.build(OracleUserAttributeCS.DEF_TABLESPACE, false);
        assertEquals(OracleUserAttributeCS.DEF_TABLESPACE, normalizer.getAttribute());
        assertEquals(false, normalizer.isToUpper());
        
    }
    
    
    /** Test create all formatters */
    @Test
    public void testCreateAllFormatters(){
        OracleCaseSensitivitySetup cs = new OracleCaseSensitivityBuilder().defineFormatters().build();
        for(OracleUserAttributeCS oua : OracleUserAttributeCS.values()){
            Assert.assertNotNull("Formatter for attribute " + oua + " cannot be null", cs.getAttributeFormatter(oua));
        }
    }
    
    /** Test create all normalizers */
    @Test
    public void testCreateAllNormalizers(){
        OracleCaseSensitivitySetup cs = new OracleCaseSensitivityBuilder().build();
        for(OracleUserAttributeCS oua : OracleUserAttributeCS.values()){
            Assert.assertNotNull("Normalizer for attribute " + oua + " cannot be null", cs.getAttributeNormalizer(oua));
        }
    }
    
    /** Test create formatter and normalizers from string map */
    @Test
    public void testCreateFromFormat(){
        new OracleCaseSensitivityBuilder().parseMap("default").build();
        try{
            new OracleCaseSensitivityBuilder().parseMap("unknown").build();
            fail("Must fail for unknown");
        }
        catch(RuntimeException e){}
        final OracleCaseSensitivitySetup cs = new OracleCaseSensitivityBuilder().parseMap("formatters={USER={quates=\"},ROLE={quates=AAA}},normalizers={ALL={upper=false}}").build();
        assertEquals("AAA",cs.getAttributeFormatter(OracleUserAttributeCS.ROLE).getQuatesChar());
        assertEquals(false,cs.getAttributeNormalizer(OracleUserAttributeCS.USER).isToUpper());
        
    }
    
    
}
