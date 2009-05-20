/**
 * 
 */
package org.identityconnectors.oracle;


import java.sql.Connection;
import java.util.Set;

import org.identityconnectors.common.Pair;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Implementation of Oracle connector. It just holds common oracle constants and delegates SPI calls to AbstractOracleOperation subclasses.
 * This connector implementation does not normalize attributes.
 * @author kitko
 *
 */
final class OracleConnectorImpl implements PoolableConnector, AuthenticateOp,
		CreateOp, DeleteOp, UpdateOp, UpdateAttributeValuesOp,
		SearchOp<Pair<String, FilterWhereBuilder>>, SchemaOp,TestOp {
    
	private final static Log log = Log.getLog(OracleConnectorImpl.class);
    
    private Connection adminConn;
    private OracleConfiguration cfg;
    private Schema schema;
    
    
    public void checkAlive() {
    	testInitialized();
        OracleSpecifics.testConnection(adminConn);
    }

    public void dispose() {
        SQLUtil.closeQuietly(adminConn);
        adminConn = null;
        cfg = null;
        schema = null;
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
        return cfg.createAdminConnection();
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
            throw new IllegalArgumentException("Invalid object class");
        }
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
		schema = new OracleOperationSchema(cfg, adminConn, log).schema();
        return schema;
	}
	
	private void testInitialized(){
		if(cfg == null || adminConn == null){
			throw new ConnectorException("Connector is not yet initialized");
		}
	}
    
    

}
