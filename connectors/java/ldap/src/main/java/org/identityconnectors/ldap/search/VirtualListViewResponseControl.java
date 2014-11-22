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
import com.sun.jndi.ldap.BerDecoder;


/**
 * 
 * Virtual List View control as specified in draft-ietf-ldapext-ldapv3-vlv-09.
 * 
 *  VirtualListViewResponse ::= SEQUENCE {
 *         targetPosition    INTEGER (0 .. maxInt),
 *         contentCount     INTEGER (0 .. maxInt),
 *         virtualListViewResult ENUMERATED {
 *              success (0),
 *              operationsError (1),
 *              protocolError (3),
 *              unwillingToPerform (53),
 *              insufficientAccessRights (50),
 *              timeLimitExceeded (3),
 *              adminLimitExceeded (11),
 *              innapropriateMatching (18),
 *              sortControlMissing (60),
 *              offsetRangeError (61),
 *              other(80),
 *              ... },
 *         contextID     OCTET STRING OPTIONAL }
 * 
 * @author semancik
 *
 */
@SuppressWarnings("restriction")
public class VirtualListViewResponseControl extends BasicControl {	
	private static final long serialVersionUID = 7697265925585176447L;

	public static final String OID = "2.16.840.1.113730.3.4.10";
	
	private int targetPosition;
	private int contentCount;
	private int virtualListViewResult;
	private byte[] contextID;
	
	public VirtualListViewResponseControl(String oid, boolean criticality, byte[] value) throws IOException {
		super(oid, criticality, value);
		decode(value);
	}
	
	public int getTargetPosition() {
		return targetPosition;
	}

	public int getContentCount() {
		return contentCount;
	}

	public int getVirtualListViewResult() {
		return virtualListViewResult;
	}

	public byte[] getContextID() {
		return contextID;
	}

	private void decode(byte[] value) throws IOException {
		if (value == null || value.length == 0) {
			return;
		}
		BerDecoder ber = new BerDecoder(value, 0, value.length);
		ber.parseSeq(null);
		targetPosition = ber.parseInt();
		contentCount = ber.parseInt();
		virtualListViewResult = ber.parseEnumeration();
		if ((ber.bytesLeft() > 0) && (ber.peekByte() == Ber.ASN_OCTET_STR)){
			contextID = ber.parseOctetString(Ber.ASN_OCTET_STR, null);
		}
	}
	
}
