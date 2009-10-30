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

package org.identityconnectors.peoplesoft.compintfc.mapping;

import java.util.*;

/**
 * @author kitko
 *
 */
public final class CollectionProperty extends Property{
    private final String key;
    private final List<Property> properties;  
    private CollectionProperty(Builder builder){
        super(builder.name);
        this.key = builder.getKey();
        this.properties = new ArrayList<Property>(builder.getProperties());
    }
    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }
    /**
     * @return the properties
     */
    public List<Property> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CollectionProperty other = (CollectionProperty) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }
    
    





    @Override
    public String toString() {
        return "CollectionProperty [name=" + name + ", key=" + key + ", properties=" + properties + "]";
    }







    public final static class Builder{
        private String name;
        private String key;
        private List<Property> properties = new ArrayList<Property>(3);
        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
        /**
         * @param name the name to set
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }
        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }
        /**
         * @param key the key to set
         */
        public Builder setKey(String key) {
            this.key = key;
            return this;
        }
        /**
         * @return the properties
         */
        public List<Property> getProperties() {
            return Collections.unmodifiableList(properties);
        }
        
        public Builder addProperty(Property property){
            this.properties.add(property);
            return this;
        }
        
        public Builder addProperties(List<Property> properties){
            this.properties.addAll(properties);
            return this;
        }
        
        
        public CollectionProperty build(){
            if(name == null){
                throw new IllegalStateException("Name not set");
            }
            if(key == null){
                throw new IllegalStateException("Key not set");
            }
            return new CollectionProperty(this);
        }
        
    }
}
