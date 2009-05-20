/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.spi.operations.SchemaOp;

/**
 * Constructs schema for Oracle connector
 * @author kitko
 *
 */
final class OracleOperationSchema extends AbstractOracleOperation implements SchemaOp {

	OracleOperationSchema(OracleConfiguration cfg, Connection adminConn,
			Log log) {
		super(cfg, adminConn, log);
	}

	public Schema schema() {
		String dbVersion = null;
		try{
			dbVersion = adminConn.getMetaData().getDatabaseProductVersion();
		}
		catch(SQLException e){
			//This is internal error
			throw new ConnectorException("Cannot resolve getMetaData().getDatabaseProductVersion()", e);
		}
		boolean express = false;
		if(dbVersion.contains("Express")){
			express = true;
		}
        Set<AttributeInfo> attrInfoSet = new HashSet<AttributeInfo>();
        attrInfoSet.add(AttributeInfoBuilder.build(Name.NAME,String.class,EnumSet.of(Flags.NOT_UPDATEABLE,Flags.REQUIRED)));
        attrInfoSet.add(OperationalAttributeInfos.PASSWORD);
        attrInfoSet.add(OperationalAttributeInfos.PASSWORD_EXPIRED);
        attrInfoSet.add(OperationalAttributeInfos.ENABLE);
        attrInfoSet.add(AttributeInfoBuilder.build(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,Long.class,EnumSet.of(Flags.NOT_UPDATEABLE,Flags.NOT_CREATABLE)));
        attrInfoSet.add(AttributeInfoBuilder.build(OperationalAttributes.DISABLE_DATE_NAME,Long.class,EnumSet.of(Flags.NOT_UPDATEABLE,Flags.NOT_CREATABLE)));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,String.class,EnumSet.of(Flags.REQUIRED)));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,String.class,express ? EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE) : null));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,String.class,EnumSet.of(Flags.MULTIVALUED)));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,String.class,EnumSet.of(Flags.MULTIVALUED)));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(
				OracleConstants.ORACLE_TEMP_TS_QUOTA_ATTR_NAME, String.class, express ? EnumSet
						.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE) : null));
        SchemaBuilder schemaBld = new SchemaBuilder(OracleConnectorImpl.class);
        schemaBld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfoSet);
        Schema schema =  schemaBld.build();
        return schema;
	}

}
