package org.identityconnectors.solaris;

import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;

public class OpUpdateImpl extends AbstractOp {

    public OpUpdateImpl(SolarisConfiguration configuration,
            SolarisConnection connection, Log log) {
        super(configuration, connection, log);
    }

    public Uid update(ObjectClass objclass, Uid uid,
            Set<Attribute> replaceAttributes, OperationOptions options) {
        //TODO
        throw new UnsupportedOperationException();
    }
    
    private String executeCommand(String command) {
        return SolarisHelper.executeCommand(getConfiguration(), getConnection(), command);
    }
}
