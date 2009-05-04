package org.identityconnectors.solaris;

import static org.identityconnectors.solaris.SolarisHelper.executeCommand;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;

public class OpAuthenticateImpl extends AbstractOp {

    public OpAuthenticateImpl(SolarisConfiguration configuration) {
        super(configuration);
    }

    public Uid authenticate(ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options) {
        SolarisConfiguration userConfig = new SolarisConfiguration(getConfiguration());
        userConfig.setUserName(username);
        userConfig.setPassword(password);
        
        SolarisConnection connection = null;
        try {
            connection = new SolarisConnection(userConfig);
        } catch (RuntimeException ex) {
            // in case of invalid credentials propagate the exception
            throw ex;
        } finally {
            if (connection != null) {
                connection.dispose();
            }
        }
        
        return new Uid(username);
    }

}
