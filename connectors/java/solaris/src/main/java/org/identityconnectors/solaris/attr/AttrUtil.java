/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package org.identityconnectors.solaris.attr;

import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.mode.ActivationMode;

/**
 * @author Radovan Semancik
 *
 */
public final class AttrUtil {

    public static Boolean parseBoolean(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.valueOf((String) val);
        } else {
            throw new IllegalArgumentException("Unexpected class " + val.getClass());
        }
    }
    
    public static ConnectorAttribute toSolarisAttribute(String icfAttrName, ObjectClass objectClass, SolarisConfiguration config) {
        ConnectorAttribute sunAttr = null;
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
        	if (Uid.NAME.equalsIgnoreCase(icfAttrName)) {
        		sunAttr = AccountAttribute.NAME;
        	} else {
        		sunAttr = AccountAttribute.forAttributeName(icfAttrName);
        	}
        } else {
        	if (Uid.NAME.equalsIgnoreCase(icfAttrName)) {
        		sunAttr = GroupAttribute.GROUPNAME;
        	} else {
        		sunAttr = GroupAttribute.forAttributeName(icfAttrName);
        	}
        }

        if (icfAttrName.equals(OperationalAttributes.ENABLE_NAME)) {
        	if (ActivationMode.EXPIRATION.getConfigString().equals(config.getActivationMode())) {
        		sunAttr = AccountAttribute.EXPIRE;
        		
        	} else if (ActivationMode.LOCKING.getConfigString().equals(config.getActivationMode())) {
        		sunAttr = AccountAttribute.LOCK;
        		
        	} else if (ActivationMode.NONE.getConfigString().equals(config.getActivationMode())) {
        		// nothing to do
        		
        	} else {
        		throw new IllegalArgumentException("Unknown activation mode "+config.getActivationMode());
        	}
        }
        
        return sunAttr;
    }

    public static List<Object> toSolarisAttributeValues(String icfAttrName, List<Object> icfValues, SolarisConfiguration config) {
        if (icfAttrName.equals(OperationalAttributes.ENABLE_NAME)) {
        	Boolean icfValue = null;
        	Object sunValue = icfValue;
        	if (icfValues != null) {
            	if (icfValues.size() > 1) {
            		throw new IllegalArgumentException("More than one value for attribute "+OperationalAttributes.ENABLE_NAME);
            	}
            	if (icfValues.size() > 0) {
            		icfValue = (Boolean)icfValues.get(0);
            	}
        	}
        	
        	if (ActivationMode.EXPIRATION.getConfigString().equals(config.getActivationMode())) {
        		if (icfValue != null) {
        			if (icfValue) {
        				sunValue = "";
        			} else {
        				sunValue = "1";
        			}
        		}
        		
        	} else if (ActivationMode.LOCKING.getConfigString().equals(config.getActivationMode())) {
        		if (icfValue != null) {
                	sunValue = !icfValue;
                }
        		
        	} else if (ActivationMode.NONE.getConfigString().equals(config.getActivationMode())) {
        		// nothing to do
        		
        	} else {
        		throw new IllegalArgumentException("Unknown activation mode "+config.getActivationMode());
        	}
        	
        	List sunValues = new ArrayList(1);
        	if (sunValue != null) {
        		sunValues.add(sunValue);
        	}
        	return sunValues;
        }
        
        return icfValues;
    }
    
    public static List<Object> toIcfAttributeValues(String icfAttrName, List<Object> sunValues, SolarisConfiguration config) {
        if (icfAttrName.equals(OperationalAttributes.ENABLE_NAME)) {
        	Object sunValue = null;
        	Boolean icfValue = null;
        	if (sunValues != null) {
            	if (sunValues.size() > 1) {
            		throw new IllegalArgumentException("More than one value for attribute "+OperationalAttributes.ENABLE_NAME);
            	}
            	if (sunValues.size() > 0) {
            		sunValue = sunValues.get(0);
            	}
        	}
        	
        	if (ActivationMode.EXPIRATION.getConfigString().equals(config.getActivationMode())) {
        		if (sunValue == null) {
        			// No expiration => account enabled
        			icfValue = true;
        		} else {
        			if ("".equals(sunValue)) {
        				icfValue = true;
        			} else if ("1".equals(sunValue)) {
        				icfValue = false;
        			}
        		}
        		
        	} else if (ActivationMode.LOCKING.getConfigString().equals(config.getActivationMode())) {
        		if (sunValue != null) {
                	icfValue = !((Boolean)sunValue);
                }
        		
        	} else if (ActivationMode.NONE.getConfigString().equals(config.getActivationMode())) {
        		// nothing to do
        		
        	} else {
        		throw new IllegalArgumentException("Unknown activation mode "+config.getActivationMode());
        	}
        	
        	List<Object> icfValues = null;
        	
        	if (icfValue != null) {
        		icfValues = new ArrayList<Object>(1);
        		icfValues.add(icfValue);
        	}
        	return icfValues;
        }
        
        return sunValues;
    }
}
