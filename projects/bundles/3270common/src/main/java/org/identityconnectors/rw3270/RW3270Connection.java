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
package org.identityconnectors.rw3270;

import org.identityconnectors.common.security.GuardedString;

import expect4j.matches.Match;

public interface RW3270Connection {

    public void dispose();

    public void test();

    public void connect();

    public void reset();
    
    public int getWidth();

    public String getStandardOutput();

    public void resetStandardOutput();

    public void send(String command);
    public void sendFromIOPair(String command);

    public void send(GuardedString command);

    public void send(char[] command);

    public String waitForInput();

    public void waitFor(Match[] matches);
    
    public void waitFor(String expression);

    public void waitFor(String part0, String part1);

    public void waitFor(final String expression, int timeOut);

    public void waitFor(final String expression0,
            final String expression1, int timeOut);

    public void sendKeys(String keys);
    public void sendEnter();
    public void sendPAKeys(int pa);
    public void sendPFKeys(int pf);
    public void setCursorPos(short pos);
    public void waitForUnlock() throws InterruptedException ;
    public void clearAndUnlock() throws InterruptedException;
    public String getDisplay();

}
