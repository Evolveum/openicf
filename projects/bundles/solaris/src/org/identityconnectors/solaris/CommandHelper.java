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
package org.identityconnectors.solaris;


public class CommandHelper {
    final static int DEFAULT_LIMIT = 120;
    private static StringBuffer limitString(StringBuffer data, int limit) {
        StringBuffer result = new StringBuffer(limit);
        if (data.length() > limit) {
            result.append(data.substring(0, limit));
            result.append("\\\n"); // /<newline> separator of Unix command line cmds.
            
            final String remainder = data.substring(limit, data.length()); 
            if (remainder.length() > limit) {
                StringBuffer sbtmp = new StringBuffer();
                sbtmp.append(remainder);
                result.append(limitString(sbtmp, limit));
            } else {
                result.append(remainder);
            }
        } else {
            return data;
        }
        return result;
    }

    public static StringBuffer limitString(String data) {
        StringBuffer sb = new StringBuffer();
        sb.append(data);
        return limitString(sb, DEFAULT_LIMIT /* == max length of line from SolarisResourceAdapter#getUpdateNativeUserScript(), line userattribparams */);
    }
}

//while (paramsWork.length() > 0) {
//    if (paramsWork.length() > 120) {
//        usermodParams.append(paramsWork.substring(0, 120));
//        usermodParams.append("\\\n");
//        paramsWork = paramsWork.substring(120, paramsWork.length());
//    } else {
//        usermodParams.append(paramsWork);
//        paramsWork = "";
//    }
//}