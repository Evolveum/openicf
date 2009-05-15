/**
 * 
 */
package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleUserAttributeCS.PROFILE;
import static org.identityconnectors.oracle.OracleUserAttributeCS.ROLE;
import static org.identityconnectors.oracle.OracleConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
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
	
	private static final OperationOptions allAttributes;
	static {
		Collection<String> atg = new HashSet<String>(OracleOperationSearch.VALID_ATTRIBUTES_TO_GET);
		atg.remove(OperationalAttributes.PASSWORD_NAME);
		allAttributes = new OperationOptionsBuilder().setAttributesToGet(atg).build();
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
				attributes.add(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"PROFILE" + i));
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
		private Set<String> attributesToGet = new HashSet<String>(OracleOperationSearch.VALID_ATTRIBUTES_TO_GET);
		
		private UIDMatcher(String ...uid){
			this.uids = new ArrayList<String>(Arrays.asList(uid));
		}
		
		private UIDMatcher(Collection<String> uids){
			this.uids = new ArrayList<String>(uids);
		}

		
		private UIDMatcher(Collection<String> attributesToGet,Collection<String> uids){
			this.uids = new ArrayList<String>(uids);
			if(attributesToGet != null && !attributesToGet.isEmpty()){
				this.attributesToGet = new HashSet<String>(attributesToGet);
				this.attributesToGet.add(Uid.NAME);
			}
		}
		

		@SuppressWarnings("unchecked")
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
					if(object.getAttributeByName(aName) == null){
						Assert.fail("Attribute : [" + aName + "] is missing");
					}
				}
			}
			//It is also error if any extra attribute is present
			for(ConnectorObject object : objects){
				for(Attribute attr : object.getAttributes()){
					boolean isInGet = false;
					for(String aGet : attributesToGet){
						if(attr.is(aGet)){
							isInGet = true;
							break;
						}
					}
					if(!isInGet){
						Assert.fail("Attribute : [" + attr.getName() + "] is not in attributesToGet ");
					}
				}
			}
			return true;
		}

		public void describeTo(Description arg0) {
			arg0.appendText("Not all uids found");
			arg0.appendValue(uids);
		}
	}


	/** Search by uid */
	@Test
	public void testSearchByUID() {
		Assert.assertThat(TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, new EqualsFilter(new Uid(user(1))),allAttributes), new UIDMatcher(user(1)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new EqualsFilter(new Uid(user(3))),allAttributes ), new UIDMatcher(user(3)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new OrFilter(new EqualsFilter(new Uid(user(5))),new EqualsFilter(new Uid(user(6)))),allAttributes), new UIDMatcher(user(5),user(6)));
	}
	
	
	/**
	 * Test Search by name
	 */
	@Test
	public void testSearchByName() {
		Assert.assertThat(TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, new EqualsFilter(new Name(user(1))),allAttributes), new UIDMatcher(user(1)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new EqualsFilter(new Name(user(3))),allAttributes ), new UIDMatcher(user(3)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new OrFilter(new EqualsFilter(new Name(user(5))),new EqualsFilter(new Name(user(6)))),allAttributes), new UIDMatcher(user(5),user(6)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new EndsWithFilter(new Name(user(2))),allAttributes), new UIDMatcher(user(2)));
	}
	
	@Test
	public void testSearchByProfile(){
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"PROFILE3")),allAttributes), new UIDMatcher(user(3)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new OrFilter(new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"PROFILE5")),new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"PROFILE7"))),allAttributes), new UIDMatcher(user(5),user(7)));
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
				UserRecord record = userReader.readUserRecord(o.getUid().getUidValue());
				assertEquals("Found tablespace does not match",defTS, record.getDefaultTableSpace());
			}
		}
	}
	
	
	@Test
	public void testSearchByQuota() throws SQLException{
		facade.update(ObjectClass.ACCOUNT, new Uid(user(5)), Collections.singleton(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"20k")), null);
		facade.update(ObjectClass.ACCOUNT, new Uid(user(6)), Collections.singleton(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"60k")), null);
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new LessThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"10000"))),allAttributes  ), new UIDMatcher());
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new GreaterThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"10000"))),allAttributes ), new UIDMatcher(user(5),user(6)));
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new GreaterThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"40000"))),allAttributes ), new UIDMatcher(user(6)));
	}
	
	@Test
	public void testSearchByRoles() throws SQLException {
		String[] roles = new String[]{"role1","role2"}; 
		dropRoles(roles);
		createRoles(roles);
        Attribute aRoles = AttributeBuilder.build(ORACLE_ROLES_ATTR_NAME, Arrays.asList(roles));
        facade.update(ObjectClass.ACCOUNT, new Uid(user(3)),Collections.singleton(aRoles),null);
        facade.update(ObjectClass.ACCOUNT, new Uid(user(7)),Collections.singleton(aRoles),null);
        //We must search using facade, currently we do not support in operator
        Assert.assertThat(TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new ContainsAllValuesFilter(aRoles)),allAttributes),new UIDMatcher(user(3),user(7)));
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
					ORACLE_PRIVS_ATTR_NAME, "CREATE SESSION",
					"SELECT ON " + testConf.getUser() + ".MYTABLE1",
					"SELECT ON " + testConf.getUser() + ".MYTABLE2");
	        facade.update(ObjectClass.ACCOUNT, new Uid(user(3)),Collections.singleton(privileges),null);
	        facade.update(ObjectClass.ACCOUNT, new Uid(user(7)),Collections.singleton(privileges),null);
	        //Must search using facade, we do not support in operator
	        Assert.assertThat(TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new ContainsAllValuesFilter(privileges)),allAttributes), new UIDMatcher(user(3),user(7)));
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
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildEnabled(true))),allAttributes), new UIDMatcher(ALL_UIDS.toArray(new String[ALL_UIDS.size()])));
		//None is disabled
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildEnabled(false))),allAttributes), new UIDMatcher());
		
		//update two of them - disable them
		facade.update(ObjectClass.ACCOUNT, new Uid(user(2)), Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
		facade.update(ObjectClass.ACCOUNT, new Uid(user(6)), Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
		List<String> newUids = new ArrayList<String>(ALL_UIDS);
		newUids.removeAll(Arrays.asList(user(2),user(6)));
		//test others are still enabled
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildEnabled(true))),allAttributes), new UIDMatcher(newUids.toArray(new String[newUids.size()])));
		//and that just two are disabled
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildEnabled(false))),allAttributes), new UIDMatcher(user(2),user(6)));
	}
	
	@Test
	public void testSearchByPasswordExpire(){
		//Unexpire password
		for(String uid : ALL_UIDS){
			facade.update(ObjectClass.ACCOUNT, new Uid(uid), CollectionUtil.newSet(AttributeBuilder.buildPasswordExpired(false),AttributeBuilder.buildPassword(uid.toCharArray())), null);
		}
		//All must be unexpired
		Assert.assertThat(TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildPasswordExpired(false))),allAttributes), new UIDMatcher(ALL_UIDS.toArray(new String[ALL_UIDS.size()])));
		//None is expired
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildPasswordExpired(true))),allAttributes), new UIDMatcher());
		
		//expire two
		facade.update(ObjectClass.ACCOUNT, new Uid(user(1)), Collections.singleton(AttributeBuilder.buildPasswordExpired(true)), null);
		facade.update(ObjectClass.ACCOUNT, new Uid(user(7)), Collections.singleton(AttributeBuilder.buildPasswordExpired(true)), null);
		List<String> newUids = new ArrayList<String>(ALL_UIDS);
		newUids.removeAll(Arrays.asList(user(1),user(7)));
		//others must remain unexpired
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildPasswordExpired(false))),allAttributes), new UIDMatcher(newUids.toArray(new String[newUids.size()])));
		//just two are expired
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.buildPasswordExpired(true))),allAttributes), new UIDMatcher(user(1),user(7)));
	}
	
	
	@Test
	public void testSearchByDisabledDate(){
		//Set all to enabled
		for(String uid : ALL_UIDS){
			facade.update(ObjectClass.ACCOUNT, new Uid(uid), Collections.singleton(AttributeBuilder.buildEnabled(true)), null);
		}
		//Now all disabled date must be null
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new EqualsFilter(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME))),allAttributes), new UIDMatcher(ALL_UIDS.toArray(new String[ALL_UIDS.size()])));
		//update two of them, so set enabled to false
		facade.update(ObjectClass.ACCOUNT, new Uid(user(2)), Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
		facade.update(ObjectClass.ACCOUNT, new Uid(user(6)), Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
		//They should have set lock date to now, but search to not null
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name(USER_PREFIX)),new NotFilter(new EqualsFilter(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME)))),allAttributes), new UIDMatcher(user(2),user(6)));
		//Enabled one
		facade.update(ObjectClass.ACCOUNT, new Uid(user(6)), Collections.singleton(AttributeBuilder.buildEnabled(true)), null);
		//Search for it
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new EqualsFilter(new Name(user(6))),new EqualsFilter(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME))),allAttributes), new UIDMatcher(user(6)));
	}
	
	@Test
	public void testSearchByExpirationDate() throws SQLException{
		for(String uid : ALL_UIDS){
			//unexpire to have null or some date in future
			facade.update(ObjectClass.ACCOUNT, new Uid(uid), CollectionUtil.newSet(AttributeBuilder.buildPassword(uid.toCharArray()),AttributeBuilder.buildPasswordExpired(false)), null);
			Timestamp expiredDate = userReader.readUserRecord(uid).getExpireDate();
			Attribute expiredDateAttr = OracleConnectorHelper.buildSingleAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, expiredDate != null ? expiredDate.getTime() : null);
			Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new EqualsFilter(new Name(uid)),new EqualsFilter(expiredDateAttr)),allAttributes), new UIDMatcher(uid));
		}
		//expire two of them
		facade.update(ObjectClass.ACCOUNT, new Uid(user(4)), Collections.singleton(AttributeBuilder.buildPasswordExpired(true)), null);
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new EqualsFilter(new Name(user(4))),new NotFilter(new EqualsFilter(AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME)))),allAttributes), new UIDMatcher(user(4)));
		Timestamp expiredDate = userReader.readUserRecord(user(4)).getExpireDate();
		Attribute expiredDateAttr = OracleConnectorHelper.buildSingleAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, expiredDate != null ? expiredDate.getTime() : null);
		Assert.assertThat(TestHelpers.searchToList(connector,ObjectClass.ACCOUNT, new AndFilter(new EqualsFilter(new Name(user(4))),new EqualsFilter(expiredDateAttr)),allAttributes), new UIDMatcher(user(4)));
	}
	
	/** Test search by authentication type */
	@Test
	public void testSearchByAuthentication(){
		//All created user have local authentication
		Assert.assertThat(TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, FilterBuilder.and(FilterBuilder.startsWith(new Name(USER_PREFIX)), FilterBuilder.equalTo(AttributeBuilder.build(ORACLE_AUTHENTICATION_ATTR_NAME,ORACLE_AUTH_LOCAL)))),new UIDMatcher(ALL_UIDS));
		//Update user3 and user4 to external
		connector.update(ObjectClass.ACCOUNT, new Uid(user(3)), CollectionUtil.newSet(AttributeBuilder.build(ORACLE_AUTHENTICATION_ATTR_NAME,ORACLE_AUTH_EXTERNAL)), allAttributes);
		connector.update(ObjectClass.ACCOUNT, new Uid(user(4)), CollectionUtil.newSet(AttributeBuilder.build(ORACLE_AUTHENTICATION_ATTR_NAME,ORACLE_AUTH_EXTERNAL)), allAttributes);
		Assert.assertThat(TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, FilterBuilder.and(FilterBuilder.startsWith(new Name(USER_PREFIX)), FilterBuilder.equalTo(AttributeBuilder.build(ORACLE_AUTHENTICATION_ATTR_NAME,ORACLE_AUTH_EXTERNAL)))),new UIDMatcher(user(3),user(4)));
	}
	
	
	/** Test that search throws IllegalArgumentException at invalid attribute in filter or attributesToGet */
	@Test
	public void testValidAttributes(){
		//Build filter by all attributes
		Filter f = createFullFilter(false);
		TestHelpers.searchToList(connector,ObjectClass.ACCOUNT,f);
		//search by dummy
		try{
			TestHelpers.searchToList(connector,ObjectClass.ACCOUNT,new EqualsFilter(AttributeBuilder.build("dummy")));
			Assert.fail("Should not search by dummy attribute");
		}
		catch(IllegalArgumentException e){}
		//search by password must fail
		try{
			TestHelpers.searchToList(connector,ObjectClass.ACCOUNT,new EqualsFilter(AttributeBuilder.buildPassword("dummy".toCharArray())));
			Assert.fail("Should not search by password attribute");
		}
		catch(IllegalArgumentException e){}
		
		//Test valid attributes to get
		//Set no attributesToGet
		OperationOptions options = new OperationOptionsBuilder().setAttributesToGet().build();
		Assert.assertThat(TestHelpers.searchToList(connector,
				ObjectClass.ACCOUNT, FilterBuilder.equalTo(new Uid(user(1))),
				options), new UIDMatcher(Arrays.asList(options
				.getAttributesToGet()), Arrays.asList(user(1))));
		//Set just name
		options = new OperationOptionsBuilder().setAttributesToGet(Name.NAME).build();
		Assert.assertThat(TestHelpers.searchToList(connector,
				ObjectClass.ACCOUNT, FilterBuilder.equalTo(new Uid(user(1))),
				options), new UIDMatcher(Arrays.asList(options
				.getAttributesToGet()), Arrays.asList(user(1))));
		//Set some dummy attribute
		options = new OperationOptionsBuilder().setAttributesToGet("dummy1","dummy2").build();
		try{
			TestHelpers.searchToList(connector, ObjectClass.ACCOUNT, FilterBuilder.equalTo(new Uid(user(1))),options);
			Assert.fail("Must fail for invalid attributesToGet");
		}
		catch(RuntimeException e){}
		
		//try other case of attributes
		f = createFullFilter(true);
		TestHelpers.searchToList(connector,ObjectClass.ACCOUNT,f);
		Collection<String> myAttrToGet = new ArrayList<String>();
		for(String s : OracleOperationSearch.VALID_ATTRIBUTES_TO_GET){
			myAttrToGet.add(s.toUpperCase());
		}
		//set attributesToGet uppercase
		TestHelpers.searchToList(connector,ObjectClass.ACCOUNT,f,new OperationOptionsBuilder().setAttributesToGet(myAttrToGet).build());
	}

	private Filter createFullFilter(boolean toUpper) {
		Filter f = new EqualsFilter(new Uid("myuid"));
		f = new AndFilter(f,new EqualsFilter(new Name("myname")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_AUTHENTICATION_ATTR_NAME,toUpper),ORACLE_AUTH_LOCAL)));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_PROFILE_ATTR_NAME,toUpper),"myprofile")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_DEF_TS_ATTR_NAME,toUpper),"myts")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_TEMP_TS_ATTR_NAME,toUpper),"mytempts")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_DEF_TS_QUOTA_ATTR_NAME,toUpper),"32K")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_TEMP_TS_QUOTA_ATTR_NAME,toUpper),"64K")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_GLOBAL_ATTR_NAME,toUpper),"myglobal")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_PRIVS_ATTR_NAME,toUpper),"MY_PRIV1","MY_PRIV2")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_ROLES_ATTR_NAME,toUpper),"MY_ROLE1","MY_ROLE2")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.build(gan(ORACLE_ROLES_ATTR_NAME,toUpper),"MY_ROLE1","MY_ROLE2")));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.buildPasswordExpired(false)));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.buildPasswordExpirationDate(0)));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.buildEnabled(true)));
		f = new AndFilter(f,new EqualsFilter(AttributeBuilder.buildDisableDate(0)));
		return f;
	}
	
	private String gan(String name,boolean toUpper){
		return toUpper ? name.toUpperCase() : name;
	}
	
	/** Test that search will fail for killed connection */
	@Test
	public void testSearchfail() throws SQLException{
		OracleConnector testConnector = createTestConnector();
		Filter f = createFullFilter(false);
		TestHelpers.searchToList(testConnector,ObjectClass.ACCOUNT,f);
		OracleSpecificsTest.killConnection(connector.getAdminConnection(), testConnector.getAdminConnection());
		try{
			TestHelpers.searchToList(testConnector,ObjectClass.ACCOUNT,f);
			Assert.fail("Search must fail for killed connection");
		}catch(RuntimeException e){}
		testConnector.dispose();
	}
}
