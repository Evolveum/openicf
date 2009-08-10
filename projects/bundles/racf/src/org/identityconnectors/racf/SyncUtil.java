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
package org.identityconnectors.racf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;

public class SyncUtil {
    private final static String             RACF_PASSWORD = "racfpassword";

    private RacfConnector                   _connector;


    public SyncUtil(RacfConnector connector) {
        _connector = connector;

        Map<String, Object> geo = getRootDSE();

        if (!geo.containsKey("changelog")
                || !geo.containsKey("lastchangenumber")
                || !geo.containsKey("firstchangenumber")) {
            String error = "Unable to locate the RACF change log.";
            throw new ConnectorException(error);
        }
    }

    public SyncToken getLatestSyncToken(ObjectClass objClass) {
        Map<String, Object> geo = getRootDSE();
        if (geo != null)
            return new SyncToken((String)geo.get("lastchangenumber"));
        return null;
    }

    public void sync(ObjectClass objClass, SyncToken token,
            SyncResultsHandler handler, OperationOptions options) {
        String maxChangeNumber = null;
        if (token!=null && token.getValue() != null) {
            maxChangeNumber = (String) token.getValue();
        }
        // either set the default to the current highest value or null
        // for no filter (get everyone).
        //
        // PWD: set to current min change number, not 0, if resetting.
        if (maxChangeNumber == null) {
            Boolean resetToToday = ((RacfConfiguration)_connector.getConfiguration()).getActiveSyncResetToToday();
            if (resetToToday==null)
                resetToToday = false;
            
            if (resetToToday.booleanValue()) {
                Map<String, Object> geo = getRootDSE();
                if (geo != null)
                    maxChangeNumber = (String)geo.get("lastchangenumber");
            } else {
                Map<String, Object> geo = getRootDSE();
                if (geo != null)
                    maxChangeNumber = (String)geo.get("firstchangenumber");
            }

            if (maxChangeNumber == null)
                maxChangeNumber = "0";
        }

        // Filter based on the person who made the change.
        //
        List<String> nameFilter = null;
        String[] attr = ((RacfConfiguration)_connector.getConfiguration()).getActiveSyncFilterChangesBy();
        if (attr != null && attr.length > 0)
            nameFilter = Arrays.asList(attr);

        // Search Context
        //
        String context = null;
        Map<String, Object> dse = getRootDSE();
        if (dse != null)
            context = (String) dse.get("changelog");
        if (context == null)
            context = "cn=changelog";

        String[] attributesToGet = {
                "targetdn",
                "changetype",
                "changes",
                "changetime",
                "changenumber",
                "ibm-changeinitiatorsname" // RACF LDAP uses this attribute for the modifiersname. 
        };

        try {
            SearchControls subTreeControls = new SearchControls(SearchControls.ONELEVEL_SCOPE, 4095, 0, attributesToGet, true, true);
            NamingEnumeration<SearchResult> entries = ((RacfConnection)_connector.getConnection()).getDirContext().search(context, getFilter(maxChangeNumber), subTreeControls);

            // Iterate through the entries, filtering and then applying changes
            //
            while (entries.hasMore()) {
                SearchResult entry = entries.next();
                Attributes attributes = entry.getAttributes();

                {
                    Object value = getAttributeValueFromLdap(attributes, "changenumber");
                    if (value != null) {
                        int cur = atoi(value.toString(), -1);
                        int max = atoi(maxChangeNumber, -1);
                        if (cur > max)
                            maxChangeNumber = value.toString();
                    }
                }

                // Filter changes by person who changed, if enabled
                //
                boolean filterOut = false;
                Map<String, Object> changes = new TreeMap<String, Object>(new StringComparator());
                addChangeLogAttributes(changes, entry);
                //GAEL: IBM does differently, "modifiersname" is not in the changes but in the
                // attribute ibm-changeinitiatorsname
                if (nameFilter != null) {
                    String modifier = (String) getAttributeValueFromLdap(attributes, "ibm-changeinitiatorsname");
                    if (modifier != null) {
                        for (int j = 0; j < nameFilter.size(); j++) {
                            String listedFilter = (String) nameFilter.get(j);

                            if (listedFilter.trim().equalsIgnoreCase(modifier.trim())) {
                                filterOut = true;
                                break;
                            }
                        }
                    }
                }

                if (!filterOut) {
                    LocalHandler localHandler = new LocalHandler();
                    javax.naming.directory.Attribute targetDn = attributes.get("targetdn");
                    Object changeType = getAttributeValueFromLdap(attributes, "changetype");
                    String change = changeType==null?null:changeType.toString();
                    if ("DELETE".equalsIgnoreCase(change)) {
                        SyncDeltaBuilder builder = new SyncDeltaBuilder();
                        builder.setDeltaType(SyncDeltaType.DELETE);
                        builder.setUid(new Uid(targetDn.get().toString().toUpperCase()));
                        builder.setToken(new SyncToken(maxChangeNumber));
                        handler.handle(builder.build());
                    } else if ("ADD".equalsIgnoreCase(change) || "MODIFY".equalsIgnoreCase(change)) {
                        String query = "(racfid="+_connector.extractRacfIdFromLdapId(targetDn.get().toString())+")";
                        
                        _connector.executeQuery(ObjectClass.ACCOUNT, query, localHandler, options);
                        if (localHandler.size()>0) {
                            ConnectorObject user = localHandler.iterator().next();
                            //TODO: for password sync, do we want to check if this object
                            // has a password?
                            SyncDeltaBuilder builder = new SyncDeltaBuilder();
                            builder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
                            builder.setObject(user);
                            builder.setToken(new SyncToken(maxChangeNumber));
                            handler.handle(builder.build());
                        }
                    }
                }
            }
        } catch (NamingException ne) {
            throw ConnectorException.wrap(ne);
        }
    }

    private Map<String, Object> getRootDSE()  {
        try {
            SearchControls subTreeControls = new SearchControls(SearchControls.OBJECT_SCOPE, 4095, 0, null, true, true);
           
            Set<String> attributesToGet = new HashSet<String>();
            attributesToGet.add("changelog");
            attributesToGet.add("firstchangenumber");
            attributesToGet.add("lastchangenumber");
            Map<String,Object> attributesRead = new HashMap<String, Object>();

            getAttributesFromLdap("", attributesRead, attributesToGet, SearchControls.OBJECT_SCOPE);
            return attributesRead;
        } catch (NamingException ne) {
            throw ConnectorException.wrap(ne);
        }
    }

    private SearchResult getAttributesFromLdap(String ldapName, Map<String, Object> attributesRead,
            Set<String> attributesToGet, int scope) throws NamingException {
        SearchControls subTreeControls = new SearchControls(scope, 4095, 0, attributesToGet==null?null:attributesToGet.toArray(new String[0]), true, true);
        SearchResult ldapObject = null;
        NamingEnumeration<SearchResult> results = _connector.getConnection().getDirContext().search(ldapName, "(objectclass=*)", subTreeControls);
        if (!results.hasMoreElements())
            return null;
        ldapObject = results.next();
        Attributes attributes = ldapObject.getAttributes();
        NamingEnumeration<? extends javax.naming.directory.Attribute> attributeEnum = attributes.getAll();
        while (attributeEnum.hasMore()) {
            javax.naming.directory.Attribute attribute = attributeEnum.next();
            attributesRead.put(attribute.getID(), LdapUtil.getValueFromAttribute(attribute));
        }
        return ldapObject;
    }

    private Object getAttributeValueFromLdap(Attributes attributes, String attributeName)
    throws NamingException {
        javax.naming.directory.Attribute entryChangeNumber = attributes.get(attributeName);
        Object value = entryChangeNumber==null?null:entryChangeNumber.get();
        return value;
    }

    private String getFilter(String maxChangeNumber) {
        // build a search string for entries after our changenumber. limit to
        // blocksize.
        String blockSize = ((RacfConfiguration)_connector.getConfiguration()).getActiveSyncBlocksize();

        String attrName = "changenumber";

        Boolean useORSearch = ((RacfConfiguration)_connector.getConfiguration()).getActiveSyncFilterUseOrSearch();
        if (useORSearch==null)
            useORSearch = false;
        
        // Adding the objectClass to the search can decrease performance let the
        // customer decide (not available via the GUI)
        Boolean removeObjectClassSearch = ((RacfConfiguration)_connector.getConfiguration()).getActiveSyncRemoveOCFromFilter();
        if (removeObjectClassSearch==null)
            removeObjectClassSearch = false;
        
        // new min is old one plus one.
        int max = Integer.parseInt(maxChangeNumber);

        int endNumber = 0;

        StringBuffer filter = new StringBuffer();

        if (blockSize != null && useORSearch) {
            // remove the objectClass completely from the search
            if (!removeObjectClassSearch) {
                filter.append("(&(objectClass=changelogentry)");
            } // if removeObjectClassSearch
            filter.append("(|(");
            filter.append(attrName);
            filter.append("=");
            filter.append(Integer.toString(max + 1));
            filter.append(')');

            try {
                endNumber = Integer.parseInt(maxChangeNumber) + Integer.parseInt(blockSize);
            } catch (NumberFormatException ne) {
                // nothing
            }

            if (endNumber > 0) {
                for (int i = (max + 2); i <= endNumber; i++) {
                    filter.append("(");
                    filter.append(attrName);
                    filter.append("=");
                    filter.append(Integer.toString(i));
                    filter.append(')');
                } // for i=max+2..endNumber
            } // if endNumber > 0
            if (!removeObjectClassSearch) {
                filter.append(")");
            } // if removeObjectClassSearch
            filter.append(")");
        } else {
            filter.append("(&(");
            // remove the objectClass completely from the search
            if (!removeObjectClassSearch) {
                filter.append("objectClass=changelogentry)(");
            } // if removeObjectClassSearch
            filter.append(attrName);
            filter.append(">=");
            filter.append(Integer.toString(max + 1));
            filter.append(')');
            if (blockSize != null) {
                try {
                    endNumber = Integer.parseInt(maxChangeNumber) + Integer.parseInt(blockSize);
                } catch (NumberFormatException ne) {
                }

                if (endNumber > 0) {
                    filter.append("(");
                    filter.append(attrName);
                    filter.append("<=");
                    filter.append(endNumber);
                    filter.append(')');
                }
            }
            filter.append(')');
        }

        return filter.toString();
    }

    private void addChangeLogAttributes(Map<String, Object> map, SearchResult changeLogEntry) throws NamingException {
        Attributes attributes = changeLogEntry.getAttributes();
        String changeType = (String) getAttributeValueFromLdap(attributes, "changetype");
        map.put("changeType",   changeType);
        map.put("changeNumber", getAttributeValueFromLdap(attributes, "changenumber"));
        map.put("targetDN",     getAttributeValueFromLdap(attributes, "targetdn"));
        map.put("identity",     getAttributeValueFromLdap(attributes, "targetdn"));
        //map.put("changes", getAttributeValueFromLdap(attributes, "changes"));

        /**
         * parse out the attribute names that have changed. This is an LDIF
         * formatted record, which are newline terminated lines of the format
         * keyword:value.
         */
        if (changeType.equals("MODIFY")) {
            // GAEL:
            //The changelog attribute "changes" is only present when a RACF user password is changed, 
            // and will contain: replace: racfPassword racfPassword: *ComeAndGetIt* -
            Object changes = getAttributeValueFromLdap(attributes, "changes");
            if (changes != null) {
                map.put("changes", changes);
                Map<String, Object> changeMap = new TreeMap(new StringComparator());
                StringTokenizer st = new StringTokenizer((String) changes, "\n");
                while (st.hasMoreTokens()) {
                    String line = st.nextToken();
                    int sepIndex = line.indexOf(':');
                    if (sepIndex > 0) {
                        String op = line.substring(0, sepIndex).trim();

                        if (op.equals("replace")) {
                            // Only replace operation can appear for the password
                            Object[] val = getLDIFAttributeValue(st);
                            if (val != null) {
                                // here the value should be (has to be) *ComeAndGetIt*
                                // GAEL: Check it in case... but well, it can't be anything else at that point
                                changeMap.put((String)val[0], val[1]);
                            }
                        }
                    }
                    map.put("changedAttributes", changeMap);
                }
            }

        } // if changeType modify
    }

    private Object[] getLDIFAttributeValue(StringTokenizer st) {
        List<String> list = new ArrayList<String>();
        String line = st.nextToken();
        String name = null;
        while (line != null && !line.startsWith("-")) {
            // If this line starts with a space, then it is a continuation
            // of the previous line, so modify the previous value by
            // appending the additional value data.
            if ( line.startsWith(" ") ) {
                // if list.size() == 0, then this is illegal LDIF, so we
                // don't cover that case.
                if ( list.size() > 0 ) {
                    int i = list.size() - 1;
                    String value = list.get(i) + line.substring(1);
                    list.set(i, value.trim());
                }
            } else {
                int sepIndex = line.indexOf(':');
                if (sepIndex > 0) {
                    String key = line.substring(0, sepIndex);
                    String value = line.substring(sepIndex + 1);
                    name = key.trim();
                    list.add(value.trim());
                }
            }

            if (st.hasMoreTokens())
                line = st.nextToken();
            else
                break;
        }

        //
        // The name has been set. Set the value to either the single value or
        // the list.
        //
        if (list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return new Object[] {name, parseLDIFValue(name, list.get(0), 0)};
        } else {
            List<Object> decodedList = new ArrayList<Object>(list.size());
            for (String value : list) {
                decodedList.add(parseLDIFValue(name, value, 0));
            }
            return new Object[] {name, decodedList};
        }
    }

    private Object parseLDIFValue(String attributeName, String line, int pos) {
        if (pos >= line.length()) {
            return "";
        }
        return line.substring(pos).trim();
    }

    private int atoi(String a, int def) {
        int i = def;
        if (a != null && a.length() > 0) {
            try {
                int decimal = a.indexOf('.');
                if (decimal > 0)
                    a = a.substring(0, decimal);
                i = Integer.parseInt(a);
            } catch (NumberFormatException e) {
                // ignore, return default
            }
        }
        return i;
    }

    static public class StringComparator implements java.util.Comparator, Serializable {
        public int compare(Object o1, Object o2) {
            String key1 = (String)o1;
            String key2 = (String)o2;

            // the arrays must be non-null but the cells may be null

            if (key1!=null && key2!=null) return key1.compareToIgnoreCase(key2);
            else if (key1 == null && key2 == null) return 0;
            else if (key1 == null) return -1;
            else return 1;
        }
    }

}
