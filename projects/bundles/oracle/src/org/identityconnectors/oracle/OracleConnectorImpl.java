/**
 * 
 */
package org.identityconnectors.oracle;


import java.sql.Connection;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
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
import org.identityconnectors.framework.spi.operations.SPIOperation;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.oracle.OracleConfiguration.ConnectionType;

/**
 * Implementation of Oracle connector. It just holds common oracle constants and delegates SPI calls to AbstractOracleOperation subclasses.
 * This connector implementation does not normalize attributes.
 * @author kitko
 *
 */
final class OracleConnectorImpl implements PoolableConnector, AuthenticateOp,
		CreateOp, DeleteOp, UpdateOp, UpdateAttributeValuesOp,
		SearchOp<Pair<String, FilterWhereBuilder>>, SchemaOp,TestOp {
    
	private final static Log log = Log.getLog(OracleConnector.class);
    
    private Connection adminConn;
    private OracleConfiguration cfg;
    private Schema schema;
    
    
    public void checkAlive() {
    	testInitialized();
    	if(adminConn == null){
    		//Here we test we are still able to get new connection
    		adminConn = createAdminConnection();
    		try{
    			//Test that connection is ok, even the datasource pool can do the same
    			OracleSpecifics.testConnection(adminConn);
    		}
    		finally{
    			if(ConnectionType.DATASOURCE.equals(cfg.getConnType())){
	    			SQLUtil.closeQuietly(adminConn);
	    			adminConn = null;
    			}
    		}
    	}
    	else{
    		OracleSpecifics.testConnection(adminConn);
    	}
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
        cfg.validate();
    }
    
    /**
     * Test of configuration and validity of connection
     */
    public void test() {
    	testInitialized();
        cfg.validate();
        startSPI(TestOp.class);
        try{
        	OracleSpecifics.testConnection(adminConn);
        }
        finally{
        	finsishSPI(TestOp.class);
        }
    }
    

    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
    	startSPI(AuthenticateOp.class);
    	try{
    		return new OracleOperationAuthenticate(cfg, adminConn, log).authenticate(objectClass, username, password, options);
    	}
    	finally{
    		finsishSPI(AuthenticateOp.class);
    	}
    }
    

    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
    	startSPI(DeleteOp.class);
    	try{
    		new OracleOperationDelete(cfg, adminConn, log).delete(objClass, uid, options);
    	}
    	finally{
    		finsishSPI(DeleteOp.class);
    	}
    }
    

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
    	startSPI(CreateOp.class);
    	try{
    		return new OracleOperationCreate(cfg, adminConn, log).create(oclass, attrs, options);
    	}
    	finally{
    		finsishSPI(CreateOp.class);
    	}
    }
    
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
    	startSPI(UpdateOp.class);
    	try{
    		return new OracleOperationUpdate(cfg, adminConn, log).update(objclass, uid, attrs, options);
    	}
    	finally{
    		finsishSPI(UpdateOp.class);
    	}
    }

    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
    	startSPI(UpdateAttributeValuesOp.class);
    	try{
    		return new OracleOperationUpdate(cfg, adminConn, log).addAttributeValues(objclass, uid, valuesToAdd, options);
    	}
    	finally{
    		finsishSPI(UpdateAttributeValuesOp.class);
    	}
    }

    public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
    	startSPI(UpdateAttributeValuesOp.class);
    	try{
        	return new OracleOperationUpdate(cfg, adminConn, log).removeAttributeValues(objclass, uid, valuesToRemove, options);
    	}
    	finally{
    		finsishSPI(UpdateAttributeValuesOp.class);
    	}
    }

	public FilterTranslator<Pair<String, FilterWhereBuilder>> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
		startSPI(SearchOp.class);
		try{
			return new OracleOperationSearch(cfg, adminConn, log).createFilterTranslator(oclass, options);
		}
		finally{
			finsishSPI(SearchOp.class);
		}
		
	}

	public void executeQuery(ObjectClass oclass, Pair<String, FilterWhereBuilder> pair, ResultsHandler handler, OperationOptions options) {
		startSPI(SearchOp.class);
		try{
			new OracleOperationSearch(cfg, adminConn, log).executeQuery(oclass, pair, handler, options);
		}
		finally{
			finsishSPI(SearchOp.class);
		}
	}

	public Schema schema() {
		if(schema != null){
			return schema;
		}
		startSPI(SearchOp.class);
		try{
			schema = new OracleOperationSchema(cfg, adminConn, log).schema();
		}
		finally{
			finsishSPI(SearchOp.class);
		}
        return schema;
	}
	
	private void testInitialized(){
		if(cfg == null){
			throw new ConnectorException("Connector is not yet initialized");
		}
	}
	
    private Connection createAdminConnection(){
        return cfg.createAdminConnection();
    }
    
    /** This method is just for tests to have direct access to connection used in Connector */
    Connection getOrCreateAdminConnection(){
    	//Now we create connection lazy
    	if(adminConn == null){
    		adminConn = createAdminConnection();
    	}
        return adminConn;
    }
    
    /** Just for tests to check state of connection */
    Connection getAdminConnection(){
    	return adminConn;
    }
    
    static Log getLog(){
        return log;
    }
    

    
	private void startSPI(Class<? extends SPIOperation> op){
		if(adminConn == null){
			adminConn = createAdminConnection();
			//Here we also test the connection, because in checkAlive we might test other connection 
			OracleSpecifics.testConnection(adminConn);
		}
	}
	
	private void finsishSPI(Class<? extends SPIOperation> op){
		if(ConnectionType.DATASOURCE.equals(cfg.getConnType())){
			SQLUtil.closeQuietly(adminConn);
			adminConn = null;
		}
	}
    

}
