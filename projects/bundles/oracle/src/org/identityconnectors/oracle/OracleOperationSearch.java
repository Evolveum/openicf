package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.MSG_ERROR_EXECUTING_SEARCH;
import static org.identityconnectors.oracle.OracleMessages.MSG_SEARCH_ATTRIBUTE_NOT_SUPPORTED_FOR_ATTRIBUTESTOGET;
import static org.identityconnectors.oracle.OracleMessages.MSG_SEARCH_ATTRIBUTE_NOT_SUPPORTED_FOR_SEARCHBY;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.identityconnectors.common.Pair;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.DatabaseFilterTranslator;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.operations.SearchOp;

/**
 * Oracle search actually executes query to search for users. It uses DBA_USERS,DBA_TS_QUOTAS,DBA_ROLE_PRIVS,DBA_SYS_PRIVS,USER_TAB_PRIVS views to perform query
 * @author kitko
 *
 */
final class OracleOperationSearch extends AbstractOracleOperation implements SearchOp<Pair<String, FilterWhereBuilder>>{
	private static final String SQL = "SELECT DISTINCT DBA_USERS.* FROM DBA_USERS";
	
	private static final String ADVANCED_SQL1 = SQL + 	" LEFT JOIN DBA_TS_QUOTAS DEF_QUOTA " +
    													"ON DBA_USERS.USERNAME = DEF_QUOTA.USERNAME AND DBA_USERS.DEFAULT_TABLESPACE=DEF_QUOTA.TABLESPACE_NAME " + 
    													"LEFT JOIN DBA_TS_QUOTAS TEMP_QUOTA " + 
    												"ON DBA_USERS.USERNAME = DEF_QUOTA.USERNAME AND DBA_USERS.TEMPORARY_TABLESPACE=TEMP_QUOTA.TABLESPACE_NAME";
	
	private static final String ADVANCED_SQL2 = ADVANCED_SQL1 + " LEFT JOIN DBA_ROLE_PRIVS " +
																"ON DBA_USERS.USERNAME=DBA_ROLE_PRIVS.GRANTEE " + 
																"LEFT JOIN DBA_SYS_PRIVS ON DBA_USERS.USERNAME=DBA_SYS_PRIVS.GRANTEE " + 
																"LEFT JOIN USER_TAB_PRIVS ON DBA_USERS.USERNAME=USER_TAB_PRIVS.GRANTEE";

	
	static final Collection<String> VALID_ATTRIBUTES_TO_GET;
	
	static final Collection<String> VALID_SEARCH_BY_ATTRIBUTES;
	
	static {
		Collection<String> tmp = new TreeSet<String>(OracleConnectorHelper.getAttributeNamesComparator());
		tmp.addAll(OracleConstants.ALL_ATTRIBUTE_NAMES);
		tmp.remove(OperationalAttributes.PASSWORD_NAME);
		tmp.add(Uid.NAME);
		VALID_ATTRIBUTES_TO_GET = Collections.unmodifiableCollection(tmp);
		VALID_SEARCH_BY_ATTRIBUTES = Collections.unmodifiableCollection(tmp);
	}
	
	
	
	OracleOperationSearch(OracleConfiguration cfg, Connection adminConn, Log log) {
		super(cfg, adminConn, log);
	}

	public FilterTranslator<Pair<String, FilterWhereBuilder>> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
		return new OracleFilterTranslator(oclass, options, cfg.getConnectorMessages(), cfg.getCSSetup());
	}

	public void executeQuery(ObjectClass oclass, Pair<String, FilterWhereBuilder> pair, ResultsHandler handler, OperationOptions options) {
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(pair.first);
        query.setWhere(pair.second);
        final String sql = query.getSQL();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
        	log.info("Executing search query : {0}", sql);
			st = this.adminConn.prepareStatement(sql);
            SQLUtil.setParams(st, query.getParams());
			rs = st.executeQuery();
			OracleUserReader userReader = new OracleUserReader(adminConn,cfg.getConnectorMessages());
            Collection<String> attributesToGet = null;
            checkAttributesToGet(options.getAttributesToGet());
            if(options.getAttributesToGet() != null && options.getAttributesToGet().length > 0){
            	attributesToGet = new HashSet<String>(Arrays.asList(options.getAttributesToGet()));
            }
            else{
            	//Now all attributes are read by default
            	//There is small performance problem with reading roles,privileges,quotas
            	attributesToGet = VALID_ATTRIBUTES_TO_GET;
            }
            boolean found = false;
			while(rs.next()){
				found = true;
                ConnectorObjectBuilder builder = buildConnectorObject(rs, userReader, attributesToGet);
                if (!handler.handle(builder.build())) {
                    break;
                }
			}
			rs.close();
			if(!found){
				//This is hack to search case insensitive by name
				handleCaseInsensitiveNames(pair,attributesToGet,handler,userReader);
			}
			adminConn.commit();
		}
        catch (Exception e) {
        	throw new ConnectorException(cfg.getConnectorMessages().format(MSG_ERROR_EXECUTING_SEARCH, null), e);
		}
        finally{
        	SQLUtil.closeQuietly(rs);
        	SQLUtil.closeQuietly(st);
        }
		
	}
	
	//This is hack for dummy applications, that send Name with wrong case
	private void handleCaseInsensitiveNames(Pair<String, FilterWhereBuilder> pair,
			Collection<String> attributesToGet, ResultsHandler handler,
			OracleUserReader userReader) throws SQLException {
        DatabaseQueryBuilder query = new DatabaseQueryBuilder(pair.first);
        query.setWhere(pair.second);
        String sql = query.getSQL();
		if("SELECT DISTINCT DBA_USERS.* FROM DBA_USERS WHERE DBA_USERS.USERNAME = ?".equals(sql)){
	        sql = "SELECT DISTINCT DBA_USERS.* FROM DBA_USERS WHERE DBA_USERS.USERNAME = UPPER(?)";
	        PreparedStatement st = null;
	        ResultSet rs = null;
	        try{
				st = this.adminConn.prepareStatement(sql);
	            SQLUtil.setParams(st, query.getParams());
				rs = st.executeQuery();
				if(rs.next()){
					ConnectorObjectBuilder builder = buildConnectorObject(rs, userReader, attributesToGet);
	                //We must set name to value from filter, otherwise framework would filter it
					String name = (String) query.getParams().get(0).getValue();
					builder.addAttribute(new Name(name));
					handler.handle(builder.build());
				}
	        }
	        finally{
	        	SQLUtil.closeQuietly(rs);
	        	SQLUtil.closeQuietly(st);
	        }

		}
		
	}

	private ConnectorObjectBuilder buildConnectorObject(ResultSet rs,
			OracleUserReader userReader, Collection<String> attributesToGet)
			throws SQLException {
		ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
		final String userName = rs.getString("USERNAME");
		bld.setUid(new Uid(userName));
		bld.setName(userName);
		bld.setObjectClass(ObjectClass.ACCOUNT);
		if(attributesToGet.contains(Name.NAME)){
			bld.addAttribute(new Name(userName));
		}
		UserRecord record = OracleUserReader.translateRowToUserRecord(rs);
		if(attributesToGet.contains(OracleConstants.ORACLE_DEF_TS_ATTR_NAME)){
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,record.getDefaultTableSpace()));
		}
		if(attributesToGet.contains(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME)){
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME,record.getTemporaryTableSpace()));
		}
		if(attributesToGet.contains(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME)){
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleUserReader.resolveAuthentication(record).toString()));
		}
		if(attributesToGet.contains(OracleConstants.ORACLE_GLOBAL_ATTR_NAME)){
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,record.getExternalName()));
		}
		if(attributesToGet.contains(OracleConstants.ORACLE_PROFILE_ATTR_NAME)){
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OracleConstants.ORACLE_PROFILE_ATTR_NAME,record.getProfile()));
		}
		if(attributesToGet.contains(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME)){
			Long quota = userReader.readUserTSQuota(userName, record.getDefaultTableSpace());
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME,quota != null ? quota.toString() : null));
		}
		if(attributesToGet.contains(OracleConstants.ORACLE_TEMP_TS_QUOTA_ATTR_NAME)){
			Long quota = userReader.readUserTSQuota(userName, record.getTemporaryTableSpace());
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OracleConstants.ORACLE_TEMP_TS_QUOTA_ATTR_NAME,quota != null ? quota.toString() : null));
		}
		if(attributesToGet.contains(OracleConstants.ORACLE_PRIVS_ATTR_NAME)){
			bld.addAttribute(AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,new OracleRolePrivReader(adminConn).readPrivileges(userName)));
		}
		if(attributesToGet.contains(OracleConstants.ORACLE_ROLES_ATTR_NAME)){
			bld.addAttribute(AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,new OracleRolePrivReader(adminConn).readRoles(userName)));
		}
		if(attributesToGet.contains(OperationalAttributes.PASSWORD_EXPIRED_NAME)){
			bld.addAttribute(AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.valueOf(record.getStatus().contains("EXPIRED"))));
		}
		if(attributesToGet.contains(OperationalAttributes.ENABLE_NAME)){
			bld.addAttribute(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.valueOf(!record.getStatus().contains("LOCKED"))));
		}
		if(attributesToGet.contains(OperationalAttributes.LOCK_OUT_NAME)){
			bld.addAttribute(AttributeBuilder.build(OperationalAttributes.LOCK_OUT_NAME,Boolean.valueOf(record.getStatus().contains("LOCKED"))));
		}
		if(attributesToGet.contains(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME)){
			Long date = record.getExpireDate() != null ? record.getExpireDate().getTime() : null;
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,date));
		}
		if(attributesToGet.contains(OperationalAttributes.DISABLE_DATE_NAME)){
			Long date = record.getLockDate() != null ? record.getLockDate().getTime() : null;
			bld.addAttribute(OracleConnectorHelper.buildSingleAttribute(OperationalAttributes.DISABLE_DATE_NAME,date));
		}
		return bld;
	}
	
	private void checkAttributesToGet(String[] attributesToGet) {
		if(attributesToGet == null){
			return;
		}
		for(String attribute : attributesToGet){
			//We do not need to use Attribute.is, we use Attribute comparator
			if(!VALID_ATTRIBUTES_TO_GET.contains(attribute)){
				throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_SEARCH_ATTRIBUTE_NOT_SUPPORTED_FOR_ATTRIBUTESTOGET, null, attribute));
			}
		}
	}

	private static final class OracleDBFilterTranslator extends DatabaseFilterTranslator{
		private String select = SQL;
		private final ConnectorMessages cm;
		private final OracleCaseSensitivitySetup cs;
		OracleDBFilterTranslator(ObjectClass oclass, OperationOptions options, ConnectorMessages cm, OracleCaseSensitivitySetup cs) {
			super(oclass, options);
			this.cm = OracleConnectorHelper.assertNotNull(cm, "cm");
			this.cs = OracleConnectorHelper.assertNotNull(cs, "cs");
		}

		@Override
		protected String getDatabaseColumnName(Attribute attribute, ObjectClass oclass, OperationOptions options) {
			checkSearchByAttribute(attribute);
			//format sql column using Formatter for concrete attribute.
			//Formatter can then e.g surround column with UPPER function 
			if(attribute.is(Name.NAME)){
				return cs.formatSQLColumn(OracleUserAttributeCS.USER, "DBA_USERS.USERNAME");
			}
			//we do not normalize UID 
			else if(attribute.is(Uid.NAME)){
				return cs.formatSQLColumn(OracleUserAttributeCS.USER, "DBA_USERS.USERNAME");
			}
			else if(attribute.is(OracleConstants.ORACLE_DEF_TS_ATTR_NAME)){
				return cs.formatSQLColumn(OracleUserAttributeCS.DEF_TABLESPACE, "DBA_USERS.DEFAULT_TABLESPACE");
			}
			else if(attribute.is(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME)){
				return cs.formatSQLColumn(OracleUserAttributeCS.TEMP_TABLESPACE, "DBA_USERS.TEMPORARY_TABLESPACE");
			}
			else if(attribute.is(OracleConstants.ORACLE_PROFILE_ATTR_NAME)){
				return cs.formatSQLColumn(OracleUserAttributeCS.PROFILE, "DBA_USERS.PROFILE");
			}
			else if(attribute.is(OracleConstants.ORACLE_GLOBAL_ATTR_NAME)){
				return cs.formatSQLColumn(OracleUserAttributeCS.GLOBAL_NAME, "DBA_USERS.EXTERNAL_NAME");
			}
			else if(attribute.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)){
				return "(CASE WHEN DBA_USERS.ACCOUNT_STATUS LIKE '%EXPIRED%' THEN 'EXPIRED' ELSE 'NOT_EXPIRED' END)";
			}
			else if(attribute.is(OperationalAttributes.ENABLE_NAME) || attribute.is(OperationalAttributes.LOCK_OUT_NAME)){
				return "(CASE WHEN DBA_USERS.ACCOUNT_STATUS LIKE '%LOCKED%' THEN 'LOCKED' ELSE 'NOT_LOCKED' END)";
			}
			else if(attribute.is(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME)){
				return "DBA_USERS.EXPIRY_DATE";
			}
			else if(attribute.is(OperationalAttributes.DISABLE_DATE_NAME)){
				return "DBA_USERS.LOCK_DATE";
			}
			else if(attribute.is(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME)){
				if(select == SQL){
					select = ADVANCED_SQL1;
				}
				return "DEF_QUOTA.MAX_BYTES";
			}
			else if(attribute.is(OracleConstants.ORACLE_TEMP_TS_QUOTA_ATTR_NAME)){
				if(select == SQL){
					select = ADVANCED_SQL1;
				}
				return "TEMP_QUOTA.MAX_BYTES";
			}
			else if(attribute.is(OracleConstants.ORACLE_ROLES_ATTR_NAME)){
				select = ADVANCED_SQL2;
				return cs.formatSQLColumn(OracleUserAttributeCS.ROLE, "GRANTED_ROLE");
			}
			else if(attribute.is(OracleConstants.ORACLE_PRIVS_ATTR_NAME)){
				select = ADVANCED_SQL2;
				return cs.formatSQLColumn(OracleUserAttributeCS.PRIVILEGE, "GRANTED_ROLE");
			}
			else if(attribute.is(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME)){
				return "(CASE WHEN DBA_USERS.PASSWORD='EXTERNAL' THEN 'EXTERNAL' ELSE (CASE WHEN DBA_USERS.EXTERNAL_NAME IS NOT NULL THEN 'GLOBAL' ELSE 'LOCAL' END) END)";
			}
			//Should not get here, invalid attributes should be already handled
			throw new IllegalArgumentException("Cannot map db column for attribute : " + attribute.getName());
		}

		private void checkSearchByAttribute(Attribute attribute) {
			if(!VALID_SEARCH_BY_ATTRIBUTES.contains(attribute.getName())){
				throw new IllegalArgumentException(cm.format(MSG_SEARCH_ATTRIBUTE_NOT_SUPPORTED_FOR_SEARCHBY, null, attribute.getName()));
			}
		}

		@Override
		protected SQLParam getSQLParam(Attribute attribute, ObjectClass oclass, OperationOptions options) {
			checkSearchByAttribute(attribute);
			if(attribute.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)){
				Boolean value = (Boolean) AttributeUtil.getSingleValue(attribute);
				if(value == null){
					return null;
				}
				return value ? new SQLParam("EXPIRED",Types.VARCHAR) : new SQLParam("NOT_EXPIRED",Types.VARCHAR);
			}
			else if(attribute.is(OperationalAttributes.ENABLE_NAME)){
				Boolean value = (Boolean) AttributeUtil.getSingleValue(attribute);
				if(value == null){
					return null;
				}
				return value ? new SQLParam("NOT_LOCKED",Types.VARCHAR) : new SQLParam("LOCKED",Types.VARCHAR);
			}
			else if(attribute.is(OperationalAttributes.LOCK_OUT_NAME)){
				Boolean value = (Boolean) AttributeUtil.getSingleValue(attribute);
				if(value == null){
					return null;
				}
				return value ? new SQLParam("LOCKED",Types.VARCHAR) : new SQLParam("NOT_LOCKED",Types.VARCHAR);
			}
			else if(attribute.is(OperationalAttributes.DISABLE_DATE_NAME)){
				Object date = AttributeUtil.getSingleValue(attribute);
				if(date instanceof Long){
					date = new java.sql.Timestamp(((Long)date));
				}
				return new SQLParam(date,Types.TIMESTAMP);
			}
			else if(attribute.is(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME)){
				Object date = AttributeUtil.getSingleValue(attribute);
				if(date instanceof Long){
					date = new java.sql.Timestamp(((Long)date));
				}
				return new SQLParam(date,Types.TIMESTAMP);
			}
			return new SQLParam(AttributeUtil.getSingleValue(attribute),Types.VARCHAR);
		}
		
		@Override
		protected boolean validateSearchAttribute(Attribute attribute) {
			checkSearchByAttribute(attribute);
			//Currently We do not support in filter
			if(attribute.is(OracleConstants.ORACLE_ROLES_ATTR_NAME)){
				return false;
			}
			if(attribute.is(OracleConstants.ORACLE_PRIVS_ATTR_NAME)){
				return false;
			}
			return true;
		}
	}
	
	
	private static final class OracleFilterTranslator implements FilterTranslator<Pair<String, FilterWhereBuilder>>{
		private final OracleDBFilterTranslator delegate ;
		OracleFilterTranslator(ObjectClass oclass, OperationOptions options, ConnectorMessages cm, OracleCaseSensitivitySetup cs) {
			delegate = new OracleDBFilterTranslator(oclass, options, cm, cs);
		}
		public List<Pair<String, FilterWhereBuilder>> translate(Filter filter) {
			List<FilterWhereBuilder> list = delegate.translate(filter);
			List<Pair<String, FilterWhereBuilder>> result = new ArrayList<Pair<String,FilterWhereBuilder>>();
			if(list == null || list.isEmpty()){
				//We will send at least sql
				result.add(new Pair<String, FilterWhereBuilder>(SQL,null));
				return result;
			}
			for(FilterWhereBuilder where : list){
				result.add(new Pair<String, FilterWhereBuilder>(delegate.select,where));
			}
			return result;
		}
		
	}
	
	 
	
	

}
