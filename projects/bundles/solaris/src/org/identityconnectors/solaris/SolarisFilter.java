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

/**
 * Solaris filter encapsulates the name of filtered attribute, and the regular
 * expression used for filtering.
 * 
 * @author David Adam
 */
public class SolarisFilter {
    /** name of the attribute that the filter is applied on. */
    private String attributeName;

    /** regular expression */
    private String regExp;

    public SolarisFilter(String attrName, String regExp) {
        this.regExp = regExp;
        attributeName = attrName;
    }

    // TODO Refactor to getName()
    public String getAttributeName() {
        return attributeName;
    }

    public String getRegExp() {
        return regExp;
    }
    
    @Override
    public int hashCode() {
        return (attributeName + regExp).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SolarisFilter) {
            SolarisFilter other = (SolarisFilter) obj;
            if (other.getAttributeName().equals(attributeName)) {
                if (other.getRegExp().equals(regExp)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("SolarisFilter[name = '%s'; regExp = '%s']", getAttributeName(), getRegExp());
    }
}
