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
package org.identityconnectors.racf;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;

import org.identityconnectors.framework.common.exceptions.ConnectorException;

public abstract class RACFPasswordEnvelopeUtilities {

    private X509Certificate certificate = null;
    private static final String DEFAULT_UTILITY = "org.identityconnectors.racf.BouncyCastlePEUtilities";

    protected X509Certificate getCertificate() {
        return certificate;
    }

    protected void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    private PrivateKey privateKey = null;

    protected PrivateKey getPrivateKey() {
        return privateKey;
    }

    protected void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    protected RACFPasswordEnvelopeUtilities(String certificateString, String privateKeyString) {
        setCertificate(decodeX509Certificate(certificateString));
        setPrivateKey(decodePrivateKey(privateKeyString));
    }

    public static RACFPasswordEnvelopeUtilities newRACFPasswordEnvelopeDecryptor(String className, String certificateString, String privateKeyString) {
        try {
            if (className==null)
                className = DEFAULT_UTILITY;
            Class clazz = Class.forName(className);
            Constructor constructor = clazz.getConstructor(new Class[] { String.class, String.class });
            return (RACFPasswordEnvelopeUtilities)constructor.newInstance((Object[])new String[] { certificateString, privateKeyString });
        } catch (SecurityException e) {
            throw ConnectorException.wrap(e);
        } catch (IllegalArgumentException e) {
            throw ConnectorException.wrap(e);
        } catch (ClassNotFoundException e) {
            throw ConnectorException.wrap(e);
        } catch (NoSuchMethodException e) {
            throw ConnectorException.wrap(e);
        } catch (InstantiationException e) {
            throw ConnectorException.wrap(e);
        } catch (IllegalAccessException e) {
            throw ConnectorException.wrap(e);
        } catch (InvocationTargetException e) {
            throw ConnectorException.wrap(e);
        }
    }

    public byte[] decrypt(byte[] encrypted) {
        return decrypt(encrypted, getCertificate(), getPrivateKey());
    }

    // The methods deal with the actual encrypted items
    //

    protected abstract Provider getProvider();

    protected abstract PrivateKey decodePrivateKey(String privateKeyString);

    protected abstract X509Certificate decodeX509Certificate(String certificateString);

    protected abstract byte[] decrypt(byte[] data, X509Certificate certificate, PrivateKey key);

    public abstract String getPassword(byte[] encrypted);

}

