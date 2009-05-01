/**
 * 
 */
package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.ORACLE_DEF_TS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_PROFILE_ATTR_NAME;
import static org.identityconnectors.oracle.OracleUserAttributeCS.PROFILE;
import static org.identityconnectors.oracle.OracleUserAttributeCS.ROLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.Pair;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author kitko
 * We will use SPI for search to really test just SPI when not needed to use API
 *
 */
public class OracleOperationSearchTest extends OracleConnectorAbstractTest{
	private static final String USER_PREFIX = "TEST_SEARCH";
	
	private static String user(int i){
		return USER_PREFIX + i;
	}
	
	private static final List<String> ALL_UIDS;
	
	static {
		List<String> tmp = new ArrayList<String>();
		for(int i = 1;i <= 10;i++){
			tmp.add(user(i));
		}
		ALL_UIDS = Collections.unmodifiableList(tmp);
	}
	
	/**
	 * Test method for {@link org.identityconnectors.oracle.OracleOperationSearch#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)}.
	 */
	@Test
	public void testCreateFilterTranslator() {
		FilterTranslator<Pair<String, FilterWhereBuilder>> translator = new OracleOperationSearch(testConf,connector.getAdminConnection(),OracleConnector.getLog()).createFilterTranslator(ObjectClass.ACCOUNT, null);
		assertNotNull(translator);
		List<Pair<String, FilterWhereBuilder>> translate = translator.translate(new EqualsFilter(new Name("test")));
		assertNotNull(translate);
	}
	
	@BeforeClass
	public static void create(){
		createProfiles();
		createTestUsers();
	}
	
	@AfterClass
	public static void delete(){
		dropTestUsers();
		dropProfiles();
	}
	
	
	
	private static void createTestUsers(){
		dropTestUsers();
		int i = 1;
		for(String uid : ALL_UIDS){
			Set<Attribute> attributes = new HashSet<Attribute>();
			if(i == 3 | i == 5 || i == 7){
				attributes.add(AttributeBuilder.build(OracleConnector.ORACLE_PROFILE_ATTR_NAME,"PROFILE" + i));
			}
			attributes.add(new Name(uid));
			facade.create(ObjectClass.ACCOUNT, attributes, null);
			i++;
		}
	}
	
	private static void createProfiles() {
		dropProfiles();
		for(String profile : new String[]{"PROFILE3","PROFILE5","PROFILE7"}){
	        try{
	            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "create profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profile) + "limit password_lock_time 6");
	            connector.getAdminConnection().commit();
	        }
	        catch(SQLException e){
	        }
		}
	}

	private static void dropProfiles() {
		for(String profile : new String[]{"profile3","profile5","profile7"}){
	        try{
	            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profile));
	        }
	        catch(SQLException e){
	        }
		}
	}

	private static void dropTestUsers(){
		for(String uid : ALL_UIDS){
			try{
				facade.delete(ObjectClass.ACCOUNT, new Uid(uid), null);
			}
			catch(UnknownUidException e){
			}
		}
	}
	
	private static class UIDMatcher extends BaseMatcher<Iterable<ConnectorObject>>{
		private List<String> uids;
		private Set<String> attributesToGet = new HashSet<String>(OracleConnector.ALL_ATTRIBUTE_NAMES);
		
		private UIDMatcher(String ...uid){
			this.uids = new ArrayList<String>(Arrays.asList(uid));
		}
		
		private UIDMatcher(Set<String> attributesToGet,String ...uid){
			this.uids = new ArrayList<String>(Arrays.asList(uid));
			this.attributesToGet = new HashSet<String>(attributesToGet);
		}

		
		private UIDMatcher(List<String> uids) {
			this.uids = new ArrayList<String>(uids);
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean matches(Object arg0) {
			if(!(arg0 instanceof List)){
				return false;
			}
			List<ConnectorObject> objects = (List<ConnectorObject>) arg0;
			if(uids.size() != objects.size()){
				return false;
			}
			for(String uid : uids){
				boolean found = false;
				for(ConnectorObject object : objects){
					if(uid.equals(object.getUid().getUidValue())){
						found = true;
						break;
					}
				}
				if(!found){
					return false;
				}
			}
			
			//Look at ConnectorObject if all attributes are present
			for(ConnectorObject object : objects){
				for(String aName : attributesToGet){
					if(OperationalAttributes.PASSWORD_NAME.equals(aName)){
						continue;
					}
					if(object.getAttributeByName(aName) == null){
						Assert.fail("Attribute : [" + aName + "] is missing");
					}
				}
			}
			return true;
		}

		@Override
		public void describeTo(Description arg0) {
			arg0.appendText("Not all uids found");
			arg0.appendValue(uids);
		}
	}


	/**
	 * Test Search by name
	 */
	@Test
	public void testSearchByName() {
		Assert.assertThat(TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, new EqualsFilter(new Name(user(1)))), new UIDMatcher(user(1)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new EqualsFilter(new Name(user(3))) ), new UIDMatcher(user(3)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new OrFilter(new EqualsFilter(new Name(user(5))),new EqualsFilter(new Name(user(6)))) ), new UIDMatcher(user(5),user(6)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new EndsWithFilter(new Name(user(2)))  ), new UIDMatcher(user(2)));
	}
	
	@Test
	public void testSearchByProfile(){
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"PROFILE3"))  ), new UIDMatcher(user(3)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new OrFilter(new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"PROFILE5")),new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"PROFILE7")))  ), new UIDMatcher(user(5),user(7)));
	}
	
	@Test
	public void testSearchByTableSpace() throws SQLException{
		List<String> allDefTS = findAllDefTS(connector.getAdminConnection());
		int i = 1;
		for(String defTS : allDefTS){
			try{
				facade.update(ObjectClass.ACCOUNT, new Uid(user(i)), Collections.singleton(AttributeBuilder.build(ORACLE_DEF_TS_ATTR_NAME,defTS)), null);
			}
			catch(ConnectorException e){
				continue;
			}
			finally{
				i++;
			}
			for(ConnectorObject o : TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(ORACLE_DEF_TS_ATTR_NAME,defTS)))){
				OracleUserReader reader = new OracleUserReader(connector.getAdminConnection());
				UserRecord record = reader.readUserRecord(o.getUid().getUidValue());
				assertEquals("Found tablespace does not match",defTS, record.defaultTableSpace);
			}
		}
	}
	
	
	@Test
	public void testSearchByQuota() throws SQLException{
		facade.update(ObjectClass.ACCOUNT, new Uid(user(5)), Collections.singleton(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"20k")), null);
		facade.update(ObjectClass.ACCOUNT, new Uid(user(6)), Collections.singleton(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"60k")), null);
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new LessThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"10000")))  ), new UIDMatcher());
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new GreaterThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"10000"))) ), new UIDMatcher(user(5),user(6)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new GreaterThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"40000"))) ), new UIDMatcher(user(6)));
	}
	
	@Test
	public void testSearchByRoles() throws SQLException {
		String[] roles = new String[]{"role1","role2"}; 
		dropRoles(roles);
		createRoles(roles);
        Attribute aRoles = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME, Arrays.asList(roles));
        facade.update(ObjectClass.ACCOUNT, new Uid(user(3)),Collections.singleton(aRoles),null);
        facade.update(ObjectClass.ACCOUNT, new Uid(user(7)),Collections.singleton(aRoles),null);
        //We must search using facade, currently we do not support in operator
        Assert.assertThat(TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new ContainsAllValuesFilter(aRoles)),null),new UIDMatcher(user(3),user(7)));
    }
	
	private void createRoles(String ...roles) throws SQLException{
		final OracleCaseSensitivitySetup cs = testConf.getCSSetup();
		for(String role : roles){
			SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "create role " + cs.normalizeAndFormatToken(OracleUserAttributeCS.ROLE,role));
		}
	}
	
	private void dropRoles(String ...roles){
		final OracleCaseSensitivitySetup cs = testConf.getCSSetup();
		for(String role : roles){
	        try{
	            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop role " + cs.normalizeAndFormatToken(ROLE, role));
	        }catch(SQLException e){}
		}
	}
	
	private void dropPrivilegeTables(){
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE1");
        }
        catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE2");
        }
        catch(SQLException e){}
	}
	
	private void createPrivilegeTables() throws SQLException{
		SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table MYTABLE1(id number)");
		SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table MYTABLE2(id number)");
	}
	
	
	@Test
	public void testSearchByPrivileges() throws SQLException {
		dropPrivilegeTables();
		createPrivilegeTables();
        try{
			Attribute privileges = AttributeBuilder.build(
					OracleConnector.ORACLE_PRIVS_ATTR_NAME, "CREATE SESSION",
					"SELECT ON " + testConf.getUser() + ".MYTABLE1",
					"SELECT ON " + testConf.getUser() + ".MYTABLE2");
	        facade.update(ObjectClass.ACCOUNT, new Uid(user(3)),Collections.singleton(privileges),null);
	        facade.update(ObjectClass.ACCOUNT, new Uid(user(7)),Collections.singleton(privileges),null);
	        //Must search using facade, we do not support in operator
	        Assert.assertThat(TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new ContainsAllValuesFilter(privileges))), new UIDMatcher(user(3),user(7)));
        }
        finally{
        	dropPrivilegeTables();
        }
    }
	
	@Test
	public void testSearchByEnabled(){
		//enable all
		for(String uid : ALL_UIDS){
			facade.update(ObjectClass.ACCOUNT, new Uid(uid), Collections.singleton(AttributeBuilder.buildEnabled(true)), null);
		}
		//all must be enables
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildEnabled(true)))), new UIDMatcher(ALL_UIDS.toArray(new String[ALL_UIDS.size()])));
		//None is disabled
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildEnabled(false)))), new UIDMatcher());
		
		//update two of them - disable them
		facade.update(ObjectClass.ACCOUNT, new Uid(user(2)), Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
		facade.update(ObjectClass.ACCOUNT, new Uid(user(6)), Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
		List<String> newUids = new ArrayList<String>(ALL_UIDS);
		newUids.removeAll(Arrays.asList(user(2),user(6)));
		//test others are still enabled
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildEnabled(true)))), new UIDMatcher(newUids.toArray(new String[newUids.size()])));
		//and that just two are disabled
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildEnabled(false)))), new UIDMatcher(user(2),user(6)));
	}
	
	@Test
	public void testSearchByPasswordExpire(){
		//Unexpire password
		for(String uid : ALL_UIDS){
			facade.update(ObjectClass.ACCOUNT, new Uid(uid), CollectionUtil.newSet(AttributeBuilder.buildPasswordExpired(false),AttributeBuilder.buildPassword(uid.toCharArray())), null);
		}
		//All must be unexpired
		Assert.assertThat(TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildPasswordExpired(false)))), new UIDMatcher(ALL_UIDS.toArray(new String[ALL_UIDS.size()])));
		//None is expired
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildPasswordExpired(true)))), new UIDMatcher());
		
		//expire two
		facade.update(ObjectClass.ACCOUNT, new Uid(user(1)), Collections.singleton(AttributeBuilder.buildPasswordExpired(true)), null);
		facade.update(ObjectClass.ACCOUNT, new Uid(user(7)), Collections.singleton(AttributeBuilder.buildPasswordExpired(true)), null);
		List<String> newUids = new ArrayList<String>(ALL_UIDS);
		newUids.removeAll(Arrays.asList(user(1),user(7)));
		//others must remain unexpired
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildPasswordExpired(false)))), new UIDMatcher(newUids.toArray(new String[newUids.size()])));
		//just two are expired
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildPasswordExpired(true)))), new UIDMatcher(user(1),user(7)));
	}
	
	
	@Test
	public void testSearchByDisabledDate(){
		//Set all to enabled
		for(String uid : ALL_UIDS){
			facade.update(ObjectClass.ACCOUNT, new Uid(uid), Collections.singleton(AttributeBuilder.buildEnabled(true)), null);
		}
		//Now all disabled date must be null
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME)))), new UIDMatcher(ALL_UIDS.toArray(new String[ALL_UIDS.size()])));
		//update two of them, so set enabled to false
		facade.update(ObjectClass.ACCOUNT, new Uid(user(2)), Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
		facade.update(ObjectClass.ACCOUNT, new Uid(user(6)), Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
		//They should have set lock date to now, but search to not null
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new NotFilter(new EqualsFilter(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME))))), new UIDMatcher(user(2),user(6)));
		//Enabled one
		facade.update(ObjectClass.ACCOUNT, new Uid(user(6)), Collections.singleton(AttributeBuilder.buildEnabled(true)), null);
		//Search for it
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new EqualsFilter(new Name(user(6))),new EqualsFilter(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME)))), new UIDMatcher(user(6)));
	}
	
	@Test
	public void testSearchByExpirationDate() throws SQLException{
		for(String uid : ALL_UIDS){
			//unexpire to have null or some date in future
			facade.update(ObjectClass.ACCOUNT, new Uid(uid), CollectionUtil.newSet(AttributeBuilder.buildPassword(uid.toCharArray()),AttributeBuilder.buildPasswordExpired(false)), null);
			Timestamp expiredDate = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid).expireDate;
			Attribute expiredDateAttr = OracleConnectorHelper.buildSingleAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, expiredDate != null ? expiredDate.getTime() : null);
			Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new EqualsFilter(new Name(uid)),new EqualsFilter(expiredDateAttr))), new UIDMatcher(uid));
		}
		//expire two of them
		facade.update(ObjectClass.ACCOUNT, new Uid(user(4)), Collections.singleton(AttributeBuilder.buildPasswordExpired(true)), null);
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new EqualsFilter(new Name(user(4))),new NotFilter(new EqualsFilter(AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME))))), new UIDMatcher(user(4)));
		Timestamp expiredDate = new OracleUserReader(connector.getAdminConnection()).readUserRecord(user(4)).expireDate;
		Attribute expiredDateAttr = OracleConnectorHelper.buildSingleAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, expiredDate != null ? expiredDate.getTime() : null);
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new EqualsFilter(new Name(user(4))),new EqualsFilter(expiredDateAttr))), new UIDMatcher(user(4)));
	}


}
