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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * Encapsulates an entry and its attributes.
 * Type of attributes must be {@link org.identityconnectors.solaris.attr.NativeAttribute}.
 * 
 * Contract: internally this class should contain NativeAttributes!
 * @author David Adam
 */
public class SolarisEntry {
    private String name;
    private Set<Attribute> attrSet;
    
    public static class Builder {
        // required
        private String name;
        private Set<Attribute> attrSet;
        
        public Builder(String name) {
            this.name = name;
            attrSet = new HashSet<Attribute>();
        }
        
        /** add a multivalue attr */
        public Builder addAttr(NativeAttribute name, List<Object> values) {
            attrSet.add(AttributeBuilder.build(name.getName(), values));
            return this;
        }
        
        /** add a singlevalue attr */
        public Builder addAttr(NativeAttribute name, String value) {
            attrSet.add(AttributeBuilder.build(name.getName(), value));
            return this;
        }
        
        /**
         * add all attributes to the present SolarisEntry from the given entry.
         * However the {@link SolarisEntry#getName()} is not preserved.
         */
        public Builder addAllAttributesFrom(SolarisEntry entry) {
            Set<Attribute> entries = entry.getAttributeSet();
            for (Attribute attribute : entries) {
                attrSet.add(attribute);
            }
            return this;
        }
        
        public SolarisEntry build() {
            return new SolarisEntry(this);
        }
    }

    /**
     * @param name the userName or groupName (depends on the search context).
     * @param attrSet the attribute set (attribute is a type-value pair).
     */
    private SolarisEntry(Builder bldr) {
        name = bldr.name;
        attrSet = bldr.attrSet;
    }
    
    /**
     * @returns the username that the attributes belong to.
     * 
     *          Note: this attribute can be used to unite attributes from more
     *          Commands (LoginsCmd, AuthsCmd, RolesCmd...).
     */
    public String getName() {
        return name;
    }
    
    public Set<Attribute> getAttributeSet() {
        return attrSet;
    }
    
    @Override
    public String toString() {
        return String.format("<Name: '%s', Attributes: [%s]", name, attrSet.toString());
    }
    
    /**
     * search for attribute in this entry
     * @param attrToFind the attribute that we search for.
     * @return null if attribute was not found in given entry.
     */
    public Attribute searchForAttribute(NativeAttribute attrToFind) {
        for (Attribute attribute : getAttributeSet()) {
            if (attrToFind.getName().equals(attribute.getName())) {
                return attribute;
            }
        }
        return null;
    }
}
