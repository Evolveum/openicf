/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import org.identityconnectors.common.Pair;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;
import static org.identityconnectors.oracle.OracleMessages.*;

/**
 * Implementation of Oracle connector. It just holds common oracle constants and delegates SPI calls to AbstractOracleOperation subclasses
 * @author kitko
 *
 */
@ConnectorClass(configurationClass=OracleConfiguration.class,
        displayNameKey = CONNECTOR_DISPLAY,
        messageCatalogPaths={"org/identityconnectors/dbcommon/Messages","org/identityconnectors/oracle/Messages"})
public final class OracleConnector implements PoolableConnector, AuthenticateOp,
		CreateOp, DeleteOp, UpdateOp, UpdateAttributeValuesOp,
		SearchOp<Pair<String, FilterWhereBuilder>>, SchemaOp,TestOp, AttributeNormalizer {
    private Connection adminConn;
    private OracleConfiguration cfg;
    private Schema schema;
    private final static Log log = Log.getLog(OracleConnector.class);
    
    static final String ORACLE_AUTHENTICATION_ATTR_NAME = "oracleAuthentication";
    static final String ORACLE_GLOBAL_ATTR_NAME = "oracleGlobalName";
    static final String ORACLE_ROLES_ATTR_NAME = "oracleRoles";
    static final String ORACLE_PRIVS_ATTR_NAME = "oraclePrivs";
    static final String ORACLE_PROFILE_ATTR_NAME = "oracleProfile";
    static final String ORACLE_DEF_TS_ATTR_NAME = "oracleDefaultTS";
    static final String ORACLE_TEMP_TS_ATTR_NAME = "oracleTempTS";
    static final String ORACLE_DEF_TS_QUOTA_ATTR_NAME = "oracleDefaultTSQuota";
    static final String ORACLE_TEMP_TS_QUOTA_ATTR_NAME = "oracleTempTSQuota";
    
    static final String ORACLE_AUTH_LOCAL = "LOCAL";
    static final String ORACLE_AUTH_EXTERNAL = "EXTERNAL";
    static final String ORACLE_AUTH_GLOBAL = "GLOBAL";
    static final String NO_CASCADE = "noCascade";
    
    private static final Map<String,OracleUserAttributeCS> attributeMapping = new HashMap<String, OracleUserAttributeCS>();
    static final Collection<String> ALL_ATTRIBUTE_NAMES = new HashSet<String>();
    static {
        attributeMapping.put(Name.NAME, OracleUserAttributeCS.USER_NAME);
        attributeMapping.put(Uid.NAME, OracleUserAttributeCS.USER_NAME);
        attributeMapping.put(ORACLE_GLOBAL_ATTR_NAME, OracleUserAttributeCS.GLOBAL_NAME);
        attributeMapping.put(ORACLE_ROLES_ATTR_NAME, OracleUserAttributeCS.ROLE);
        attributeMapping.put(ORACLE_PRIVS_ATTR_NAME, OracleUserAttributeCS.PRIVILEGE);
        attributeMapping.put(ORACLE_PROFILE_ATTR_NAME, OracleUserAttributeCS.PROFILE);
        attributeMapping.put(ORACLE_DEF_TS_ATTR_NAME, OracleUserAttributeCS.DEF_TABLESPACE);
        attributeMapping.put(ORACLE_TEMP_TS_ATTR_NAME, OracleUserAttributeCS.TEMP_TABLESPACE);
        
        ALL_ATTRIBUTE_NAMES.addAll(Arrays.asList(
				ORACLE_AUTHENTICATION_ATTR_NAME, ORACLE_GLOBAL_ATTR_NAME,
				ORACLE_ROLES_ATTR_NAME, ORACLE_PRIVS_ATTR_NAME,
				ORACLE_PROFILE_ATTR_NAME, ORACLE_DEF_TS_ATTR_NAME,
				ORACLE_TEMP_TS_ATTR_NAME, ORACLE_DEF_TS_QUOTA_ATTR_NAME,
				ORACLE_TEMP_TS_QUOTA_ATTR_NAME,
				OperationalAttributes.PASSWORD_EXPIRED_NAME,OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,
				OperationalAttributes.ENABLE_NAME,OperationalAttributes.DISABLE_DATE_NAME,
				Name.NAME,OperationalAttributes.PASSWORD_NAME
				));
    }
    
    
    public void checkAlive() {
    	testInitialized();
        OracleSpecifics.testConnection(adminConn);
    }

    public void dispose() {
        SQLUtil.closeQuietly(adminConn);
    }

    public OracleConfiguration getConfiguration() {
        return cfg;
    }

    public void init(Configuration cfg) {
        this.cfg = (OracleConfiguration) cfg;
        this.adminConn = createAdminConnection();
    }
    
    /**
     * Test of configuration and validity of connection
     */
    public void test() {
    	testInitialized();
        cfg.validate();
        OracleSpecifics.testConnection(adminConn);
    }
    

    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
        return new OracleOperationAuthenticate(cfg, adminConn, log).authenticate(objectClass, username, password, options);
    }
    
    private Connection createAdminConnection(){
        return cfg.createConnection(cfg.getUser(), cfg.getPassword());
    }

    
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        new OracleOperationDelete(cfg, adminConn, log).delete(objClass, uid, options);
    }
    
    Connection getAdminConnection(){
        return adminConn;
    }
    
    static Log getLog(){
        return log;
    }

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        return new OracleOperationCreate(cfg, adminConn, log).create(oclass, attrs, options);
    }
    
    static void checkObjectClass(ObjectClass objectClass,ConnectorMessages messages){
        if(!ObjectClass.ACCOUNT.equals(objectClass)){
            throw new IllegalArgumentException("Invalid obejct class");
        }
    }

    public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
    	if(attribute == null){
    		return null;
    	}
        String name = attribute.getName();
        final OracleUserAttributeCS oracleUserAttribute = attributeMapping.get(name);
        if(oracleUserAttribute == null){
            return attribute;
        }
        List<Object> values = new ArrayList<Object>();
        if(attribute.getValue() == null){
        	return attribute;
        }
        for(Object o : attribute.getValue()){
            if(o instanceof String){
                o = cfg.getCSSetup().normalizeToken(oracleUserAttribute, (String) o);
            }
            values.add(o);
        }
        return AttributeBuilder.build(name,values);
    }
    
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        return new OracleOperationUpdate(cfg, adminConn, log).update(objclass, uid, attrs, options);
    }

    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        return new OracleOperationUpdate(cfg, adminConn, log).addAttributeValues(objclass, uid, valuesToAdd, options);
    }

    public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        return new OracleOperationUpdate(cfg, adminConn, log).removeAttributeValues(objclass, uid, valuesToRemove, options);
    }

	public FilterTranslator<Pair<String, FilterWhereBuilder>> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
		return new OracleOperationSearch(cfg, adminConn, log).createFilterTranslator(oclass, options);
	}

	public void executeQuery(ObjectClass oclass, Pair<String, FilterWhereBuilder> pair, ResultsHandler handler, OperationOptions options) {
		new OracleOperationSearch(cfg, adminConn, log).executeQuery(oclass, pair, handler, options);
	}

	public Schema schema() {
		if(schema != null){
			return schema;
		}
		String dbVersion = null;
		try{
			dbVersion = adminConn.getMetaData().getDatabaseProductVersion();
		}
		catch(SQLException e){
			throw new ConnectorException("Cannot resolve getMetaData().getDatabaseProductVersion()",e);
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
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_AUTHENTICATION_ATTR_NAME,String.class,EnumSet.of(Flags.REQUIRED)));
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_GLOBAL_ATTR_NAME,String.class,express ? EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE) : null));
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_ROLES_ATTR_NAME,String.class,EnumSet.of(Flags.MULTIVALUED)));
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_PRIVS_ATTR_NAME,String.class,EnumSet.of(Flags.MULTIVALUED)));
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_PROFILE_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_DEF_TS_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_TEMP_TS_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_DEF_TS_QUOTA_ATTR_NAME,String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(ORACLE_TEMP_TS_QUOTA_ATTR_NAME,String.class,express ? EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE) : null));
        SchemaBuilder schemaBld = new SchemaBuilder(getClass());
        schemaBld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfoSet);
        schema =  schemaBld.build();
        return schema;
	}
	
	private void testInitialized(){
		if(cfg == null || adminConn == null){
			throw new ConnectorException("Connector is not yet initialized");
		}
	}
    
    

}
