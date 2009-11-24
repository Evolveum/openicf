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

package org.identityconnectors.peoplesoft.wrappers;

import java.io.*;

import org.identityconnectors.common.*;
import org.identityconnectors.framework.common.exceptions.*;

import psft.pt8.joa.*;

/**
 * @author kitko
 *
 */
public final class SessionWrapper implements ISession{
    private final ISession delegate;
    private final static String LINE_SEPARATOR = System.getProperty("line.separator");
    
    public SessionWrapper(ISession delegate){
        this.delegate = delegate;
    }

    public boolean connect(long paramLong, String paramString1, String paramString2, String paramString3, byte[] paramArrayOfByte) {
        boolean result = delegate.connect(paramLong, paramString1, paramString2, paramString3, paramArrayOfByte);
        if(!result){
            psft.pt8.joa.IPSMessageCollection msgs = getPSMessages();
            if (msgs != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Cannot connect to peoplesoft : ");
                for ( int i=0; i<msgs.getCount(); i++) {
                    if (i>0) sb.append(", ");
                    IPSMessage msg = msgs.next();
                    sb.append(msg.getText());
                    if(StringUtil.isNotEmpty(msg.getSource())){
                        sb.append(LINE_SEPARATOR);
                        sb.append("Source :").append(msg.getSource());
                    }
                    if(StringUtil.isNotEmpty(msg.getExplainText())){
                        sb.append(LINE_SEPARATOR);
                        sb.append("ExplainText :").append(msg.getExplainText());
                    }
                }
                throw new ConnectorException(sb.toString());
            }
        }
        return result;
    }

    public boolean connectUsingCountryCd(long paramLong, String paramString1, String paramString2, String paramString3, String paramString4,
            byte[] paramArrayOfByte) {
        return delegate.connectUsingCountryCd(paramLong, paramString1, paramString2, paramString3, paramString4, paramArrayOfByte);
    }

    public boolean disconnect() {
        return delegate.disconnect();
    }

    public String getClassName() {
        return delegate.getClassName();
    }

    public Object getCompIntfc(String paramString) throws JOAException {
        return delegate.getCompIntfc(paramString);
    }

    public Object getComponent(String paramString) throws JOAException {
        return delegate.getComponent(paramString);
    }

    public boolean getErrorPending() {
        return delegate.getErrorPending();
    }

    public INamespace getNamespace(String paramString) throws JOAException, IOException {
        return delegate.getNamespace(paramString);
    }

    public String getNamespaceName() {
        return delegate.getNamespaceName();
    }

    public Object getProperty(String paramString) throws JOAException {
        return delegate.getProperty(paramString);
    }

    public IPSMessageCollection getPSMessages() {
        return delegate.getPSMessages();
    }

    public IRegionalSettings getRegionalSettings() throws JOAException {
        return delegate.getRegionalSettings();
    }

    public boolean getSuspendFormatting() {
        return delegate.getSuspendFormatting();
    }

    public boolean getWarningPending() {
        return delegate.getWarningPending();
    }

    public Object invokeMethod(String paramString, Object[] paramArrayOfObject) throws JOAException {
        return delegate.invokeMethod(paramString, paramArrayOfObject);
    }

    public String sendSynchronizationRequest(String paramString) throws JOAException, IOException {
        return delegate.sendSynchronizationRequest(paramString);
    }

    public void setProperty(String paramString, Object paramObject) throws JOAException {
        delegate.setProperty(paramString, paramObject);
    }

    public void setSuspendFormatting(boolean paramBoolean) {
        delegate.setSuspendFormatting(paramBoolean);
    }
}
