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
 * abstraction of all Solaris Object type attributes (including GROUP, ACCOUNT).
 * @author David Adam
 */
public interface SolarisAttribute {
    /** @return the name of the GROUP attribute */
    public abstract String getName();

    /**
     * get the command to acquire the Attribute. Executing this command tends to
     * return a CSV like text structure, that fruther needed to be parsed by
     * some regular expression, {@link SolarisAttribute#getRegEx()}
     */
    public abstract String getCommand();

    /**
     * the regular expression that is used for parsing the result of the
     * command, {@link SolarisAttribute#getCommand()}. It extracts the Uid and the 
     * filtered Attribute in this particular order.
     */
    public abstract String getRegExpForUidAndAttribute();
}