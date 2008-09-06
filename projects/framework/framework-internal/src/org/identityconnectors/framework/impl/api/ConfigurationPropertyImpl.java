/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.framework.impl.api;

import java.util.Locale;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.common.objects.ConnectorMessages;


public class ConfigurationPropertyImpl implements ConfigurationProperty {

    // =======================================================================
    // Fields
    // =======================================================================

    
    /**
     * Order the property should be displayed.
     */
    private int _order;
    
    /**
     * Is this a confidential property?
     */
    private boolean _confidential;
    
    /**
     * Unique name of the property.
     */
    private String _name;
    
    /**
     * Help message key.
     */
    private String _helpMessageKey;
    
    /**
     * Display message key.
     */
    private String _displayMessageKey;
    
    /**
     * The value of the property
     */
    private Object _value;
    
    /**
     * The type of this property
     */
    private Class<?> _type;


    /**
     * The container. Not serialized in this object. Set when this
     * property is added to parent
     */
    private transient ConfigurationPropertiesImpl _parent;
    

    // =======================================================================
    // Internal Methods
    // =======================================================================
    public int getOrder() {
        return _order;
    }
    
    public void setOrder(int order) {
        _order = order;
    }
    
    public void setConfidential(boolean confidential) {
        _confidential = confidential;
    }
    
    public void setName(String name) {
        _name = name;
    }

    public String getHelpMessageKey() {
        return _helpMessageKey;
    }

    public void setHelpMessageKey(String key) {
        _helpMessageKey = key;
    }

    public String getDisplayMessageKey() {
        return _displayMessageKey;
    }

    public void setDisplayMessageKey(String key) {
        _displayMessageKey = key;
    }
    
    public void setType(Class<?> type) {
        _type = type;
    }
    
    public ConfigurationPropertiesImpl getParent() {
        return _parent;
    }
    
    public void setParent(ConfigurationPropertiesImpl parent) {
        _parent = parent;
    }
    
    private String formatMessage(String key, String dflt, Object...args) {
        APIConfigurationImpl apiConfig = getParent().getParent();
        Locale locale = apiConfig.getLocale();
        ConnectorMessages messages = apiConfig.getConnectorInfo().getMessages();
        return messages.format(locale, key, dflt, args);
    }
    
    // =======================================================================
    // Interface Methods
    // =======================================================================

    /**
     * {@inheritDoc}
     */
    public boolean isConfidential() {
        return _confidential;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return _name;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getType() {
        return _type;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValue() {
        return _value;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setValue(Object value) {
        _value = value;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getHelpMessage(String def) {
        return formatMessage(_helpMessageKey, def);
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName(String def) {
        return formatMessage(_displayMessageKey, def);
    }
    
    public int hashCode() {
        return getName().hashCode();
    }
    
    public boolean equals(Object o) {
        if ( o instanceof ConfigurationPropertyImpl ) {
            ConfigurationPropertyImpl other = (ConfigurationPropertyImpl)o;
            if (!getName().equals(other.getName())) {
                return false;
            }
            if (!CollectionUtil.equals(getValue(),other.getValue())) {
                return false;
            }
            if (getOrder() != other.getOrder()) {
                return false;
            }
            if (!CollectionUtil.equals(getHelpMessageKey(),other.getHelpMessageKey())) {
                return false;
            }
            if (!CollectionUtil.equals(getDisplayMessageKey(),other.getDisplayMessageKey())) {
                return false;
            }
            if (isConfidential() != other.isConfidential()) {
                return false;
            }
            if (!CollectionUtil.equals(getType(),other.getType())) {
                return false;
            }
            
            return true;
        }
        return false;
    }
    
}
