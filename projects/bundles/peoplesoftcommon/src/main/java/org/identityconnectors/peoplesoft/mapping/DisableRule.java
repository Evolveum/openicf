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

package org.identityconnectors.peoplesoft.mapping;

/**
 * @author kitko
 *
 */
public final class DisableRule {
    private final String property;
    private final String trueValue;
    private final String falseValue;
    
    private DisableRule(Builder builder) {
        super();
        this.property = builder.getName();
        this.trueValue = builder.getTrueValue();
        this.falseValue = builder.getFalseValue();
    }

    /**
     * @return the name
     */
    public String getName() {
        return property;
    }

    /**
     * @return the trueValue
     */
    public String getTrueValue() {
        return trueValue;
    }

    /**
     * @return the falseValue
     */
    public String getFalseValue() {
        return falseValue;
    }
    
    @Override
    public String toString() {
        return "DisableRule [name=" + property + ", trueValue=" + trueValue + ", falseValue=" + falseValue + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((falseValue == null) ? 0 : falseValue.hashCode());
        result = prime * result + ((property == null) ? 0 : property.hashCode());
        result = prime * result + ((trueValue == null) ? 0 : trueValue.hashCode());
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
        DisableRule other = (DisableRule) obj;
        if (falseValue == null) {
            if (other.falseValue != null)
                return false;
        } else if (!falseValue.equals(other.falseValue))
            return false;
        if (property == null) {
            if (other.property != null)
                return false;
        } else if (!property.equals(other.property))
            return false;
        if (trueValue == null) {
            if (other.trueValue != null)
                return false;
        } else if (!trueValue.equals(other.trueValue))
            return false;
        return true;
    }



    public static final class Builder {
        private String name;
        private String trueValue;
        private String falseValue;
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
         * @return the trueValue
         */
        public String getTrueValue() {
            return trueValue;
        }
        /**
         * @param trueValue the trueValue to set
         */
        public Builder setTrueValue(String trueValue) {
            this.trueValue = trueValue;
            return this;
        }
        /**
         * @return the falseValue
         */
        public String getFalseValue() {
            return falseValue;
        }
        /**
         * @param falseValue the falseValue to set
         */
        public Builder setFalseValue(String falseValue) {
            this.falseValue = falseValue;
            return this;
        }
        
        public DisableRule build(){
            return new DisableRule(this);
        }
        
        
    }
    
    
    
}
