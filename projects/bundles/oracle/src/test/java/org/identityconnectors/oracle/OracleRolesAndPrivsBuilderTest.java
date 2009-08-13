/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.*;

import org.identityconnectors.test.common.TestHelpers;
import org.junit.*;
import org.junit.matchers.JUnitMatchers;

/**
 * Tests for OracleRolesAndPrivsBuilder
 * @author kitko
 *
 */
public class OracleRolesAndPrivsBuilderTest {

    /**
     * Test grant methods
     */
    @Test
    public void testBuildGrants() {
        OracleRolesAndPrivsBuilder builder = new OracleRolesAndPrivsBuilder(new OracleCaseSensitivityBuilder(TestHelpers.createDummyMessages()).build());
        List<String> roles = Arrays.asList("myRole1","myRole2");
        List<String> privileges = Arrays.asList("CREATE SESSION","SELECT ON MYTABLE");
        List<String> sql = builder.buildGrantRoles("testUser", roles);
        Assert.assertNotNull(sql);
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant \"myRole1\" to \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant \"myRole2\" to \"testUser\""));
        sql = builder.buildGrantPrivileges("testUser", privileges);
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant CREATE SESSION to \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("grant SELECT ON MYTABLE to \"testUser\""));
    }

    /**
     * Test revoke methods
     */
    @Test
    public void testBuildRevokes() {
        OracleRolesAndPrivsBuilder builder = new OracleRolesAndPrivsBuilder(new OracleCaseSensitivityBuilder(TestHelpers.createDummyMessages()).build());
        List<String> roles = Arrays.asList("myRole1","myRole2");
        List<String> privileges = Arrays.asList("CREATE SESSION","SELECT ON MYTABLE");
        List<String> sql = builder.buildRevokeRoles("testUser", roles);
        Assert.assertNotNull(sql);
        Assert.assertThat(sql, JUnitMatchers.hasItem("revoke \"myRole1\" from \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("revoke \"myRole2\" from \"testUser\""));
        sql = builder.buildRevokePrivileges("testUser", privileges);
        Assert.assertNotNull(sql);
        Assert.assertThat(sql, JUnitMatchers.hasItem("revoke CREATE SESSION from \"testUser\""));
        Assert.assertThat(sql, JUnitMatchers.hasItem("revoke SELECT ON MYTABLE from \"testUser\""));
        
        
    }

}
