/**
 * 
 */
package org.identityconnectors.oracleerp;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author petr
 *
 */
public class OracleERPUtilTest {
    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#whereAnd(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testWhereAnd() {
        final String BASE_SELECT = "SELECT 1 FROM x";
        final String EMPTY = "";
        final String WHERE1 = " WHERE a=1";    
        final String WHERE2 = " where b=2";   
        final String WHERE3 = " c=3";   
        assertEquals("BASE_SELECT, WHERE2", "SELECT 1 FROM x WHERE b=2", OracleERPUtil.whereAnd(BASE_SELECT, WHERE2));
        assertEquals("BASE_SELECT + WHERE1, WHERE2", "SELECT 1 FROM x WHERE ( a=1 ) AND ( b=2 )", OracleERPUtil.whereAnd(BASE_SELECT + WHERE1, WHERE2));
        assertEquals("BASE_SELECT + WHERE1, EMPTY", "SELECT 1 FROM x WHERE a=1", OracleERPUtil.whereAnd(BASE_SELECT + WHERE1, EMPTY));
        assertEquals("BASE_SELECT, EMPTY", "SELECT 1 FROM x", OracleERPUtil.whereAnd(BASE_SELECT, EMPTY));
        assertEquals("BASE_SELECT + WHERE1, WHERE3", "SELECT 1 FROM x WHERE ( a=1 ) AND ( c=3 )", OracleERPUtil.whereAnd(BASE_SELECT + WHERE1, WHERE3));
        assertEquals("BASE_SELECT, WHERE3", "SELECT 1 FROM x WHERE c=3", OracleERPUtil.whereAnd(BASE_SELECT, WHERE3));
    }
}
