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
     * Test method for {@link org.identityconnectors.oracle.OracleRolesAndPrivsBuilder#buildGrantSQL(java.lang.String, java.util.List, java.util.List)}.
     */
    @Test
    public void testBuildCreateSQL() {
        OracleRolesAndPrivsBuilder builder = new OracleRolesAndPrivsBuilder(new OracleCaseSensitivityBuilder().build());
        List<String> roles = Arrays.asList("myRole1","myRole2");
        List<String> privileges = Arrays.asList("CREATE SESSION","SELECT ON MYTABLE");
        List<String> sql = builder.buildGrantRolesSQL("testUser", roles);
        Assert.assertNotNull(sql);
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant \"myRole1\" to \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant \"myRole2\" to \"testUser\""));
        sql = builder.buildGrantPrivilegesSQL("testUser", privileges);
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant CREATE SESSION to \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant SELECT ON MYTABLE to \"testUser\""));
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
