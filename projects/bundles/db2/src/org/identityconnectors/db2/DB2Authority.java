/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.db2;

/**
 *  Utility class for representing a DB2 authority, including
 *  table type, specific function of permission, object to
 *  apply permission to and user name.
 */
class DB2Authority {
    /**
     *  Constructor.
     */
    DB2Authority(String authorityType,
        String authorityFunction, String authorityObject,
        String userName)
    {
        this.authorityType = authorityType;
        this.authorityFunction = authorityFunction;
        this.authorityObject = authorityObject;
        this.userName = userName;
    }
    final String authorityType;
    final String authorityFunction;
    final String authorityObject;
    final String userName;

    public String toString() {
        return "{DB2Authority: Type=" + authorityType + ", Function="
            + authorityFunction + ", Object=" + authorityObject
            + ", User=" + userName + "}";
    }
}
