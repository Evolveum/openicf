/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.rw3270.wrq;

import java.util.Properties;

import javax.naming.NamingException;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.rw3270.PoolableConnectionConfiguration;
import org.identityconnectors.rw3270.RW3270BaseConnection;

import com.wrq.eNetwork.ECL.ECLErr;
import com.wrq.eNetwork.ECL.ECLOIA;
import com.wrq.eNetwork.ECL.ECLPS;
import com.wrq.eNetwork.ECL.ECLSession;
import com.wrq.eNetwork.ECL.event.ECLPSEvent;
import com.wrq.eNetwork.ECL.event.ECLPSListener;

public class WrqConnection extends RW3270BaseConnection implements ECLPSListener {
    private ECLPS                     _ps;
    private ECLSession                _session;
    private ECLOIA                    _oia;

    public WrqConnection(PoolableConnectionConfiguration config) throws NamingException {
        super(config);
    }

    @Override
    protected void clearAndUnlock() throws InterruptedException {
        _session.transmitTerminalKey(33);
        waitForUnlock();
    }

    @Override
    protected String getDisplay() {
        return _ps.GetString().getDataString();
    }

    @Override
    protected void sendEnter() {
        try {
            _ps.SendKeys("[enter]");
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    protected void sendKeys(String keys) {
        try {
            _ps.SendKeys(keys);
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    protected void sendPAKeys(int pa) {
        try {
            _ps.SendKeys("[pa"+pa+"]");
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    protected void sendPFKeys(int pf) {
        try {
            _ps.SendKeys("[pf"+pf+"]");
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    protected void setCursorPos(short pos) {
        try {
            _ps.SetCursorPos(pos);
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    protected void waitForUnlock() throws InterruptedException {
        while (_oia.InputInhibited()!=ECLOIA.INHIBIT_NOTINHIBITED) {
            Thread.sleep(100);
        }
    }

    public void connect() {
        Properties p = new Properties();
        p.put(ECLSession.SESSION_HOST, _config.getHostNameOrIpAddr()+":"+_config.getHostTelnetPortNumber());
        p.put(ECLSession.SESSION_OWNER_IS_APPLICATION, ECLSession.SESSION_ON);
        p.put("SESSION_QUIETMODE", "true");
        p.put(ECLSession.SESSION_TYPE, ECLSession.SESSION_TYPE_3270_STR);
        p.put(ECLSession.SESSION_USE_CLASSPATH, ECLSession.SESSION_ON);
        p.put(ECLSession.SESSION_TERMINAL_MODEL, _model+"");
        try {
            _session = new ECLSession(p);
        } catch (ECLErr e) {
            throw new ConnectorException(e);
        }
        _session.StartCommunication();

        if (!((ECLSession) _session).IsCommStarted()) {
            // Sleep a bit to wait for error notification.
            //
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            throw new ConnectorException(_config.getConnectorMessages().format("NoConnection", "No connection to host:{0}", _lastConnError));
        }

        try {
            _ps = _session.GetPS();
            _ps.RegisterPSEvent(this);
            _oia = _session.GetOIA();
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    public void dispose() {
        _session.dispose();
        _session = null;
    }

    public String getStandardOutput() {
        return _buffer.toString()+_ps.GetString().getDataString();
    }

    public void resetStandardOutput() {
        _buffer.setLength(0);
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
