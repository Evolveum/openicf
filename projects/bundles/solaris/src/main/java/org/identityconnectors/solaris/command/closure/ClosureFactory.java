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
package org.identityconnectors.solaris.command.closure;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.solaris.command.MatchBuilder;

import expect4j.Closure;
import expect4j.ExpectState;

/**
 * Factory for making Closures. The concept is that a closure represents a call
 * back method that is a reaction to certain matches in output text from the
 * Solaris resource. <br>
 * Example [executing 'useradd' command]:
 * <ul>
 * <li>create a Match[] list using {@link MatchBuilder#build()}.</li>
 * <li>Each match contains a matcher that triggers the run() method on the
 * {@link Closure}.</li>
 * </ul>
 * 
 * @author David Adam
 */
public abstract class ClosureFactory implements Closure {
    protected String msg;
    
    private ClosureFactory(String message) {
        msg = message;
    }
    
    public abstract void run(ExpectState state) throws Exception;
    
    /** throws a {@link ConnectorException} when the callback run() is executed. */
    static class ConnectorExceptionClosure extends ClosureFactory {
        public ConnectorExceptionClosure(String message) {
            super(message);
        }

        @Override
        public void run(ExpectState state) throws Exception {
            throw new ConnectorException(String.format("%s\nBUFFER CONTENT:\n%s", super.msg, state.getBuffer()));
        }
    }
    
    /** throws a {@link UnknownUidException} when the callback run() is executed. */
    static class UnknownUidExceptionClosure extends ClosureFactory {
        public UnknownUidExceptionClosure(String message) {
            super(message);
        }

        @Override
        public void run(ExpectState state) throws Exception {
            throw new UnknownUidException(String.format("%s\nBUFFER CONTENT:\n%s", super.msg, state.getBuffer()));
        }
    }
    
    /** idle call that is doing nothing in callback method. */
    static class NullClosure implements Closure {
        public void run(ExpectState state) throws Exception {
            //Nothing here, on purpose.
        }
    }
    
    public static Closure newUnknownUidException(String msg) {
        return new UnknownUidExceptionClosure(msg);
    }
    
    public static Closure newConnectorException(String msg) {
        return new ConnectorExceptionClosure(msg);
    }
    
    public static Closure newNullClosure() {
        return new NullClosure();
    }
}
