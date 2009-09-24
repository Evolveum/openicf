/**
 * 
 */
package org.identityconnectors.oracleerp;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.script.ScriptBuilder;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Test;

/**
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class AccountOperationGetUserAfterActionTest extends OracleERPTestsBase {
    private static final String HR_EMP_NUM = "hr_emp_num";
    private static final String USER_AFTER_ACTION = "org.identityconnectors.oracleerp.OracleERPConnector/config/GetUserAfterAction.groovy";

    /**
     * Test method for {@link org.identityconnectors.oracleerp.AccountOperationGetUserAfterAction#runScriptOnConnector(java.lang.Object, org.identityconnectors.framework.common.objects.ConnectorObjectBuilder)}.
     */
    @Test
    public void testRunScriptOnConnector() {
        final OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        final String scriptText = IOUtil.getResourceAsString(this.getClass(), USER_AFTER_ACTION);
        final ScriptBuilder sb = new ScriptBuilder().setScriptLanguage(GROOVY).setScriptText(scriptText );
        cfg.setUserAfterActionScript(sb.build());
        
        final OracleERPConnector c = getConnector(cfg);
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);
        replaceNameByRandom(attrs);
        
        
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        final ConnectorObject co = results.get(0);
        final Set<Attribute> returned = co.getAttributes();
        
        //The new, script added attribute must exist and have value 5
        final Attribute find = AttributeUtil.find(HR_EMP_NUM, returned);
        assertNotNull(HR_EMP_NUM, find );
        final List<Object> value = find.getValue();
        assertEquals(HR_EMP_NUM, 1, value.size() );
        assertEquals(HR_EMP_NUM, "5", value.get(0) );
   }
    
    /**
     * Test method for {@link org.identityconnectors.oracleerp.AccountOperationGetUserAfterAction#runScriptOnConnector(java.lang.Object, org.identityconnectors.framework.common.objects.ConnectorObjectBuilder)}.
     */
    @Test(expected=ConnectorException.class)
    public void testRunScriptOnConnectorError() {
        final OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        final String scriptText = IOUtil.getResourceAsString(this.getClass(), RUN_ERROR_ACTION);
        final ScriptBuilder sb = new ScriptBuilder().setScriptLanguage(GROOVY).setScriptText(scriptText );
        cfg.setUserAfterActionScript(sb.build());
        
        final OracleERPConnector c = getConnector(cfg);
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);
        replaceNameByRandom(attrs);
        
        
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        TestHelpers.searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        fail("expect no connector object");
   }
    
    /**
     * Test method for {@link org.identityconnectors.oracleerp.AccountOperationGetUserAfterAction#runScriptOnConnector(java.lang.Object, org.identityconnectors.framework.common.objects.ConnectorObjectBuilder)}.
     */
    @Test(expected=ConnectorException.class)
    public void testRunScriptOnConnectorParsedError() {
        final OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        final ScriptBuilder sb = new ScriptBuilder().setScriptLanguage(GROOVY).setScriptText("return (  ;");
        cfg.setUserAfterActionScript(sb.build());
        
        final OracleERPConnector c = getConnector(cfg);
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);
        replaceNameByRandom(attrs);
        
        
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        TestHelpers.searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        fail("expect no connector object");
   }
}
