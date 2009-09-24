/**
 * 
 */
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.OracleERPUtil.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.IOUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Test;

/**
 * @author petr
 *
 */
public class OracleERPOperationRunScriptOnConnectorTest extends OracleERPTestsBase {

    private static final String EXPECTED_MAIL = "person@somewhere.com";
    private static final String UPDATE_AFTER_ACTION = "org.identityconnectors.oracleerp.OracleERPConnector/config/UpdateAfterAction.groovy";

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPOperationRunScriptOnConnector#runScriptOnConnector(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)}.
     */
    @Test
    public void testRunScriptOnConnector() {
        final OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        final String scriptText = IOUtil.getResourceAsString(this.getClass(), UPDATE_AFTER_ACTION);
        final OracleERPConnector c = getConnector(cfg);
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);
        replaceNameByRandom(attrs);
        
        
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        
        Map<String, Object> scriptArguments = new HashMap<String, Object>();
        scriptArguments.put(Name.NAME, AttributeUtil.getNameFromAttributes(attrs));
        scriptArguments.put(OperationalAttributes.PASSWORD_NAME, AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attrs));
        scriptArguments.put(ACTION, "update");
        scriptArguments.put(TIMING, "after");
        scriptArguments.put(ATTRIBUTES, AttributeUtil.toMap(attrs));
        
        ScriptContext request = new ScriptContext(GROOVY, scriptText, scriptArguments);
        final Object ret = c.runScriptOnConnector(request , null);
        assertNotNull("mail not returned", ret);
        assertEquals("mail value", EXPECTED_MAIL, ret);
    }
    

    /**
     * Test method for {@link org.identityconnectors.oracleerp.AccountOperationGetUserAfterAction#runScriptOnConnector(java.lang.Object, org.identityconnectors.framework.common.objects.ConnectorObjectBuilder)}.
     */
    @Test(expected=ConnectorException.class)
    public void testRunScriptOnConnectorError() {
        final OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        final String scriptText = IOUtil.getResourceAsString(this.getClass(), RUN_ERROR_ACTION);
        final OracleERPConnector c = getConnector(cfg);
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);
        replaceNameByRandom(attrs);
        
        
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        
        Map<String, Object> scriptArguments = new HashMap<String, Object>();
        scriptArguments.put(Name.NAME, AttributeUtil.getNameFromAttributes(attrs));
        scriptArguments.put(OperationalAttributes.PASSWORD_NAME, AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attrs));
        scriptArguments.put(ACTION, "update");
        scriptArguments.put(TIMING, "after");
        scriptArguments.put(ATTRIBUTES, AttributeUtil.toMap(attrs));
        
        ScriptContext request = new ScriptContext(GROOVY, scriptText, scriptArguments);
        final Object ret = c.runScriptOnConnector(request , null);
        assertNotNull("mail not returned", ret);
        assertEquals("mail value", EXPECTED_MAIL, ret);
   }
    
    /**
     * Test method for {@link org.identityconnectors.oracleerp.AccountOperationGetUserAfterAction#runScriptOnConnector(java.lang.Object, org.identityconnectors.framework.common.objects.ConnectorObjectBuilder)}.
     */
    @Test(expected=ConnectorException.class)
    public void testRunScriptOnConnectorParsedError() {
        final OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        final String scriptText = "return (  ;";
        final OracleERPConnector c = getConnector(cfg);
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);
        replaceNameByRandom(attrs);
        
        
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        
        Map<String, Object> scriptArguments = new HashMap<String, Object>();
        scriptArguments.put(Name.NAME, AttributeUtil.getNameFromAttributes(attrs));
        scriptArguments.put(OperationalAttributes.PASSWORD_NAME, AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attrs));
        scriptArguments.put(ACTION, "update");
        scriptArguments.put(TIMING, "after");
        scriptArguments.put(ATTRIBUTES, AttributeUtil.toMap(attrs));
        
        ScriptContext request = new ScriptContext(GROOVY, scriptText, scriptArguments);
        final Object ret = c.runScriptOnConnector(request , null);
        assertNotNull("mail not returned", ret);
        assertEquals("mail value", EXPECTED_MAIL, ret);
   }
}
