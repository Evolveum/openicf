/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.*;
import static org.identityconnectors.oracle.OracleConnector.*;

import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.*;

/**
 * Tests for OracleCreateAttributesReader
 * @author kitko
 *
 */
public class OracleCreateAttributesReaderTest {

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleCreateAttributesReader#readCreateAuthAttributes(java.util.Set, org.identityconnectors.oracle.OracleUserAttributes)}.
     */
    @Test
    public final void testReadCreateAuthAttributes() {
        final OracleCreateAttributesReader reader = new OracleCreateAttributesReader(TestHelpers.createDummyMessages());
        OracleUserAttributes caAttributes = new OracleUserAttributes();
        caAttributes.userName = "testUser";
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(ORACLE_AUTHENTICATION_ATTR_NAME, ORACLE_AUTH_LOCAL));
        attributes.add(AttributeBuilder.buildPassword("myPassword".toCharArray()));
        reader.readCreateAuthAttributes(attributes, caAttributes);
        Assert.assertEquals(OracleAuthentication.LOCAL, caAttributes.auth);
        Assert.assertNotNull("Password must not be null",caAttributes.password);
    }
    
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleCreateAttributesReader#readCreateRestAttributes(java.util.Set, org.identityconnectors.oracle.OracleUserAttributes)}.
     */
    @Test
    public final void testReadCreateRestAttributes() {
        final OracleCreateAttributesReader reader = new OracleCreateAttributesReader(TestHelpers.createDummyMessages());
        OracleUserAttributes caAttributes = new OracleUserAttributes();
        caAttributes.userName = "testUser";
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.buildPasswordExpired(true));
        attributes.add(AttributeBuilder.build(ORACLE_DEF_TS_ATTR_NAME, "defts"));
        attributes.add(AttributeBuilder.build(ORACLE_TEMP_TS_ATTR_NAME, "tempts"));
        attributes.add(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME, "myprofile"));
        attributes.add(AttributeBuilder.buildEnabled(true));
        attributes.add(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"30M"));
        attributes.add(AttributeBuilder.build(ORACLE_TEMP_TS_QUOTA_ATTR_NAME,"100M"));
        reader.readCreateRestAttributes(attributes, caAttributes);
        Assert.assertEquals("defts", caAttributes.defaultTableSpace);
        Assert.assertEquals(true, caAttributes.expirePassword);
        Assert.assertEquals("tempts", caAttributes.tempTableSpace);
        Assert.assertEquals("myprofile", caAttributes.profile);
        Assert.assertEquals(true, caAttributes.enable);
        Assert.assertNotNull("Default quata is null", caAttributes.defaultTSQuota);
        Assert.assertEquals("30M", caAttributes.defaultTSQuota.size);
        Assert.assertNotNull("Temp quata is null", caAttributes.tempTSQuota);
        Assert.assertEquals("100M", caAttributes.tempTSQuota.size);
        
        
    }


}


