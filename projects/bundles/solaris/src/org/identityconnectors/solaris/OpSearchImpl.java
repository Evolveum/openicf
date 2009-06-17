package org.identityconnectors.solaris;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;


public class OpSearchImpl extends AbstractOp {
    
    public OpSearchImpl(SolarisConfiguration configuration,
            SolarisConnection connection, Log log) {
        super(configuration, connection, log);
    }
    
    public void executeQuery(ObjectClass oclass, SolarisFilter query,
            ResultsHandler handler, OperationOptions options) {
        throw new UnsupportedOperationException();
//      //evaluate if the user exists
//        final String command = "cut -d: -f1 /etc/passwd | grep -v \"^[+-]\"";
//        output = executeCommand(command);
    }
}
