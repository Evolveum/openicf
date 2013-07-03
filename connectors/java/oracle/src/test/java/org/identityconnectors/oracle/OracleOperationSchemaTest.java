/**
 *
 */
package org.identityconnectors.oracle;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.testng.annotations.Test;

/**
 * @author kitko
 *
 */
public class OracleOperationSchemaTest extends OracleConnectorAbstractTest {

    @Test(groups = { "integration" })
    public void testSchema() {
        Schema schema = facade.schema();
        assertNotNull(schema);
        ObjectClassInfo account = schema.findObjectClassInfo(ObjectClass.ACCOUNT_NAME);
        assertNotNull(account);
        Set<String> attributeNames = new HashSet<String>(OracleConstants.ALL_ATTRIBUTE_NAMES);
        for (AttributeInfo info : account.getAttributeInfo()) {
            for (Iterator<String> i = attributeNames.iterator(); i.hasNext();) {
                if (info.is(i.next())) {
                    i.remove();
                }
            }
        }
        // Assert.assertThat("All attributes must be present in schema",attributeNames,
        // new IsEqual<Set<String>>(Collections.<String>emptySet()));
        assertThat(attributeNames).isSameAs(Collections.<String> emptySet());
    }

}
