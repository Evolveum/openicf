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
 * Enumeration for command line switches parameters used in update scripts.
 * 
 * @author David Adam
 */
public enum UpdateParamsEnum {
    /* **** ACCOUNT **** */
    AUTHORIZATION("A"), 
    GROUP("g"), 
    EXPIRE("e"), 
    INACTIVE("f"), 
    UID("u"), 
    SECONDARY_GROUP("G"), 
    ROLE("R"), 
    COMMENT("c"), 
    SHELL("s"), 
    PROFILE("P"), 
    DIR("d"),
    /* **** GROUP **** */
    //TODO
    UNKNOWN(null);

    private final String commandLineSwitch;

    private UpdateParamsEnum(String commandLineSwitch) {
        this.commandLineSwitch = String.format("-%s", commandLineSwitch);
    }

    @Override
    public String toString() {
        if (commandLineSwitch == null)
            throw new IllegalArgumentException(String.format("No command line switch defined for %s", this));
        return commandLineSwitch;
    }
}
