/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.rw3270.hod;

import java.util.Properties;

import javax.naming.NamingException;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.rw3270.RW3270Configuration;
import org.identityconnectors.rw3270.RW3270BaseConnection;

import com.ibm.eNetwork.ECL.ECLErr;
import com.ibm.eNetwork.ECL.ECLOIA;
import com.ibm.eNetwork.ECL.ECLPS;
import com.ibm.eNetwork.ECL.ECLSession;
import com.ibm.eNetwork.ECL.event.ECLPSEvent;
import com.ibm.eNetwork.ECL.event.ECLPSListener;


public class HodConnection extends RW3270BaseConnection implements ECLPSListener {
    private ECLPS                     _ps;
    private ECLSession                _session;
    private ECLOIA                    _oia;

    public HodConnection(RW3270Configuration config) throws NamingException {
        super(config);
    }

    @Override
    public void clearAndUnlock() throws InterruptedException {
        try {
            _ps.SendKeys("[clear]");
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public String getDisplay() {
        return _ps.getString();
    }

    @Override
    public void sendEnter() {
        try {
            _ps.SendKeys("[enter]");
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void sendKeys(String keys) {
        try {
            _ps.SendKeys(keys);
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void sendPAKeys(int pa) {
        try {
            _ps.SendKeys("[pa"+pa+"]");
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void sendPFKeys(int pf) {
        try {
            _ps.SendKeys("[pf"+pf+"]");
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void setCursorPos(short pos) {
        try {
            _ps.SetCursorPos(pos);
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    public void waitForUnlock() throws InterruptedException {
        while (_oia.InputInhibited()!=ECLOIA.INHIBIT_NOTINHIBITED) {
            Thread.sleep(100);
        }
    }

    public void connect() {
        Properties p = new Properties(asProperties(_config.getConnectionProperties()));
        p.put(ECLSession.SESSION_HOST, _config.getHostNameOrIpAddr());
        p.put(ECLSession.SESSION_HOST_PORT, _config.getHostTelnetPortNumber());
        p.put("SESSION_QUIETMODE", "true");

        try {
            _session = new ECLSession(p);
            _session.StartCommunication();
    
            if (!_session.IsCommStarted()) {
                // Sleep a bit to wait for error notification.
                //
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                throw new ConnectorException(_config.getConnectorMessages().format("NoConnection", "No connection to host:{0}", _lastConnError));
            }

            _ps = _session.GetPS();
            _ps.RegisterPSEvent(this);
            _oia = _session.GetOIA();

        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    public void dispose() {
        if (_session==null)
            return;
        try {
            logoutUser();
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        } finally {
            _session.dispose();
            _session = null;
        }
    }

    public String getStandardOutput() {
        return _buffer.toString();//+_ps.getString();
    }

    public void test() {
        // TODO Auto-generated method stub
    }

    public void PSNotifyError(ECLPS ps, ECLErr error) {
        _lastConnError = error.GetMsgText();
    }

    public void PSNotifyEvent(ECLPSEvent event) {
        if (event.GetType()==ECLPS.HOST_EVENTS)
            _semaphore.release();
    }

    public void PSNotifyStop(ECLPS event, int arg1) {
    }
}
