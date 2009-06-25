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
package org.identityconnectors.solaris.constants;


public enum SolarisGroupAttrsEnum {
    /*
     * CONSTRAINT (for adding enums):
     * the Enum items name must be the uppercase version of the actual
     * attribute name.
     */
 // TODO add command line switches that are used for altering these
 // attributes
    GROUPNAME("groupName", UpdateParamsEnum.UNKNOWN),
    GID("gid", UpdateParamsEnum.UNKNOWN),
    USERS("users", UpdateParamsEnum.UNKNOWN);

    /** name of the attribute, it comes from the adapter's schema (prototype xml) */
    private String attrName;
    private UpdateParamsEnum cmdSwitch;
    
    private SolarisGroupAttrsEnum(String attrName, UpdateParamsEnum cmdSwitch) {
        this.attrName = attrName;
        this.cmdSwitch = cmdSwitch;
    }
    
    /**
     * @throws IllegalArgumentException in case there doesn't exists an assigned enum constant
     */
    public static SolarisGroupAttrsEnum toEnum(String s) throws IllegalArgumentException {
        return SolarisGroupAttrsEnum.valueOf(s.toUpperCase());
    }
    
    @Override
    public String toString() {
        return attrName;
    }
    
    /** generates the command line switch for the attribute */
    public String getCmdSwitch() {
        return cmdSwitch.toString();
    }
}
