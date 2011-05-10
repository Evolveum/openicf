/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.*;
import java.util.*;

import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.*;
import org.identityconnectors.framework.spi.operations.*;

/**
 * Constructs schema for Oracle connector.
 * Connector uses hardcoded schema , it just checks the version of oracle and support/does not support global authentication.
 * @author kitko
 *
 */
final class OracleOperationSchema extends AbstractOracleOperation implements SchemaOp {
	//Last veersion for oracle where oracle supports quotas for temporary table spaces
	private static final Pair<Integer, Integer> LAST_TMP_TS_QUOTA_VERSION = new Pair<Integer, Integer>(10,1);
	
	OracleOperationSchema(OracleConfiguration cfg, Connection adminConn) {
		super(cfg, adminConn);
	}

	public Schema schema() {
		String dbProductVersion = null;
		Pair<Integer, Integer> dbVersion = null;;
		try{
			DatabaseMetaData metaData = adminConn.getMetaData();
			dbVersion = new Pair<Integer, Integer>(metaData.getDatabaseMajorVersion(), metaData.getDatabaseMinorVersion());
			dbProductVersion = metaData.getDatabaseProductVersion();
		}
		catch(SQLException e){
			//This is internal error
			throw new ConnectorException("Cannot resolve getMetaData().getDatabaseProductVersion()", e);
		}
		boolean express = false;
		if(dbProductVersion.contains("Express")){
			express = true;
		}
        Set<AttributeInfo> attrInfoSet = new HashSet<AttributeInfo>();
        attrInfoSet.add(AttributeInfoBuilder.build(Name.NAME,String.class,EnumSet.of(Flags.NOT_UPDATEABLE,Flags.REQUIRED)));
        attrInfoSet.add(OperationalAttributeInfos.PASSWORD);
        attrInfoSet.add(OperationalAttributeInfos.PASSWORD_EXPIRED);
        attrInfoSet.add(OperationalAttributeInfos.ENABLE);
        attrInfoSet.add(OperationalAttributeInfos.LOCK_OUT); //This is implemented same like disable
        attrInfoSet.add(AttributeInfoBuilder.build(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,Long.class,EnumSet.of(Flags.NOT_UPDATEABLE,Flags.NOT_CREATABLE)));
        attrInfoSet.add(AttributeInfoBuilder.build(OperationalAttributes.DISABLE_DATE_NAME,Long.class,EnumSet.of(Flags.NOT_UPDATEABLE,Flags.NOT_CREATABLE)));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,String.class,express ? EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE) : null));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,String.class,EnumSet.of(Flags.MULTIVALUED)));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,String.class,EnumSet.of(Flags.MULTIVALUED)));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_TEMP_TS_QUOTA_ATTR_NAME,String.class, !isTempTsQuotaWriteable(dbVersion) ? EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE) : null));
        SchemaBuilder schemaBld = new SchemaBuilder(OracleConnector.class);
        schemaBld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfoSet);
        Schema schema =  schemaBld.build();
        return schema;
	}

	/** If current version of oracle is greater then LAST_TMP_TS_QUOTA_VERSION, we will not support
	 *  writeable temporary table space quotas
	 * @param dbVersion
	 * @return true resource supports ORACLE_DEF_TS_QUOTA_ATTR_NAME attribute
	 */
	private boolean isTempTsQuotaWriteable(Pair<Integer, Integer> dbVersion) {
		if(dbVersion.getFirst() < LAST_TMP_TS_QUOTA_VERSION.getFirst()){
			return true;
		}
		else if(dbVersion.getFirst() > LAST_TMP_TS_QUOTA_VERSION.getFirst()){
			return false;
		}
		else{
			return dbVersion.getSecond() <= LAST_TMP_TS_QUOTA_VERSION.getSecond();
		}
	}

}
