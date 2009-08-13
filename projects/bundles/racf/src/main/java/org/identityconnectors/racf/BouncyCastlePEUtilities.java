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

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

public class BouncyCastlePEUtilities extends RACFPasswordEnvelopeUtilities {

    private BouncyCastleProvider _provider;
    private boolean _inited = false;

    public BouncyCastlePEUtilities(String certificateString, String privateKeyString) {
        super(certificateString, privateKeyString);
        init();
    }

    private void init() {
        _provider = new BouncyCastleProvider();
        Security.addProvider(_provider);
        _inited = true;
    }

    protected PrivateKey decodePrivateKey(String privateKeyString) {
        PEMReader reader = null;
        if (!_inited)
            init();
        try {
            reader = new PEMReader(new StringReader(privateKeyString));
            return ((KeyPair) reader.readObject()).getPrivate();
        } catch (IOException e) {
            throw ConnectorException.wrap(e);
        } finally {
            try { reader.close(); } catch (Exception e) {}
        }
    }

    protected X509Certificate decodeX509Certificate(String certificateString) {
        PEMReader reader = null;
        if (!_inited)
            init();
        try {
            reader = new PEMReader(new StringReader(certificateString));
            return (X509Certificate) reader.readObject();
        } catch (IOException e) {
            throw ConnectorException.wrap(e);
        } finally {
            try { reader.close(); } catch (Exception e) {}
        } 
    }

    private RecipientId getRecipientId(X509Certificate certificate) throws IOException {
        RecipientId recId = new RecipientId();
        recId.setSerialNumber(certificate.getSerialNumber());
        recId.setIssuer(certificate.getIssuerX500Principal().getEncoded());

        return recId;
    }

    protected byte[] decrypt(byte[] encrypted, X509Certificate certificate, PrivateKey key) {
        byte[] decrypted;
        try {
            CMSEnvelopedData enveloped = new CMSEnvelopedData(encrypted);

            RecipientInformationStore recipients = enveloped.getRecipientInfos();
            RecipientInformation recipient = recipients.get(getRecipientId(getCertificate()));

            decrypted = recipient.getContent(getPrivateKey(),"BC");
        } catch (NoSuchProviderException e) {
            throw ConnectorException.wrap(e);
        } catch (IOException e) {
            throw ConnectorException.wrap(e);
        } catch (CMSException e) {
            throw ConnectorException.wrap(e);
        }

        return decrypted;
    }

    public String getPassword(byte[] envelope) {
        ASN1InputStream aIn = null;
        try {
            aIn = new ASN1InputStream(envelope);
            Object o = null;
            DEROctetString oString = null;

            while ( (o = aIn.readObject()) != null ) {
                if ( o instanceof DERSequence ) {

                    // identifier (1.2.840.113549.1.7.1)
                    DERSequence seq = (DERSequence) o;
                    if (seq.size() >= 2 &&
                            seq.getObjectAt(0) instanceof DERObjectIdentifier &&
                            "1.2.840.113549.1.7.1".equals(((DERObjectIdentifier) seq.getObjectAt(0)).getId()) ) {

                        if (seq.getObjectAt(1) instanceof DERTaggedObject &&
                                ((DERTaggedObject) seq.getObjectAt(1)).getObject() instanceof DEROctetString ) {

                            oString = (DEROctetString) ((DERTaggedObject) seq.getObjectAt(1)).getObject();
                            break;
                        }
                    }
                }
            }
            aIn.close();
            aIn = null;
            String pw = null;
            if ( oString != null ) {
                aIn = new ASN1InputStream(oString.getOctets());
                DERSequence seq = (DERSequence) aIn.readObject();
                if ( seq.getObjectAt(2) instanceof DERUTF8String ) {
                    pw = ((DERUTF8String) seq.getObjectAt(2)).getString();
                }
                aIn.close();
                aIn = null;
            }
            return pw;
        } catch (IOException e) {
            try {
                if (aIn!=null)
                    aIn.close();
            } catch (IOException e2) {
            }
            throw ConnectorException.wrap(e);
        }
    }

    protected Provider getProvider() {
        return _provider;
    }
}

