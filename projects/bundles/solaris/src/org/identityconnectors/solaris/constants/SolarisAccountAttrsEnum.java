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


/**
 * 
 * @author David Adam
 *
 */
public enum SolarisAccountAttrsEnum {
    /*
     * CONSTRAINT (for adding enums):
     * the Enum items name must be the uppercase version of the actual
     * attribute name.
     */
    /** home directory */
    DIR("dir", UpdateParamsEnum.DIR), 
    SHELL("shell", UpdateParamsEnum.SHELL),
    /** primary group */
    GROUP("group", UpdateParamsEnum.GROUP),
    SECONDARY_GROUP("secondary_group", UpdateParamsEnum.SECONDARY_GROUP),
    UID("uid", UpdateParamsEnum.UID),
    EXPIRE("expire", UpdateParamsEnum.EXPIRE),
    INACTIVE("inactive", UpdateParamsEnum.INACTIVE),
    COMMENT("comment", UpdateParamsEnum.COMMENT),
    TIME_LAST_LOGIN("time_last_login", UpdateParamsEnum.UNKNOWN);
    
    /** name of the attribute, it comes from the adapter's schema (prototype xml) */
    private String attrName;
    private UpdateParamsEnum cmdSwitch;
    
    private SolarisAccountAttrsEnum(String attrName, UpdateParamsEnum cmdSwitch) {
        this.attrName = attrName;
        this.cmdSwitch = cmdSwitch;
    }
    
    /**
     * @throws IllegalArgumentException in case there doesn't exists an assigned enum constant
     */
    public static SolarisAccountAttrsEnum toEnum(String s) throws IllegalArgumentException {
        return SolarisAccountAttrsEnum.valueOf(s.toUpperCase());
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
