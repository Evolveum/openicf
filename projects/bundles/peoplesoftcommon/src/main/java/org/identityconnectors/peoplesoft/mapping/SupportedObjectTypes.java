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

import java.util.*;

/**
 * @author kitko
 *
 */
public final class SupportedObjectTypes {
    
    public final static class Feautures{
        private final List<String> features ;
        private Feautures(Builder builder){
            features = new ArrayList<String>(builder.features);
        }
        
        public List<String> getFeautures(){
            return Collections.unmodifiableList(features);
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((features == null) ? 0 : features.hashCode());
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
            Feautures other = (Feautures) obj;
            if (features == null) {
                if (other.features != null)
                    return false;
            } else if (!features.equals(other.features))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Feautures [features=" + features + "]";
        }



        public static final class Builder{
            private final List<String> features = new ArrayList<String>();
            public List<String> getFeautues(){
                return Collections.unmodifiableList(features);
            }
            public Builder addFeature(String feature){
                this.features.add(feature);
                return this;
            }
            public Feautures build(){
                return new Feautures(this);
            }
        }
    }
    private final Map<String, Feautures> features;
    
    private SupportedObjectTypes(Builder builder){
        this.features = new LinkedHashMap<String, Feautures>(builder.features);
    }
    
    public Collection<String> getObjectTypes(){
        return Collections.unmodifiableCollection(features.keySet());
    }
    
    public Feautures getFeautures(String objectType){
        return features.get(objectType);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((features == null) ? 0 : features.hashCode());
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
        SupportedObjectTypes other = (SupportedObjectTypes) obj;
        if (features == null) {
            if (other.features != null)
                return false;
        } else if (!features.equals(other.features))
            return false;
        return true;
    }
    
    
    @Override
    public String toString() {
        return "SupportedObjectTypes [features=" + features + "]";
    }





    public static final class Builder{
        private final Map<String, Feautures> features = new LinkedHashMap<String, Feautures>();
        public Builder addFeautures(String objectType, Feautures feautures){
            this.features.put(objectType, feautures);
            return this;
        }
        public SupportedObjectTypes build(){
            return new SupportedObjectTypes(this);
        }
    }
    
    
    
    
}
