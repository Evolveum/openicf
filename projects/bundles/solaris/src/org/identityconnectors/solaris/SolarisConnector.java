/**
 * 
 */
package org.identityconnectors.solaris;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;

/**
 * @author david
 *
 */
@ConnectorClass(
        displayNameKey = "Solaris",
        configurationClass = SolarisConfiguration.class)
public class SolarisConnector implements PoolableConnector, AuthenticateOp, SchemaOp {

    /**
     * Setup logging for the {@link DatabaseTableConnector}.
     */
    private Log log = Log.getLog(SolarisConnector.class);
    
    private SolarisConnection _connection;

    private SolarisConfiguration _configuration; 
    
    /**
     * 
     */
    public SolarisConnector() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.PoolableConnector#checkAlive()
     */
    public void checkAlive() {
        _connection.test();
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.Connector#dispose()
     */
    public void dispose() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.Connector#getConfiguration()
     */
    public Configuration getConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(Configuration cfg) {
        _configuration = (SolarisConfiguration) cfg;
        _connection = new SolarisConnection(_configuration);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options) {
        // TODO Auto-generated method stub
        return null;
    }

    public Schema schema() {
        // TODO Auto-generated method stub
        return null;
    }

}
