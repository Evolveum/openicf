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
package org.identityconnectors.solaris.command;

import org.identityconnectors.framework.common.exceptions.ConnectorException;

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
    private ClosureFactory() {
    }
    
    public abstract void run(ExpectState state) throws Exception;
    
    /** throws a {@link ConnectorException} when the callback run() is executed. */
    public static class ConnectorExceptionClosure extends ClosureFactory {
        
        private String errMsg;
        private boolean isReject = false;
        
        private ConnectorExceptionClosure() {
            // empty on purpose
        }

        @Override
        public void run(ExpectState state) throws Exception {
            errMsg = state.getBuffer();
            isReject = true;
        }
        
        public String getErrMsg() {
            return errMsg;
        }
        public boolean isMatched() {
            return isReject;
        }
    }
    
    /** idle call that is doing nothing in callback method. */
    public static class CaptureClosure implements Closure {
        private String msg;
        private boolean matched = false;
        
        private CaptureClosure() {
            // empty on purpose
        }
        
        public void run(ExpectState state) throws Exception {
            this.msg = state.getBuffer();
            matched = true;
        }
        
        public String getMsg() {
            return msg;
        }
        
        public boolean isMatched() {
            return matched;
        }
    }
    
    public static class TimeoutClosure implements Closure {
        private String msg;
        private String buffer;
        private boolean matched = false;
        
        private TimeoutClosure(String msg) {
            this.msg = msg;
        }
        public void run(ExpectState state) throws Exception {
            buffer = state.getBuffer();
            matched = true;
        }
        
        public boolean isMatched() {
            return matched;
        }
        
        public String getErrMsg() {
            return String.format("%s buffer: <%s>", msg, buffer);
        }
    }
    
    public static ConnectorExceptionClosure newConnectorException() {
        return new ConnectorExceptionClosure();
    }
    
    public static CaptureClosure newCaptureClosure() {
        return new CaptureClosure();
    }

    /**
     * @param msg the given message is appended when timeout occurs
     */
    public static TimeoutClosure newTimeoutClosure(String msg) {
        return new TimeoutClosure(msg);
    }
}
