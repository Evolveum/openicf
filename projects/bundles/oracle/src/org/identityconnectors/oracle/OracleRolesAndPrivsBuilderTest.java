/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.*;

import org.junit.*;
import org.junit.matchers.JUnitMatchers;

/**
 * Tests for OracleRolesAndPrivsBuilder
 * @author kitko
 *
 */
public class OracleRolesAndPrivsBuilderTest {

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleRolesAndPrivsBuilder#buildCreateSQL(java.lang.String, java.util.List, java.util.List)}.
     */
    @Test
    public void testBuildCreateSQL() {
        OracleRolesAndPrivsBuilder builder = new OracleRolesAndPrivsBuilder();
        List<String> roles = Arrays.asList("myRole1","myRole2");
        List<String> privileges = Arrays.asList("CREATE SESSION","SELECT ON MYTABLE");
        final List<String> sql = builder.buildCreateSQL("testUser", roles, privileges);
        Assert.assertNotNull(sql);
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant \"myRole1\" to \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant \"myRole2\" to \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant \"CREATE SESSION\" to \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant \"SELECT ON MYTABLE\" to \"testUser\""));
        System.out.println(sql);
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleRolesAndPrivsBuilder#buildAlter(java.lang.String, java.util.List, java.util.List)}.
     */
    @Test
    public void testBuildAlter() {
        //TODO
    }

}
