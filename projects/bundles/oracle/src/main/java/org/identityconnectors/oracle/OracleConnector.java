/**
 * 
 */
package org.identityconnectors.oracle;


import static org.identityconnectors.oracle.OracleMessages.ORACLE_CANNOT_CREATE_TEST_USER;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

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
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
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
 * Implementation of Oracle connector. It just delegates SPI calls to {@link AbstractOracleOperation} subclasses.
 * This connector implementation does not normalize attributes.
 * @author kitko
 *
 */
@ConnectorClass(configurationClass=OracleConfiguration.class,
        displayNameKey = OracleMessages.MSG_CONNECTOR_DISPLAY,
        messageCatalogPaths={"org/identityconnectors/dbcommon/Messages","org/identityconnectors/oracle/Messages"})
public final class OracleConnector implements PoolableConnector, AuthenticateOp,
		CreateOp, DeleteOp, UpdateOp, UpdateAttributeValuesOp,
		SearchOp<Pair<String, FilterWhereBuilder>>, SchemaOp,TestOp,AttributeNormalizer {
    
    private Connection adminConn;
    private OracleConfiguration cfg;
    private Schema schema;
    private OracleAttributeNormalizer normalizer;
    
    
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
        	testUseDriverForAuthentication();
        }
        finally{
        	finsishSPI(TestOp.class);
        }
    }
    
    private void testUseDriverForAuthentication(){
        if(cfg.isUseDriverForAuthentication()){
            //Ok, here it means we are using datasource
            //So try to get OracleDriverConnectionInfo and create connection
            OracleDriverConnectionInfo connInfo = OracleSpecifics.parseConnectionInfo(adminConn, cfg.getConnectorMessages());
            //Here we need some dummy user/password to test authenticate. Can these fail because of some resource configuration ?
            //Create the user
            String userName = "test" + System.currentTimeMillis();
            try{
                SQLUtil.executeUpdateStatement(adminConn, "create user " + userName + " identified by " + userName);
                SQLUtil.executeUpdateStatement(adminConn, "grant create session to " + userName);
                OracleDriverConnectionInfo newInfo = new OracleDriverConnectionInfo.Builder().setvalues(connInfo).setUser(userName).setPassword(new GuardedString(userName.toCharArray())).build();
                Connection conn = OracleSpecifics.createDriverConnection(newInfo, cfg.getConnectorMessages());
                conn.close();
            }
            catch(SQLException e){
                throw new ConnectorException(ORACLE_CANNOT_CREATE_TEST_USER,e);
            }
            finally{
                try{
                    SQLUtil.executeUpdateStatement(adminConn, "drop user " + userName);
                }
                catch(SQLException e){
                    //Should not happen
                    throw new ConnectorException("Cannot drop testuser", e);
                }
            }
        }
    }
    

    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
    	startSPI(AuthenticateOp.class);
    	try{
    		Pair<String, GuardedString> pair = createOracleAttributeNormalizer().normalizeAuthenticateEntry(username, password);
    		username = pair.getFirst();
    		password = pair.getSecond();
    		return new OracleOperationAuthenticate(cfg, adminConn).authenticate(objectClass, username, password, options);
    	}
    	finally{
    		finsishSPI(AuthenticateOp.class);
    	}
    }
    

    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
    	startSPI(DeleteOp.class);
    	try{
    		new OracleOperationDelete(cfg, adminConn).delete(objClass, uid, options);
    	}
    	finally{
    		finsishSPI(DeleteOp.class);
    	}
    }
    

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
    	startSPI(CreateOp.class);
    	try{
    		attrs = createOracleAttributeNormalizer().normalizeAttributes(oclass, CreateOp.class, attrs);
    		return new OracleOperationCreate(cfg, adminConn).create(oclass, attrs, options);
    	}
    	finally{
    		finsishSPI(CreateOp.class);
    	}
    }
    
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
    	startSPI(UpdateOp.class);
    	try{
    		attrs = createOracleAttributeNormalizer().normalizeAttributes(objclass, UpdateOp.class, attrs);
    		return new OracleOperationUpdate(cfg, adminConn).update(objclass, uid, attrs, options);
    	}
    	finally{
    		finsishSPI(UpdateOp.class);
    	}
    }

    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
    	startSPI(UpdateAttributeValuesOp.class);
    	try{
    		valuesToAdd = createOracleAttributeNormalizer().normalizeAttributes(objclass, UpdateOp.class, valuesToAdd);
    		return new OracleOperationUpdate(cfg, adminConn).addAttributeValues(objclass, uid, valuesToAdd, options);
    	}
    	finally{
    		finsishSPI(UpdateAttributeValuesOp.class);
    	}
    }

    public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
    	startSPI(UpdateAttributeValuesOp.class);
    	try{
    		valuesToRemove = createOracleAttributeNormalizer().normalizeAttributes(objclass, UpdateOp.class, valuesToRemove);
        	return new OracleOperationUpdate(cfg, adminConn).removeAttributeValues(objclass, uid, valuesToRemove, options);
    	}
    	finally{
    		finsishSPI(UpdateAttributeValuesOp.class);
    	}
    }

	public FilterTranslator<Pair<String, FilterWhereBuilder>> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
		startSPI(SearchOp.class);
		try{
			return new OracleOperationSearch(cfg, adminConn).createFilterTranslator(oclass, options);
		}
		finally{
			finsishSPI(SearchOp.class);
		}
		
	}

	public void executeQuery(ObjectClass oclass, Pair<String, FilterWhereBuilder> pair, ResultsHandler handler, OperationOptions options) {
		startSPI(SearchOp.class);
		try{
			new OracleOperationSearch(cfg, adminConn).executeQuery(oclass, pair, handler, options);
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
			schema = new OracleOperationSchema(cfg, adminConn).schema();
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

	public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
		return createOracleAttributeNormalizer().normalizeAttribute(oclass, attribute);
	}
	
	private OracleAttributeNormalizer createOracleAttributeNormalizer(){
		if(normalizer == null){
			normalizer = cfg.getNormalizerName().createNormalizer(cfg.getCSSetup());
		}
		return normalizer;
	}
    

}
