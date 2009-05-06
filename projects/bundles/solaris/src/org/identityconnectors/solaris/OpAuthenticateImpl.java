package org.identityconnectors.solaris;

import static org.identityconnectors.solaris.SolarisHelper.executeCommand;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;

public class OpAuthenticateImpl extends AbstractOp {

    public OpAuthenticateImpl(SolarisConfiguration configuration, SolarisConnection connection, Log log) {
        super(configuration, connection, log);
    }

    public Uid authenticate(ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options) {
        SolarisConfiguration userConfig = getConfiguration();
        userConfig.setUserName(username);
        userConfig.setPassword(password);
        
        SolarisConnection connection = null;
        try {
            connection = new SolarisConnection(userConfig);
        } catch (RuntimeException ex) {
            getLog().warn("Failed to authenticate user ''{0}'' RuntimeException thrown during authentication.", username);
            // in case of invalid credentials propagate the exception
            throw ex;
        } finally {
            if (connection != null) {
                connection.dispose();
            }
        }
        
        getLog().info("User ''{0}'' succesfully authenticated", username);
        return new Uid(username);
    }

}
