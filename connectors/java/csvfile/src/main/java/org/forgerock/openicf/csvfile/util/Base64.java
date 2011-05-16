/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */
package org.forgerock.openicf.csvfile.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.identityconnectors.common.logging.Log;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 *
 * @author lazyman
 */
public class Base64 {

    private static final Log log = Log.getLog(Base64.class);

    public static String encode(byte[] data) {
        if (data == null) {
            return null;
        }

        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }

    public static String encode(String data) {
        if (data == null) {
            return null;
        }

        try {
            return encode(data.getBytes("utf-8"));
        } catch (UnsupportedEncodingException ex) {
            log.error("Unsupported encoding exception, reason: " + ex.getMessage());
        }

        return null;
    }

    public static byte[] decode(String encdata) {
        if (encdata == null) {
            return null;
        }

        try {
            BASE64Decoder decoder = new BASE64Decoder();
            return decoder.decodeBuffer(encdata);
        } catch (IOException ex) {
            log.error("Unknown error occured, reason: " + ex.getMessage());
        }

        return null;
    }
}
