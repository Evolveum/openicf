package org.identityconnectors.databasetable;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.contract.data.DataProvider;
import org.identityconnectors.contract.data.GroovyDataProvider;
import org.identityconnectors.databasetable.util.PropertiesParser;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.*;

import static org.identityconnectors.common.ByteUtil.randomBytes;

public class DatabaseTablePostgreSQLTests extends DatabaseTableTestBase {

    public DatabaseTablePostgreSQLTests() throws IOException {
    }

    PropertiesParser parser = new PropertiesParser("src/test/resources/TEST.properties");
    private final String testUID = "a7924d33-c3cd-4e1c-91ad-a5070c344ad7";
    private final String testMockUID = "ce014695-48e3-4a4a-9e6f-c06c104f7dd7";

    @Override
    protected DatabaseTableConfiguration getConfiguration() throws Exception {

        DatabaseTableConfiguration cfg = new DatabaseTableConfiguration();
        cfg.setJdbcDriver(parser.fetchTestDataSingleValue("jdbcDriver"));
        cfg.setKeyColumn(parser.fetchTestDataSingleValue("keyColumn"));
        cfg.setPasswordColumn(parser.fetchTestDataSingleValue("passwordColumn"));
        cfg.setTable(parser.fetchTestDataSingleValue("table"));
        cfg.setJdbcUrlTemplate(parser.fetchTestDataSingleValue("jdbcUrlTemplate"));
        cfg.setHost(parser.fetchTestDataSingleValue("host"));
        cfg.setDatabase(parser.fetchTestDataSingleValue("database"));
        cfg.setPort(parser.fetchTestDataSingleValue("port"));
        cfg.setUser(parser.fetchTestDataSingleValue("user"));
        cfg.setChangeLogColumn(parser.fetchTestDataSingleValue("changeLogColumn"));
        //cfg.setAlreadyExistMessages(parser.fetchTestDataSingleValue("alreadyExists"));
        cfg.setPassword(new GuardedString(
                parser.fetchTestDataSingleValue("password").toCharArray()));
        //dp.loadConfiguration(POSTGRE_CONFIGURATINON, cfg);
        //cfg.setConnectorMessages(TestHelpers.createDummyMessages());
        cfg.setSQLStateExceptionHandling(false);
        return cfg;
    }

    @Override
    protected Set<Attribute> getCreateAttributeSet(DatabaseTableConfiguration cfg) throws Exception {

        Set<Attribute> ret = new HashSet<Attribute>();
        ret.add(AttributeBuilder.build(Name.NAME, testUID));
        if (StringUtil.isNotBlank(cfg.getPasswordColumn())) {
            ret.add(AttributeBuilder.buildPassword(new GuardedString("Test Pasword".toCharArray())));
        } else {
            ret.add(AttributeBuilder.build(PASSWORD, "Test Pasword"));
        }
        ret.add(AttributeBuilder.build(MANAGER, MANAGER));
        ret.add(AttributeBuilder.build(MIDDLENAME, MIDDLENAME));
        ret.add(AttributeBuilder.build(FIRSTNAME, FIRSTNAME));
        ret.add(AttributeBuilder.build(LASTNAME, LASTNAME));
        ret.add(AttributeBuilder.build(EMAIL, EMAIL + "@te.st.com"));
        ret.add(AttributeBuilder.build(DEPARTMENT, DEPARTMENT));
        ret.add(AttributeBuilder.build(TITLE, TITLE));
        ret.add(AttributeBuilder.build(SALARY, new BigDecimal("360536.75")));
        ret.add(AttributeBuilder.build(JPEGPHOTO, randomBytes(r, 2000)));

        if (!cfg.getChangeLogColumn().equalsIgnoreCase(AGE)) {
            ret.add(AttributeBuilder.build(AGE, r.nextInt(100)));
        }
        if (!cfg.getChangeLogColumn().equalsIgnoreCase(ACCESSED)) {
            ret.add(AttributeBuilder.build(ACCESSED, r.nextLong()));
        }

        if (!cfg.getChangeLogColumn().equalsIgnoreCase(CHANGELOG)) {
            ret.add(AttributeBuilder.build(CHANGELOG, new Timestamp(System.currentTimeMillis()).getTime()));
        }

        return ret;

    }

    @Override
    protected Set<Attribute> getModifyAttributeSet(DatabaseTableConfiguration cfg) throws Exception {
        return getCreateAttributeSet(cfg);
    }

    @Override // Added an uuid value as mock user name
    @Test(expectedExceptions = InvalidCredentialException.class)
    public void testAuthenticateWrongOriginal() throws Exception {
        log.ok("testAuthenticateOriginal");
        final DatabaseTableConfiguration cfg = getConfiguration();
        con = getConnector(cfg);
        // this should throw InvalidCredentials exception, as we query a
        // non-existing user
        con.authenticate(ObjectClass.ACCOUNT, testMockUID, new GuardedString("MOM".toCharArray()), null);
    }

    @Override
    @Test(expectedExceptions = InvalidCredentialException.class)
    public void testResolveUsernameWrongOriginal() throws Exception {
        log.ok("testAuthenticateOriginal");
        final DatabaseTableConfiguration cfg = getConfiguration();
        con = getConnector(cfg);
        // this should throw InvalidCredentials exception, as we query a
        // non-existing user
        con.resolveUsername(ObjectClass.ACCOUNT, testMockUID, null);
    }


    @Override // OVERRIDE for type change
    @Test
    public void testSearchWithNullPassword() throws Exception {
        log.ok("testSearchWithNullPassword");
        final String SQL_TEMPLATE = "UPDATE {0} SET password = null WHERE {1} = ?";
        final DatabaseTableConfiguration cfg = getConfiguration();
        final String sql = MessageFormat.format(SQL_TEMPLATE, cfg.getTable(), cfg.getKeyColumn());
        con = getConnector(cfg);
        PreparedStatement ps = null;
        DatabaseTableConnection conn = DatabaseTableConnection.createDBTableConnection(cfg);
        final Set<Attribute> expected = getCreateAttributeSet(cfg);
        Uid uid = con.create(ObjectClass.ACCOUNT, expected, null);

        //set password to null
        //expected.setPassword((String) null);
        try {
            List<SQLParam> values = new ArrayList<SQLParam>();
            values.add(new SQLParam("user", uid.getUidValue(), Types.OTHER, "uuid"));
            ps = conn.prepareStatement(sql, values);
            ps.execute();
            conn.commit();
        } finally {
            SQLUtil.closeQuietly(ps);
        }
        // attempt to get the record back..
        List<ConnectorObject> results = TestHelpers.searchToList(con, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        AssertJUnit.assertTrue("expect 1 connector object", results.size() == 1);
        final Set<Attribute> attributes = results.get(0).getAttributes();
        attributeSetsEquals(con.schema(), expected, attributes);
    }


    // Type changes
    @Override
    @Test
    public void testSyncIncemental() throws Exception {
        final String ERR1 = "Could not find new object.";
        final String SQL_TEMPLATE = "UPDATE Accounts SET changelog = ? WHERE accountId = ?";
        // create connector
        final DatabaseTableConfiguration cfg = getConfiguration();
        con = getConnector(cfg);
        final Set<Attribute> expected = getCreateAttributeSet(cfg);

        // create the object
        final Uid uid = con.create(ObjectClass.ACCOUNT, expected, null);
        AssertJUnit.assertNotNull(uid);
        final Long changelog = 10L;

        // update the last change
        PreparedStatement ps = null;
        DatabaseTableConnection conn = DatabaseTableConnection.createDBTableConnection(cfg);
        try {
            List<SQLParam> values = new ArrayList<SQLParam>();
            values.add(new SQLParam("changelog", changelog, Types.INTEGER));
            values.add(new SQLParam("accountId", uid.getUidValue(), Types.OTHER, "UUID"));
            ps = conn.prepareStatement(SQL_TEMPLATE, values);
            ps.execute();
            conn.commit();
        } finally {
            SQLUtil.closeQuietly(ps);
        }

        System.out.println("Uid: " + uid);
        FindUidSyncHandler ok = new FindUidSyncHandler(uid);
        // attempt to find the newly created object..
        con.sync(ObjectClass.ACCOUNT, new SyncToken(changelog - 1), ok, null);
        AssertJUnit.assertTrue(ERR1, ok.found);
        // Test the created attributes are equal the searched
        AssertJUnit.assertNotNull(ok.attributes);
        attributeSetsEquals(con.schema(), expected, ok.attributes);

        //Not in the next result
        FindUidSyncHandler empt = new FindUidSyncHandler(uid);
        // attempt to find the newly created object..
        con.sync(ObjectClass.ACCOUNT, ok.token, empt, null);
        AssertJUnit.assertFalse(ERR1, empt.found);
    }

    @Override
    @Test
    public void testSyncUsingIntegerColumn() throws Exception {
        final String ERR1 = "Could not find new object.";
        final String SQL_TEMPLATE = "UPDATE Accounts SET age = ? WHERE accountId = ?";
        final DatabaseTableConfiguration cfg = getConfiguration();
        cfg.setChangeLogColumn(AGE);
        con = getConnector(cfg);
        final Set<Attribute> expected = getCreateAttributeSet(cfg);
        final Uid uid = con.create(ObjectClass.ACCOUNT, expected, null);

        // update the last change
        PreparedStatement ps = null;
        DatabaseTableConnection conn = DatabaseTableConnection.createDBTableConnection(cfg);
        Integer changed = new Long(System.currentTimeMillis()).intValue();
        try {
            List<SQLParam> values = new ArrayList<SQLParam>();
            values.add(new SQLParam("age", changed, Types.INTEGER));
            values.add(new SQLParam("accountId", uid.getUidValue(), Types.OTHER, "UUID"));
            ps = conn.prepareStatement(SQL_TEMPLATE, values);
            ps.execute();
            conn.commit();
        } finally {
            SQLUtil.closeQuietly(ps);
        }

        System.out.println("Uid: " + uid);
        FindUidSyncHandler ok = new FindUidSyncHandler(uid);
        // attempt to find the newly created object..
        con.sync(ObjectClass.ACCOUNT, new SyncToken(changed - 1000), ok, null);
        AssertJUnit.assertTrue(ERR1, ok.found);
        // Test the created attributes are equal the searched
        AssertJUnit.assertNotNull(ok.attributes);
        attributeSetsEquals(con.schema(), expected, ok.attributes, AGE);

        System.out.println("Uid: " + uid);
        FindUidSyncHandler empt = new FindUidSyncHandler(uid);
        // attempt to find the newly created object..
        con.sync(ObjectClass.ACCOUNT, ok.token, empt, null);
        AssertJUnit.assertFalse(ERR1, empt.found);
    }

    @Override
    @Test
    public void testSyncUsingLongColumn() throws Exception {
        final String ERR1 = "Could not find new object.";
        final String SQL_TEMPLATE = "UPDATE Accounts SET accessed = ? WHERE accountId = ?";

        final DatabaseTableConfiguration cfg = getConfiguration();
        cfg.setChangeLogColumn(ACCESSED);
        con = getConnector(cfg);
        final Set<Attribute> expected = getCreateAttributeSet(cfg);
        final Uid uid = con.create(ObjectClass.ACCOUNT, expected, null);

        // update the last change
        PreparedStatement ps = null;
        DatabaseTableConnection conn = DatabaseTableConnection.createDBTableConnection(cfg);
        Integer changed = new Long(System.currentTimeMillis()).intValue();
        try {
            List<SQLParam> values = new ArrayList<SQLParam>();
            values.add(new SQLParam("accessed", changed, Types.INTEGER));
            values.add(new SQLParam("accountId", uid.getUidValue(), Types.OTHER, "UUID"));
            ps = conn.prepareStatement(SQL_TEMPLATE, values);
            ps.execute();
            conn.commit();
        } finally {
            SQLUtil.closeQuietly(ps);
        }
        System.out.println("Uid: " + uid);
        FindUidSyncHandler ok = new FindUidSyncHandler(uid);
        // attempt to find the newly created object..
        con.sync(ObjectClass.ACCOUNT, new SyncToken(changed - 1000), ok, null);
        AssertJUnit.assertTrue(ERR1, ok.found);
        // Test the created attributes are equal the searched
        AssertJUnit.assertNotNull(ok.attributes);
        attributeSetsEquals(con.schema(), expected, ok.attributes, ACCESSED);

        System.out.println("Uid: " + uid);
        FindUidSyncHandler empt = new FindUidSyncHandler(uid);
        // attempt to find the newly created object..
        con.sync(ObjectClass.ACCOUNT, ok.token, empt, null);
        AssertJUnit.assertFalse(ERR1, empt.found);
    }


    @AfterMethod
    public void tryToCleanUpAfterMethod() {

        try {
            if (con != null) {
                con.delete(ObjectClass.ACCOUNT, new Uid(testUID), null);
            }
        } catch (Exception e) {
            // No reaction
        }
    }
}
