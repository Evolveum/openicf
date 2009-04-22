package org.identityconnectors.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.identityconnectors.oracle.OracleConnector.*;

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
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.operations.SearchOp;


class OracleOperationSearch extends AbstractOracleOperation implements SearchOp<Pair<String, FilterWhereBuilder>>{
	private static final String SQL = "SELECT DISTINCT DBA_USERS.USERNAME FROM DBA_USERS";
	
	private static final String ADVANCED_SQL1 = SQL + 	" LEFT JOIN DBA_TS_QUOTAS DEF_QUOTA " +
    													"ON DBA_USERS.USERNAME = DEF_QUOTA.USERNAME AND DBA_USERS.DEFAULT_TABLESPACE=DEF_QUOTA.TABLESPACE_NAME " + 
    													"LEFT JOIN DBA_TS_QUOTAS TEMP_QUOTA " + 
    													"ON DBA_USERS.USERNAME = DEF_QUOTA.USERNAME AND DBA_USERS.TEMPORARY_TABLESPACE=TEMP_QUOTA.TABLESPACE_NAME";
	
	private static final String ADVANCED_SQL2 = ADVANCED_SQL1 + " LEFT JOIN DBA_ROLE_PRIVS " +
																"ON DBA_USERS.USERNAME=DBA_ROLE_PRIVS.GRANTEE " + 
																"LEFT JOIN DBA_SYS_PRIVS ON DBA_USERS.USERNAME=DBA_SYS_PRIVS.GRANTEE " + 
																"LEFT JOIN USER_TAB_PRIVS ON DBA_USERS.USERNAME=USER_TAB_PRIVS.GRANTEE";

	
	OracleOperationSearch(OracleConfiguration cfg, Connection adminConn, Log log) {
		super(cfg, adminConn, log);
	}

	@Override
	public FilterTranslator<Pair<String, FilterWhereBuilder>> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
		return new OracleFilterTranslator(oclass,options);
	}

	@Override
	public void executeQuery(ObjectClass oclass, Pair<String, FilterWhereBuilder> pair, ResultsHandler handler, OperationOptions options) {
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(pair.first);
        query.setWhere(pair.second);
        final String sql = query.getSQL();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
			st = this.adminConn.prepareStatement(sql);
            SQLUtil.setParams(st, query.getParams());
			rs = st.executeQuery();
			OracleUserReader userReader = new OracleUserReader(adminConn);
			while(rs.next()){
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                final String userName = rs.getString("USERNAME");
                bld.setUid(new Uid(userName));
                bld.setName(userName);
                bld.setObjectClass(ObjectClass.ACCOUNT);
                //Better would be to have it in one select, but this way, it is much more readable
                UserRecord record = userReader.readUserRecord(userName);
                Set<String> attributesToGet = null;
                if(options.getAttributesToGet() != null && options.getAttributesToGet().length > 0){
                	attributesToGet = new HashSet<String>(Arrays.asList(options.getAttributesToGet()));
                }
                else{
                	attributesToGet = new HashSet<String>(Arrays.asList(ORACLE_DEF_TS_ATTR_NAME,ORACLE_GLOBAL_ATTR_NAME,ORACLE_AUTHENTICATION_ATTR_NAME,
                			ORACLE_PROFILE_ATTR_NAME,ORACLE_TEMP_TS_ATTR_NAME,ORACLE_DEF_TS_QUOTA_ATTR_NAME,ORACLE_TEMP_TS_QUOTA_ATTR_NAME,
                			ORACLE_PRIVS_ATTR_NAME,ORACLE_ROLES_ATTR_NAME));
                }
                if(attributesToGet.contains(ORACLE_DEF_TS_ATTR_NAME)){
                	bld.addAttribute(AttributeBuilder.build(ORACLE_DEF_TS_ATTR_NAME,record.defaultTableSpace));
                }
                if(attributesToGet.contains(ORACLE_TEMP_TS_ATTR_NAME)){
                	bld.addAttribute(AttributeBuilder.build(ORACLE_TEMP_TS_ATTR_NAME,record.temporaryTableSpace));
                }
                if(attributesToGet.contains(ORACLE_AUTHENTICATION_ATTR_NAME)){
                	bld.addAttribute(AttributeBuilder.build(ORACLE_AUTHENTICATION_ATTR_NAME,userReader.resolveAuthentication(record).toString()));
                }
                if(attributesToGet.contains(ORACLE_GLOBAL_ATTR_NAME)){
                	bld.addAttribute(AttributeBuilder.build(ORACLE_GLOBAL_ATTR_NAME,record.externalName));
                }
                if(attributesToGet.contains(ORACLE_PROFILE_ATTR_NAME)){
                	bld.addAttribute(AttributeBuilder.build(ORACLE_PROFILE_ATTR_NAME,record.profile));
                }
                if(attributesToGet.contains(ORACLE_DEF_TS_QUOTA_ATTR_NAME)){
                	Long quota = userReader.readUserTSQuota(userName, record.defaultTableSpace);
					bld.addAttribute(AttributeBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,quota != null ? quota.toString() : null));
                }
                if(attributesToGet.contains(ORACLE_TEMP_TS_QUOTA_ATTR_NAME)){
                	Long quota = userReader.readUserTSQuota(userName, record.temporaryTableSpace);
					bld.addAttribute(AttributeBuilder.build(ORACLE_TEMP_TS_QUOTA_ATTR_NAME,quota != null ? quota.toString() : null));
                }
                if(attributesToGet.contains(ORACLE_PRIVS_ATTR_NAME)){
                	bld.addAttribute(AttributeBuilder.build(ORACLE_PRIVS_ATTR_NAME,new OracleRolePrivReader(adminConn).readPrivileges(userName)));
                }
                if(attributesToGet.contains(ORACLE_ROLES_ATTR_NAME)){
                	bld.addAttribute(AttributeBuilder.build(ORACLE_ROLES_ATTR_NAME,new OracleRolePrivReader(adminConn).readRoles(userName)));
                }
                ConnectorObject ret = bld.build();
                if (!handler.handle(ret)) {
                    break;
                }
			}
		}
        catch (SQLException e) {
        	throw new ConnectorException("Error running search query",e);
		}
        finally{
        	SQLUtil.closeQuietly(st);
        }
		
	}
	
	private static class OracleDBFilterTranslator extends DatabaseFilterTranslator{
		private String select = SQL;
		OracleDBFilterTranslator(ObjectClass oclass,OperationOptions options) {
			super(oclass, options);
		}

		@Override
		protected String getDatabaseColumnName(Attribute attribute, ObjectClass oclass, OperationOptions options) {
			if(attribute.is(Name.NAME)){
				return "DBA_USERS.USERNAME";
			}
			if(attribute.is(OracleConnector.ORACLE_DEF_TS_ATTR_NAME)){
				return "DBA_USERS.DEFAULT_TABLESPACE";
			}
			if(attribute.is(OracleConnector.ORACLE_PROFILE_ATTR_NAME)){
				return "DBA_USERS.PROFILE";
			}
			if(attribute.is(OracleConnector.ORACLE_GLOBAL_ATTR_NAME)){
				return "DBA_USERS.EXTERNAL_NAME";
			}
			if(attribute.is(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME)){
				return "DBA_USERS.TEMPORARY_TABLESPACE";
			}
			if(attribute.is(OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME)){
				if(select == SQL){
					select = ADVANCED_SQL1;
				}
				return "DEF_QUOTA.MAX_BYTES";
			}
			if(attribute.is(OracleConnector.ORACLE_TEMP_TS_QUOTA_ATTR_NAME)){
				if(select == SQL){
					select = ADVANCED_SQL1;
				}
				return "TEMP_QUOTA.MAX_BYTES";
			}
			if(attribute.is(OracleConnector.ORACLE_ROLES_ATTR_NAME)){
				select = ADVANCED_SQL2;
				return "GRANTED_ROLE";
			}
			if(attribute.is(OracleConnector.ORACLE_PRIVS_ATTR_NAME)){
				select = ADVANCED_SQL2;
				return "GRANTED_ROLE";
			}
			return null;
		}

		@Override
		protected SQLParam getSQLParam(Attribute attribute, ObjectClass oclass, OperationOptions options) {
			return new SQLParam(AttributeUtil.getSingleValue(attribute),Types.VARCHAR);
		}

		@Override
		protected boolean validateSearchAttribute(Attribute attribute) {
			//Currently We do not support in filter
			if(attribute.is(OracleConnector.ORACLE_ROLES_ATTR_NAME)){
				return false;
			}
			if(attribute.is(OracleConnector.ORACLE_PRIVS_ATTR_NAME)){
				return false;
			}
			return true;
		}
		
		
	}
	
	
	private static class OracleFilterTranslator implements FilterTranslator<Pair<String, FilterWhereBuilder>>{
		OracleDBFilterTranslator delegate ;
		OracleFilterTranslator(ObjectClass oclass,OperationOptions options) {
			delegate = new OracleDBFilterTranslator(oclass, options);
		}
		@Override
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
