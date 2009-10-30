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
public final class ComponentInterface {
    private final String interfaceName;
    private final String getKey;
    private final String findKey;
    private final String createKey;
    private final DisableRule disableRule;
    private final List<Property> properties;
    private final SupportedObjectTypes supportedObjectTypes;
    
    private ComponentInterface(Builder builder){
        this.interfaceName = builder.getInterfaceName();
        this.getKey = builder.getGetKey();
        this.findKey = builder.getFindKey();
        this.createKey = builder.getCreateKey();
        this.disableRule = builder.getDisableRule();
        this.properties = new ArrayList<Property>(builder.getProperties());
        this.supportedObjectTypes = builder.getSupportedObjectTypes();
    }
    /**
     * @return the interfaceName
     */
    public String getInterfaceName() {
        return interfaceName;
    }
    /**
     * @return the getKey
     */
    public String getGetKey() {
        return getKey;
    }
    /**
     * @return the findKey
     */
    public String getFindKey() {
        return findKey;
    }
    /**
     * @return the createKey
     */
    public String getCreateKey() {
        return createKey;
    }
    /**
     * @return the disableRule
     */
    public DisableRule getDisableRule() {
        return disableRule;
    }
    /**
     * @return the properties
     */
    public List<Property> getProperties() {
        return Collections.unmodifiableList(properties);
    }
    
    public SupportedObjectTypes getSupportedObjectTypes(){
        return supportedObjectTypes;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((createKey == null) ? 0 : createKey.hashCode());
        result = prime * result + ((disableRule == null) ? 0 : disableRule.hashCode());
        result = prime * result + ((findKey == null) ? 0 : findKey.hashCode());
        result = prime * result + ((getKey == null) ? 0 : getKey.hashCode());
        result = prime * result + ((interfaceName == null) ? 0 : interfaceName.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((supportedObjectTypes == null) ? 0 : supportedObjectTypes.hashCode());
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
        ComponentInterface other = (ComponentInterface) obj;
        if (createKey == null) {
            if (other.createKey != null)
                return false;
        } else if (!createKey.equals(other.createKey))
            return false;
        if (disableRule == null) {
            if (other.disableRule != null)
                return false;
        } else if (!disableRule.equals(other.disableRule))
            return false;
        if (findKey == null) {
            if (other.findKey != null)
                return false;    /* (non-Javadoc)
             * @see java.lang.Object#toString()
             */

        } else if (!findKey.equals(other.findKey))
            return false;
        if (getKey == null) {
            if (other.getKey != null)
                return false;
        } else if (!getKey.equals(other.getKey))
            return false;
        if (interfaceName == null) {
            if (other.interfaceName != null)
                return false;
        } else if (!interfaceName.equals(other.interfaceName))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (supportedObjectTypes == null) {
            if (other.supportedObjectTypes != null)
                return false;
        } else if (!supportedObjectTypes.equals(other.supportedObjectTypes))
            return false;
        return true;
    }

    


    @Override
    public String toString() {
        return "ComponentInterface [createKey=" + createKey + ", disableRule=" + disableRule + ", findKey=" + findKey + ", getKey=" + getKey
                + ", interfaceName=" + interfaceName + ", properties=" + properties + ", supportedObjectTypes=" + supportedObjectTypes + "]";
    }




    public final static class Builder{
        private String interfaceName;
        private String getKey;
        private String findKey;
        private String createKey;
        private DisableRule disableRule;
        private List<Property> properties = new ArrayList<Property>(3);
        private SupportedObjectTypes supportedObjectTypes;
        /**
         * @return the interfaceName
         */
        public String getInterfaceName() {
            return interfaceName;
        }
        /**
         * @param interfaceName the interfaceName to set
         * @return 
         */
        public Builder setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }
        /**
         * @return the getKey
         */
        public String getGetKey() {
            return getKey;
        }
        /**
         * @param getKey the getKey to set
         * @return 
         */
        public Builder setGetKey(String getKey) {
            this.getKey = getKey;
            return this;
        }
        /**
         * @return the findKey
         */
        public String getFindKey() {
            return findKey;
        }
        /**
         * @param findKey the findKey to set
         * @return 
         */
        public Builder setFindKey(String findKey) {
            this.findKey = findKey;
            return this;
        }
        /**
         * @return the createKey
         */
        public String getCreateKey() {
            return createKey;
        }
        /**
         * @param createKey the createKey to set
         * @return 
         */
        public Builder setCreateKey(String createKey) {
            this.createKey = createKey;
            return this;
        }
        /**
         * @return the disableRule
         */
        public DisableRule getDisableRule() {
            return disableRule;
        }
        /**
         * @param disableRule the disableRule to set
         * @return 
         */
        public Builder setDisableRule(DisableRule disableRule) {
            this.disableRule = disableRule;
            return this;
        }
        /**
         * @return the properties
         */
        public List<Property> getProperties() {
            return Collections.unmodifiableList(properties);
        }
        /**
         * @param properties the properties to set
         * @return 
         */
        public Builder addProperties(List<Property> properties) {
            this.properties.addAll(properties);
            return this;
        }
        
        public Builder addProperty(Property property) {
            this.properties.add(property);
            return this;
        }
        /**
         * @return the supportedObjectTypes
         */
        public SupportedObjectTypes getSupportedObjectTypes() {
            return supportedObjectTypes;
        }
        /**
         * @param supportedObjectTypes the supportedObjectTypes to set
         */
        public Builder setSupportedObjectTypes(SupportedObjectTypes supportedObjectTypes) {
            this.supportedObjectTypes = supportedObjectTypes;
            return this;
        }
        
        public ComponentInterface build(){
            return new ComponentInterface(this);
        }
        
        
    }

}
