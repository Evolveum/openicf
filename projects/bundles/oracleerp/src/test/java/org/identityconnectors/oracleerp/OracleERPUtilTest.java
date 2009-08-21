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

    /**77
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getAttributeInfos(org.identityconnectors.framework.common.objects.Schema, java.lang.String)}.
     *
    @Test
    public void testGetAttributeInfos() {
        fail("Not yet implemented"); // TODO testGetAttributeInfos
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getCreatableAttributes(java.util.Set)}.
     *
    @Test
    public void testGetCreatableAttributes() {
        fail("Not yet implemented"); // TODO testGetCreatableAttributes
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getUpdatableAttributes(java.util.Set)}.
     *
    @Test
    public void testGetUpdatableAttributes() {
        fail("Not yet implemented"); // TODO testGetUpdatableAttributes
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getReadableAttributes(java.util.Set)}.
     *
    @Test
    public void testGetReadableAttributes() {
        fail("Not yet implemented"); // TODO testGetReadableAttributes
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getAttributesToGet(org.identityconnectors.framework.common.objects.OperationOptions, java.util.Set, org.identityconnectors.oracleerp.Messages)}.
     *
    @Test
    public void testGetAttributesToGet() {
        fail("Not yet implemented"); // TODO testGetAttributesToGet
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#stringToTimestamp(java.lang.String)}.
     *
    @Test
    public void testStringToTimestamp() {
        fail("Not yet implemented"); // TODO testStringToTimestamp
    }

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

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getColumnValues(java.sql.ResultSet)}.
     *
    @Test
    public void testGetColumnValues() {
        fail("Not yet implemented"); // TODO testGetColumnValues
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getColumn(java.sql.ResultSet, int)}.
     *
    @Test
    public void testGetColumn() {
        fail("Not yet implemented"); // TODO testGetColumn
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#extractLong(java.lang.String, java.util.Map)}.
     *
    @Test
    public void testExtractLong() {
        fail("Not yet implemented"); // TODO testExtractLong
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#extractDate(java.lang.String, java.util.Map)}.
     *
    @Test
    public void testExtractDate() {
        fail("Not yet implemented"); // TODO testExtractDate
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#normalizeStrDate(java.lang.String)}.
     *
    @Test
    public void testNormalizeStrDate() {
        fail("Not yet implemented"); // TODO testNormalizeStrDate
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getCurrentDate()}.
     *
    @Test
    public void testGetCurrentDate() {
        fail("Not yet implemented"); // TODO testGetCurrentDate
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#convertToListString(java.util.List)}.
     *
    @Test
    public void testConvertToListString() {
        fail("Not yet implemented"); // TODO testConvertToListString
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#listToCommaDelimitedString(java.util.List)}.
     *
    @Test
    public void testListToCommaDelimitedString() {
        fail("Not yet implemented"); // TODO testListToCommaDelimitedString
    } 

    /**
     * Test method for {@link org.identityconnectors.oracleerp.OracleERPUtil#getFilterId(org.identityconnectors.dbcommon.FilterWhereBuilder)}.
     *
    @Test
    public void testGetFilterId() {
        fail("Not yet implemented"); // TODO testGetFilterId
    }*/

}
