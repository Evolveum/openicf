package org.identityconnectors.solaris;

import org.junit.Assert;
import org.junit.Test;

public class ConnectionTypeTest {
    @Test
    public void testGoodConversion() {
        Assert.assertTrue(ConnectionType.toConnectionType("telnet").equals(ConnectionType.TELNET));
        Assert.assertTrue(ConnectionType.toConnectionType("TELNET").equals(ConnectionType.TELNET));
        Assert.assertTrue(ConnectionType.toConnectionType("TeLnet").equals(ConnectionType.TELNET));
        Assert.assertTrue(ConnectionType.toConnectionType("sSh").equals(ConnectionType.SSH));
        Assert.assertTrue(ConnectionType.toConnectionType("SSH").equals(ConnectionType.SSH));
        Assert.assertTrue(ConnectionType.toConnectionType("ssh").equals(ConnectionType.SSH));
    }
    
    @Test
    public void testWrongConfiguration() {
        try {
            final String test = "bassh";
            ConnectionType.toConnectionType(test);
            Assert.fail(String.format("Error, accepted: %s", test));
        } catch (RuntimeException ex) {
            //ok
        }
        try {
            final String test = "telnetSSh";
            ConnectionType.toConnectionType(test);
            Assert.fail(String.format("Error, accepted: %s", test));
        } catch (RuntimeException ex) {
            //ok
        }
    }
}
