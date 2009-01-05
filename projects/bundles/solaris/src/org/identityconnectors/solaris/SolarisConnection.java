package org.identityconnectors.solaris;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.vms.GuardedStringAccessor;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SolarisConnection {
    private Session _session = null;
    private Channel _channel = null;
    private boolean connected = false;

    /**
     * Setup logging for the {@link SolarisConnection}.
     */
    private static final Log log = Log.getLog(SolarisConnection.class);

    private SolarisConfiguration config;
    private InputStream _in;
    private OutputStream _out;

    /** constructor */
    public SolarisConnection(SolarisConfiguration config) {
        this.config = config;
    }

    public void test() {
        connect();
        disconnect();
    }

    public void connect() {
        // test if it isConnected()
        if (!isConnected()) {
            try {
                _session = openSession();
                _channel = _session.openChannel("shell");
                _in = _channel.getInputStream();
                _out = _channel.getOutputStream();
                _channel.connect();
                setConnected(true);
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Disconnect from SSH server. Just closes the streams.
     */
    public void disconnect() {
        if (_channel != null) {
            _channel.disconnect();
        }
        if (_session != null) {
            _session.disconnect();
        }
    }

    private Session openSession() throws JSchException, ConnectorException,
            IOException {

        //TODO this line makes the connection ignore the fingerprint
        // in production version fingerprint should be considered
        JSch.setConfig("StrictHostKeyChecking", "no");
        
        JSch jsch = new JSch();
        Session session = jsch.getSession(config.getUserName(), config
                .getHostNameOrIpAddr(), Integer.parseInt(config.getPort()));
        session.setPassword(getPassword(config));
        session.connect();
        return session;
    }

    private String getPassword(SolarisConfiguration config2) {
        GuardedString pass = config2.getPassword();
        GuardedStringAccessor gsa = new GuardedStringAccessor();
        pass.access(gsa);
        char[] cleanPasswd = gsa.getArray();
        return new String(cleanPasswd);
    }

    // public void dispose() {
    //
    // }

    /* ***************** GET/SET *********************** */
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
