/**
 * 
 */
package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.*;
import static org.identityconnectors.oracle.OracleUserAttribute.*;
import static org.junit.Assert.*;



import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

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
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author kitko
 *
 */
public class OracleOperationSearchTest extends OracleConnectorAbstractTest{

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
		for(int i = 1;i <= 10;i++){
			Set<Attribute> attributes = new HashSet<Attribute>();
			if(i == 3 | i == 5 || i == 7){
				attributes.add(AttributeBuilder.build(OracleConnector.ORACLE_PROFILE_ATTR_NAME,"profile" + i));
			}
			attributes.add(new Name("test" + i));
			
			facade.create(ObjectClass.ACCOUNT, attributes, null);
		}
	}
	
	private static void createProfiles() {
		dropProfiles();
		for(String profile : new String[]{"profile3","profile5","profile7"}){
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
		for(int i = 1;i <= 10;i++){
			Uid uid = new Uid("test" + i);
			try{
				facade.delete(ObjectClass.ACCOUNT, uid, null);
			}
			catch(UnknownUidException e){
			}
		}
	}
	
	private static class UIDMatcher extends BaseMatcher<Iterable<ConnectorObject>>{
		private List<String> uids;
		
		private UIDMatcher(String ...uid){
			this.uids = new ArrayList<String>(Arrays.asList(uid));
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
				for(String aName : OracleConnector.ALL_ATTRIBUTE_NAMES){
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
	
	private static class Listhandler implements ResultsHandler{
		private final List<ConnectorObject> results = new ArrayList<ConnectorObject>();
		@Override
		public boolean handle(ConnectorObject obj) {
			results.add(obj);
			return true;
		}
		List<ConnectorObject> getResultsAndClear(){
			List<ConnectorObject> results = new ArrayList<ConnectorObject>(this.results);
			this.results.clear();
			return results;
		}
		
	}

	/**
	 * Test Search by name
	 */
	@Test
	public void testSearchByName() {
		Listhandler handler = new Listhandler();
		facade.search(ObjectClass.ACCOUNT, new EqualsFilter(new Name("TEST1")), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST1"));
		facade.search(ObjectClass.ACCOUNT, new EqualsFilter(new Name("TEST3")), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST3"));
		facade.search(ObjectClass.ACCOUNT, new OrFilter(new EqualsFilter(new Name("TEST5")),new EqualsFilter(new Name("TEST6"))), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST5","TEST6"));
		facade.search(ObjectClass.ACCOUNT, new EndsWithFilter(new Name("2")), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST2"));
	}
	
	@Test
	public void testSearchByProfile(){
		Listhandler handler = new Listhandler();
		facade.search(ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"profile3")), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST3"));
		facade.search(ObjectClass.ACCOUNT, new OrFilter(new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"profile5")),new EqualsFilter(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,"profile7"))), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST5","TEST7"));
	}
	
	@Test
	public void testSearchByTableSpace() throws SQLException{
		Listhandler handler = new Listhandler();
		List<String> allDefTS = findAllDefTS(connector.getAdminConnection());
		int i = 1;
		for(String defTS : allDefTS){
			String userName = "TEST" + i;
			try{
				facade.update(ObjectClass.ACCOUNT, new Uid(userName), Collections.singleton(AttributeBuilder.build(ORACLE_DEF_TS_ATTR_NAME,defTS)), null);
			}
			catch(ConnectorException e){
				continue;
			}
			finally{
				i++;
			}
			facade.search(ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(ORACLE_DEF_TS_ATTR_NAME,defTS)), handler, null);
			for(ConnectorObject o : handler.getResultsAndClear()){
				OracleUserReader reader = new OracleUserReader(connector.getAdminConnection());
				UserRecord record = reader.readUserRecord(o.getUid().getUidValue());
				assertEquals("Found tablespace does not match",defTS, record.defaultTableSpace);
			}
		}
	}
	
	
	@Test
	public void testSearchByQuota() throws SQLException{
		Listhandler handler = new Listhandler();
		facade.update(ObjectClass.ACCOUNT, new Uid("TEST5"), Collections.singleton(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"20k")), null);
		facade.update(ObjectClass.ACCOUNT, new Uid("TEST6"), Collections.singleton(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"60k")), null);
		facade.search(ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name("TEST")),new LessThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"10000"))), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher());
		facade.search(ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name("TEST")),new GreaterThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"10000"))), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST5","TEST6"));
		facade.search(ObjectClass.ACCOUNT, new AndFilter(new StartsWithFilter(new Name("TEST")),new GreaterThanFilter(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,"40000"))), handler, null);
		Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST6"));
	}
	
	@Test
	public void testSearchByRoles() throws SQLException {
		Listhandler handler = new Listhandler();
		String[] roles = new String[]{"role1","role2"}; 
		dropRoles(roles);
		createRoles(roles);
        Attribute aRoles = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME, Arrays.asList(roles));
        facade.update(ObjectClass.ACCOUNT, new Uid("TEST3"),Collections.singleton(aRoles),null);
        facade.update(ObjectClass.ACCOUNT, new Uid("TEST7"),Collections.singleton(aRoles),null);
        facade.search(ObjectClass.ACCOUNT,new AndFilter(new StartsWithFilter(new Name("TEST")),new ContainsAllValuesFilter(aRoles)),handler,null);
        Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST3","TEST7"));
    }
	
	private void createRoles(String ...roles) throws SQLException{
		final OracleCaseSensitivitySetup cs = testConf.getCSSetup();
		for(String role : roles){
			SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "create role " + cs.normalizeAndFormatToken(OracleUserAttribute.ROLE,role));
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
		Listhandler handler = new Listhandler();
		dropPrivilegeTables();
		createPrivilegeTables();
        try{
			Attribute privileges = AttributeBuilder.build(
					OracleConnector.ORACLE_PRIVS_ATTR_NAME, "CREATE SESSION",
					"SELECT ON " + testConf.getUser() + ".MYTABLE1",
					"SELECT ON " + testConf.getUser() + ".MYTABLE2");
	        facade.update(ObjectClass.ACCOUNT, new Uid("TEST3"),Collections.singleton(privileges),null);
	        facade.update(ObjectClass.ACCOUNT, new Uid("TEST7"),Collections.singleton(privileges),null);
	        facade.search(ObjectClass.ACCOUNT,new AndFilter(new StartsWithFilter(new Name("TEST")),new ContainsAllValuesFilter(privileges)),handler,null);
	        Assert.assertThat(handler.getResultsAndClear(), new UIDMatcher("TEST3","TEST7"));
        }
        finally{
        	dropPrivilegeTables();
        }
    }
	

}
