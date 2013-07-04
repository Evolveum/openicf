/**
 *
 */
package org.identityconnectors.oracleerp;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * @author petr
 *
 */
public class OracleERPUtilTest {
    /**
     * Test method for
     * {@link org.identityconnectors.oracleerp.OracleERPUtil#whereAnd(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testWhereAnd() {
        final String baseSelect = "SELECT 1 FROM x";
        final String empty = "";
        final String where1 = " WHERE a=1";
        final String where2 = " where b=2";
        final String where3 = " c=3";
        AssertJUnit.assertEquals("BASE_SELECT, WHERE2", "SELECT 1 FROM x WHERE b=2", OracleERPUtil
                .whereAnd(baseSelect, where2));
        AssertJUnit.assertEquals("BASE_SELECT + WHERE1, WHERE2",
                "SELECT 1 FROM x WHERE ( a=1 ) AND ( b=2 )", OracleERPUtil.whereAnd(baseSelect
                        + where1, where2));
        AssertJUnit.assertEquals("BASE_SELECT + WHERE1, EMPTY", "SELECT 1 FROM x WHERE a=1",
                OracleERPUtil.whereAnd(baseSelect + where1, empty));
        AssertJUnit.assertEquals("BASE_SELECT, EMPTY", "SELECT 1 FROM x", OracleERPUtil.whereAnd(
                baseSelect, empty));
        AssertJUnit.assertEquals("BASE_SELECT + WHERE1, WHERE3",
                "SELECT 1 FROM x WHERE ( a=1 ) AND ( c=3 )", OracleERPUtil.whereAnd(baseSelect
                        + where1, where3));
        AssertJUnit.assertEquals("BASE_SELECT, WHERE3", "SELECT 1 FROM x WHERE c=3", OracleERPUtil
                .whereAnd(baseSelect, where3));
    }
}
