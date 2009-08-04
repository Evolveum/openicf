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
package org.identityconnectors.solaris.operation.search;

import java.util.Set;

import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.constants.SolarisAttribute;
/**
 * encapsulates matching of a single attribute by a regular expression.
 * @author David Adam
 */
public abstract class AttributeFilter implements Node {

    private String regex;
    private SolarisAttribute attr;

    /** 
     * @param attributeName the attribute name that is filtered
     * @param regex the regular expression that is used to filter 
     */
    public AttributeFilter(String attributeName, String regex) {
        attr = SolarisUtil.getAttributeBasedOnName(attributeName);
        this.regex = regex;
    }

    public abstract Set<Uid> getSatisfyingUids();
}
