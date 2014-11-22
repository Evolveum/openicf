/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2014 Evolveum All Rights Reserved
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html See the License for the specific
 * language governing permission and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://forgerock.org/license/CDDLv1.0.html If
 * applicable, add the following below the CDDL Header, with the fields enclosed
 * by brackets [] replaced by your own identifying information: " Portions
 * Copyrighted [year] [name of copyright owner]"
 * 
*/
package org.identityconnectors.ldap.search;

import java.io.IOException;

import javax.naming.ldap.BasicControl;

import com.sun.jndi.ldap.Ber;
import com.sun.jndi.ldap.BerEncoder;


/**
 * 
 * Virtual List View control as specified in draft-ietf-ldapext-ldapv3-vlv-09.
 * 
 *  VirtualListViewRequest ::= SEQUENCE {
 *         beforeCount    INTEGER (0..maxInt),
 *         afterCount     INTEGER (0..maxInt),
 *         target       CHOICE {
 *                        byOffset        [0] SEQUENCE {
 *                             offset          INTEGER (1 .. maxInt),
 *                             contentCount    INTEGER (0 .. maxInt) },
 *                        greaterThanOrEqual [1] AssertionValue },
 *         contextID     OCTET STRING OPTIONAL }
 * 
 * Simplistic implementation that only supports byOffset choice.
 * 
 * @author semancik
 *
 */
@SuppressWarnings("restriction")
public class VirtualListViewRequestControl extends BasicControl {	
	private static final long serialVersionUID = 6735555195642409439L;
	
	public static final String OID = "2.16.840.1.113730.3.4.9";
	
	public VirtualListViewRequestControl(int beforeCount, int afterCount, int offset, int contentCount, byte[] contextID, boolean criticality) throws IOException {
		super(OID, criticality, null);
		super.value = setEncodedValue(beforeCount, afterCount, offset, contentCount, contextID);
	}

	private byte[] setEncodedValue(int beforeCount, int afterCount, int offset, int contentCount, byte[] contextID) throws IOException {
		BerEncoder ber = new BerEncoder();
		ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
		ber.encodeInt(beforeCount);
		ber.encodeInt(afterCount);
		ber.beginSeq(Ber.ASN_CONTEXT | Ber.ASN_CONSTRUCTOR | 0);
		ber.encodeInt(offset);
		ber.encodeInt(contentCount);
		ber.endSeq();
	    if (contextID != null) {
			ber.encodeOctetString(contextID, Ber.ASN_OCTET_STR);
	    }
		ber.endSeq();
		return ber.getTrimmedBuf();
	}
	
}
