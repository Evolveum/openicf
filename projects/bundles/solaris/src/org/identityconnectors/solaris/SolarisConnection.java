package org.identityconnectors.solaris;

import org.identityconnectors.common.logging.Log;

public class SolarisConnection {
    /**
     * Setup logging for the {@link SolarisConnection}.
     */
    private static final Log log = Log.getLog(SolarisConnection.class);
    
    private SolarisConfiguration config;
    
    /** constructor */
    public SolarisConnection(SolarisConfiguration config) {
        this.config = config;
    }
    
    /** constructor for unit tests */
    SolarisConnection() {
    }
    
    public void connect() {
        
    }
    
    public void test() {
        
    }
    
    public void dispose() {
        
    }
}
