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
public final class ComponentInterfaces {
    private final Map<String,ComponentInterface> interfaces;
    
    private ComponentInterfaces(Builder builder){
        this.interfaces = new LinkedHashMap<String, ComponentInterface>(builder.interfaces);
    }
    
    public Collection<String> getInterfaceNames(){
        return interfaces.keySet();
    }
    
    public ComponentInterface getInterface(String name){
        return interfaces.get(name);
    }
    
    public final static class Builder{
        private final Map<String,ComponentInterface> interfaces = new LinkedHashMap<String,ComponentInterface>(3);
        
        public Map<String,ComponentInterface> getInterfaces(){
            return Collections.unmodifiableMap(interfaces);
        }
        
        public Builder registerInterface(String name, ComponentInterface compInterface){
            this.interfaces.put(name, compInterface);
            return this;
        }
        
        public ComponentInterfaces build(){
            return new ComponentInterfaces(this);
        }
    }
}
