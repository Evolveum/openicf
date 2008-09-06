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
package org.identityconnectors.rw3270.freehost3270;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.naming.NamingException;

import net.sf.freehost3270.client.IsProtectedException;
import net.sf.freehost3270.client.RW3270;
import net.sf.freehost3270.client.RWTnAction;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.rw3270.PoolableConnectionConfiguration;
import org.identityconnectors.rw3270.RW3270BaseConnection;


public class FH3270Connection extends RW3270BaseConnection {
    private RW3270                    _rw3270;
    private static final short[]      _PFKEYS         = new short[] { 
        0,          RW3270.PF1, RW3270.PF2, RW3270.PF3, RW3270.PF4,
        RW3270.PF5, RW3270.PF6, RW3270.PF7, RW3270.PF8, RW3270.PF9,
        RW3270.PF10,RW3270.PF11,RW3270.PF12,RW3270.PF13,RW3270.PF14,
        RW3270.PF15,RW3270.PF16,RW3270.PF17,RW3270.PF18,RW3270.PF19,
        RW3270.PF20,RW3270.PF21,RW3270.PF22,RW3270.PF23,RW3270.PF24,
    };
    private static final short[]      _PAKEYS          = new short[] { 
        0,          RW3270.PA1, RW3270.PA2, RW3270.PA3, 
    };
    
    public FH3270Connection(PoolableConnectionConfiguration config) throws NamingException {
        super(config);
        _rw3270 = new RW3270(_model, new RWTnAction() {
            public void beep() { }
            public void broadcastMessage(String msg) { }
            public void cursorMove(int oldPos, int newPos) { }
            public void status(int msg) {
                _lastConnError = msg+"";
            }
            public void incomingData() {
                _semaphore.release();
            }
        });
    }

    @Override
    protected void clearAndUnlock() throws InterruptedException {
        _rw3270.clear();
        waitForUnlock();
    }

    @Override
    protected String getDisplay() {
        return new String(_rw3270.getDisplay());
    }

    @Override
    protected void sendEnter() {
        _rw3270.enter();
    }

    @Override
    protected void sendKeys(String keys) {
        try {
            for (int i=0; i<keys.length(); i++) {
                _rw3270.type(keys.charAt(i));
            }
        } catch (IsProtectedException e) {
            throw new ConnectorException(e);
        }
    }

    @Override
    protected void sendPAKeys(int pa) {
        _rw3270.PA(_PAKEYS[pa]);
    }

    @Override
    protected void sendPFKeys(int pf) {
        _rw3270.PF(_PFKEYS[pf]);
    }

    @Override
    protected void setCursorPos(short pos) {
        _rw3270.setCursorPosition(pos);
    }

    @Override
    protected void waitForUnlock() throws InterruptedException {
        while (_rw3270.isKeyboardLocked()) {
            Thread.sleep(100);
        }
    }

    public void connect() {
        try {
            _rw3270.connect(_config.getHostNameOrIpAddr(), _config.getHostTelnetPortNumber());
        } catch (UnknownHostException e) {
            throw new ConnectorException(e);
        } catch (IOException e) {
            throw new ConnectorException(e);
        }
    }

    public void dispose() {
        _rw3270.disconnect();
        _rw3270 = null;
    }

    public String getStandardOutput() {
        return _buffer.toString()+new String(_rw3270.getDisplay());
    }

    public void resetStandardOutput() {
        _buffer.setLength(0);
    }

    public void test() {
        // TODO Auto-generated method stub
    }
}
