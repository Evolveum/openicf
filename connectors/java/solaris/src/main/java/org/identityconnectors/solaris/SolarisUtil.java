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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 *
 * Portions Copyrighted 2012 Evolveum, Radovan Semancik
 */
package org.identityconnectors.solaris;

//import static org.identityconnectors.solaris.SolarisMessages.MSG_NOT_SUPPORTED_OBJECTCLASS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.AttrUtil;
import org.identityconnectors.solaris.attr.ConnectorAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.mode.ActivationMode;
import org.identityconnectors.solaris.operation.AbstractOp;
import org.identityconnectors.solaris.operation.search.SolarisEntries;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/** helper class for Solaris specific operations. */
public final class SolarisUtil {

	private static final Log logger = Log.getLog(SolarisUtil.class);
	
    private SolarisUtil() {
    }

    /** Maximum number of characters per line in Solaris shells. */
    public static final int DEFAULT_LIMIT = 120;

    private static StringBuilder limitString(StringBuilder data, int limit) {
        StringBuilder result = null;
        if (data.length() > limit) {
            result = new StringBuilder(limit);
            result.append(data.substring(0, limit));
            // /<newline> separator of Unix command line cmds.
            result.append("\\\n");
            final String remainder = data.substring(limit, data.length());
            if (remainder.length() > limit) {
                // TODO performance: might be a more effective way to handle
                // this copying. Maybe skip copying and pass the respective part
                // of StringBuilder directly to the recursive call.
                StringBuilder sbtmp = new StringBuilder();
                sbtmp.append(remainder);
                result.append(limitString(sbtmp, limit));
            } else {
                result.append(remainder);
            }
        } else {
            return data;
        }
        return result;
    }

    /**
     * Cut the command into pieces, so it doesn't have a longer line than the
     * given DEFAULT_LIMIT.
     *
     * @param data
     * @return
     */
    public static String limitString(StringBuilder data) {
        /*
         * == max length of line from
         * SolarisResourceAdapter#getUpdateNativeUserScript(), line
         * userattribparams
         */
        return limitString(data, DEFAULT_LIMIT).toString();
    }

    public static void controlObjectClassValidity(ObjectClass oclass,
            ObjectClass[] acceptedObjectClasses, Class<? extends AbstractOp> operation,
            final SolarisConfiguration configuration) {
        for (ObjectClass objectClass : acceptedObjectClasses) {
            if (objectClass.equals(oclass)) {
                return;
            }
        }

        throw new UnsupportedOperationException(configuration.getConnectorMessages().format(
                "MSG_NOT_SUPPORTED_OBJECTCLASS",
                "Object class '%s' is not supported by operation %s.", oclass, operation.getName()));
    }

    /**
     * Translate the given connector attributes to their native Solaris
     * counterpart.
     *
     * Contract: PASSWORD and UID shouldn't be contained in the attribute set,
     * they supposed to be handled separately, outside of SolarisEntry.
     *
     * @param entryName
     *            the entry's name (can be either GROUP or ACCOUNT)
     * @param objectClass
     *            object class type
     * @param attributes
     *            connector attributes
     * @return the translated attributes encapsulated
     */
    public static SolarisEntry convertIcfAttributesToSolarisEntry(String entryName, ObjectClass objectClass,
            Set<Attribute> attributes, SolarisConfiguration config) {
        // translate connector attributes to native counterparts
        final SolarisEntry.Builder builder = new SolarisEntry.Builder(entryName);
        
        boolean ignoreExpire = false;
        boolean ignoreLock = false;
        
        // This is kind of HACK. We brutally override the original values if activation is configured.
        // TODO: do it correctly and change the entire connector to delta-based. However it may be easier to
        // just drop the connector and create a new one.
        for (Attribute attribute : attributes) {
        	if (attribute.getName().equals(OperationalAttributes.ENABLE_NAME)) {
        		if (ActivationMode.EXPIRATION.getConfigString().equals(config.getActivationMode())) {
        			ignoreExpire = true;
        		} else if (ActivationMode.LOCKING.getConfigString().equals(config.getActivationMode())) {
        			ignoreLock = true;
        		}
        	}
        }
        
        for (Attribute attribute : attributes) {
        	String icfAttrName = attribute.getName();
        	ConnectorAttribute sunAttr = null;
        	List<Object> icfValues = attribute.getValue();
        	List<Object> sunValues = icfValues;
        	
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
            
            if (ignoreExpire && sunAttr != null && sunAttr.getNative() == NativeAttribute.USER_EXPIRE) {
            	// skip, will be overridden by __ENABLE__
            	continue;
            }

            if (ignoreLock && sunAttr != null && sunAttr.getNative() == NativeAttribute.LOCK) {
            	// skip, will be overridden by __ENABLE__
            	continue;
            }

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
            		sunAttr = AccountAttribute.EXPIRE;
            		
            		if (icfValue != null) {
            			if (icfValue) {
            				sunValue = "";
            			} else {
            				sunValue = "1";
            			}
            		}
            		
            	} else if (ActivationMode.LOCKING.getConfigString().equals(config.getActivationMode())) {
            		sunAttr = AccountAttribute.LOCK;
            		
            		if (icfValue != null) {
                    	sunValue = !icfValue;
                    }
            		
            	} else if (ActivationMode.NONE.getConfigString().equals(config.getActivationMode())) {
            		// nothing to do
            		
            	} else {
            		throw new IllegalArgumentException("Unknown activation mode "+config.getActivationMode());
            	}
            	
            	sunValues = new ArrayList<Object>(1);
            	if (sunValue != null) {
            		sunValues.add(sunValue);
            	}
            }
            
            if (sunAttr != null) {
                builder.addAttr(sunAttr.getNative(), sunValues);
            } else if (!attribute.getName().equals(OperationalAttributes.PASSWORD_NAME)
                    && !attribute.getName().equals(Uid.NAME)) {
                // FIXME: do we really need this exception here? It'd be way
                // more beautiful without if-s.
                // TODO it might be a more beautiful sort out this error (filter
                // the attributes in layers above this class).
                throw new ConnectorException("ERROR: Unsupported attribute: " + attribute.getName());
            }
        }
        return builder.build();
    }
    
    /**
     * @param entry
     *            can be both ACCOUNT and GROUP
     * @return A connector object based on attributes of given 'entry', that
     *         contains the ATTRS_TO_GET.
     */
    public static ConnectorObject convertToConnectorObject(SolarisEntry entry, String[] attrsToGet, 
    		ObjectClass oclass, SolarisConfiguration config) {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        Map<String, Attribute> indexedEntry = AttributeUtil.toMap(entry.getAttributeSet());

        // add UID and Name (contract of ConnectorObject)
        final String entryName = entry.getName();
        builder.addAttribute(new Uid(entryName));
        builder.addAttribute(new Name(entryName));

        // and rest of the attributes
        for (String icfAttrName : attrsToGet) {
        	ConnectorAttribute connAttr = AttrUtil.toSolarisAttribute(icfAttrName, oclass, config);
        	
            final Attribute attrToConvert = indexedEntry.get(connAttr.getNative().getName());
            List<Object> sunValues = (attrToConvert != null) ? attrToConvert.getValue() : null;
            if (sunValues == null) {
                sunValues = Collections.emptyList();
            }

            List<Object> icfValues = AttrUtil.toIcfAttributeValues(icfAttrName, sunValues, config);

            logger.ok("Converted attr {0}={1} to {2}={3}", connAttr.getName(), sunValues, icfAttrName, icfValues);
            
            if (icfValues != null) {
            	builder.addAttribute(icfAttrName, icfValues);
            }
        }

        return builder.build();
    }

    /**
     * Search for the existence of given entry based on its unique
     * identificator.
     *
     * @param entryType
     *            type of entry
     * @param entry
     *            check the presence of this entry
     * @param conn
     * @return true if entry exists, false otherwise
     */
    public static boolean exists(ObjectClass entryType, SolarisEntry entry, SolarisConnection conn) {
        Iterator<SolarisEntry> result = null;
        if (entryType.is(ObjectClass.ACCOUNT_NAME)) {
            return SolarisEntries.getAccount(entry.getName(), EnumSet.of(NativeAttribute.NAME),
                    conn) != null;
        } else if (entryType.is(ObjectClass.GROUP_NAME)) {
            result = SolarisEntries.getAllGroups(EnumSet.of(NativeAttribute.NAME), conn);
        } else {
            throw new ConnectorException("Non existing object class: " + entryType.toString());
        }
        while (result != null && result.hasNext()) {
            SolarisEntry it = result.next();
            if (it.getName().equals(entry.getName())) {
                return true;
            }
        }
        return false;
    }
}
