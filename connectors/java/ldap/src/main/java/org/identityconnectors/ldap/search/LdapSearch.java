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
 * 
 * Portions Copyrighted [2013-2014] Forgerock
 * Portions Copyrighted 2014 Evolveum
 */
package org.identityconnectors.ldap.search;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;

import java.util.Date;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.SortControl;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedByteArray.Accessor;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.QualifiedUid;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.ldap.ADUserAccountControl;
import org.identityconnectors.ldap.GroupHelper;
import org.identityconnectors.ldap.LdapConfiguration;
import org.identityconnectors.ldap.LdapConnection;
import org.identityconnectors.ldap.LdapConstants;
import org.identityconnectors.ldap.LdapEntry;
import org.identityconnectors.ldap.schema.LdapSchemaMapping;
import org.identityconnectors.ldap.sync.sunds.PasswordDecryptor;

import static org.identityconnectors.common.CollectionUtil.newCaseInsensitiveSet;
import static org.identityconnectors.common.CollectionUtil.newSet;
import static org.identityconnectors.common.StringUtil.isBlank;

import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.ldap.ADLdapUtil;

import static org.identityconnectors.ldap.LdapUtil.buildMemberIdAttribute;
import static org.identityconnectors.ldap.LdapUtil.getStringAttrValues;
import static org.identityconnectors.ldap.ADLdapUtil.objectGUIDtoString;
import static org.identityconnectors.ldap.ADLdapUtil.fetchGroupMembersByRange;
import static org.identityconnectors.ldap.ADLdapUtil.getADLdapDatefromJavaDate;
import static org.identityconnectors.ldap.ADLdapUtil.getJavaDateFromADTime;

import org.identityconnectors.ldap.LdapConnection.ServerType;

/**
 * A class to perform an LDAP search against a {@link LdapConnection}.
 *
 * @author Andrei Badea
 */
public class LdapSearch {

    private static final Log log = Log.getLog(LdapSearch.class);
    private final LdapConnection conn;
    private final ObjectClass oclass;
    private final LdapFilter filter;
    private final OperationOptions options;
    private final GroupHelper groupHelper;
    private final String[] baseDNs;
    private PasswordDecryptor passwordDecryptor = null;
    private final ResultsHandler handler;

    public static Set<String> getAttributesReturnedByDefault(LdapConnection conn, ObjectClass oclass) {
        if (oclass.equals(LdapSchemaMapping.ANY_OBJECT_CLASS)) {
            return newSet(Name.NAME);
        }
        Set<String> result = newCaseInsensitiveSet();
        ObjectClassInfo oci = conn.getSchemaMapping().schema().findObjectClassInfo(oclass.getObjectClassValue());
        if (oci != null) {
            for (AttributeInfo info : oci.getAttributeInfo()) {
                if (info.isReturnedByDefault()) {
                    result.add(info.getName());
                }
            }
        }
        return result;
    }

    public LdapSearch(LdapConnection conn, ObjectClass oclass, LdapFilter filter, ResultsHandler handler, OperationOptions options) {
        this(conn, oclass, filter, handler, options, conn.getConfiguration().getBaseContexts());
    }

    public LdapSearch(LdapConnection conn, ObjectClass oclass, LdapFilter filter, ResultsHandler handler, OperationOptions options, String... baseDNs) {
        this.conn = conn;
        this.oclass = oclass;
        this.filter = filter;
        this.options = options;
        this.baseDNs = baseDNs;
        this.handler = handler;

        groupHelper = new GroupHelper(conn);
    }

    public final void execute() {
        execute(handler);
    }

    /**
     * Performs the search and passes the resulting {@link ConnectorObject}s to
     * the given handler.
     *
     * @param handler the handler.
     * @throws NamingException if a JNDI exception occurs.
     */
    public final void execute(final ResultsHandler handler) {
        final String[] attrsToGetOption = options.getAttributesToGet();
        final Set<String> attrsToGet = getAttributesToGet(attrsToGetOption);
        LdapInternalSearch search = getInternalSearch(attrsToGet);
        search.execute(new LdapSearchResultsHandler() {
            public boolean handle(String baseDN, SearchResult result) throws NamingException {
                return handler.handle(createConnectorObject(baseDN, result, attrsToGet, attrsToGetOption != null));
            }
        });
        
        if (handler instanceof SearchResultsHandler) {
        	String pagedResultsCookie = search.getPagedResultsCookie();
			int remainingPagedResults = search.getRemainingPagedResults();
			org.identityconnectors.framework.common.objects.SearchResult result = 
        			new org.identityconnectors.framework.common.objects.SearchResult(pagedResultsCookie, remainingPagedResults);
			((SearchResultsHandler)handler).handleResult(result);
        }
    }

    /**
     * Executes the query against all configured base DNs and returns the first
     * {@link ConnectorObject} or {@code null}.
     */
    public final ConnectorObject getSingleResult() {
        final String[] attrsToGetOption = options.getAttributesToGet();
        final Set<String> attrsToGet = getAttributesToGet(attrsToGetOption);
        final ConnectorObject[] results = new ConnectorObject[]{null};
        LdapInternalSearch search = getInternalSearch(attrsToGet);
        search.execute(new LdapSearchResultsHandler() {
            public boolean handle(String baseDN, SearchResult result) throws NamingException {
                results[0] = createConnectorObject(baseDN, result, attrsToGet, attrsToGetOption != null);
                return false;
            }
        });
        return results[0];
    }

    private LdapInternalSearch getInternalSearch(Set<String> attrsToGet) {
        // This is a bit tricky. If the LdapFilter has an entry DN,
        // we only need to look at that entry and check whether it matches
        // the native filter. Moreover, when looking at the entry DN
        // we must not throw exceptions if the entry DN does not exist or is
        // not valid -- just as no exceptions are thrown when the native
        // filter doesn't return any values.
        //
        // In the simple case when the LdapFilter has no entryDN, we
        // will just search over our base DNs looking for entries
        // matching the native filter.

        LdapSearchStrategy strategy;
        List<String> baseDNs;
        int searchScope;

        String filterEntryDN = filter != null ? filter.getEntryDN() : null;
        if (filterEntryDN != null) {
            // Would be good to check that filterEntryDN is under the configured base contexts.
            // However, the adapter is likely to pass entries outside the base contexts,
            // so not checking in order to be on the safe side.
            strategy = new DefaultSearchStrategy(true);
            baseDNs = singletonList(filterEntryDN);
            searchScope = SearchControls.OBJECT_SCOPE;
        } else {
            strategy = getSearchStrategy();
            baseDNs = getBaseDNs();
            searchScope = getLdapSearchScope();
        }

        SearchControls controls = LdapInternalSearch.createDefaultSearchControls();
        Set<String> ldapAttrsToGet = getLdapAttributesToGet(attrsToGet);
        controls.setReturningAttributes(ldapAttrsToGet.toArray(new String[ldapAttrsToGet.size()]));
        controls.setSearchScope(searchScope);

        String optionsFilter = LdapConstants.getSearchFilter(options);
        String userFilter = null;
        if (oclass.equals(ObjectClass.ACCOUNT)) {
            userFilter = conn.getConfiguration().getAccountSearchFilter();
        } else if (oclass.equals(ObjectClass.GROUP)) {
            userFilter = conn.getConfiguration().getGroupSearchFilter();
        }
        String nativeFilter = filter != null ? filter.getNativeFilter() : null;
        return new LdapInternalSearch(conn, getSearchFilter(optionsFilter, nativeFilter, userFilter), baseDNs, strategy, controls);
    }

    private Set<String> getLdapAttributesToGet(Set<String> attrsToGet) {
        Set<String> cleanAttrsToGet = newCaseInsensitiveSet();
        cleanAttrsToGet.addAll(attrsToGet);
        cleanAttrsToGet.remove(LdapConstants.LDAP_GROUPS_NAME);
        boolean posixGroups = cleanAttrsToGet.remove(LdapConstants.POSIX_GROUPS_NAME);
        Set<String> result = conn.getSchemaMapping().getLdapAttributes(oclass, cleanAttrsToGet, true);
        if (posixGroups) {
            result.add(GroupHelper.getPosixRefAttribute());
        }
        // For compatibility with the adapter, we do not ask the server for DN attributes,
        // such as entryDN; we compute them ourselves. Some servers might not support such attributes anyway.
        result.removeAll(LdapEntry.ENTRY_DN_ATTRS);
        return result;
    }

    /**
     * Creates a {@link ConnectorObject} based on the given search result. The
     * search result name is expected to be a relative one, thus the {@code
     * baseDN} parameter is needed in order to create the whole entry DN, which
     * is used to compute the connector object's name attribute.
     */
    private ConnectorObject createConnectorObject(String baseDN, SearchResult result, Set<String> attrsToGet, boolean emptyAttrWhenNotFound) {
        LdapEntry entry = LdapEntry.create(baseDN, result);

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(oclass);
        builder.setUid(conn.getSchemaMapping().createUid(oclass, entry));
        builder.setName(conn.getSchemaMapping().createName(oclass, entry));

        for (String attrName : attrsToGet) {
            Attribute attribute = null;
            if (attrName.equalsIgnoreCase(Uid.NAME) || attrName.equalsIgnoreCase(Name.NAME)) {
                continue;
            }
            if (LdapConstants.isLdapGroups(attrName)) {
                List<String> ldapGroups = groupHelper.getLdapGroups(entry.getDN().toString());
                attribute = AttributeBuilder.build(LdapConstants.LDAP_GROUPS_NAME, ldapGroups);
            } else if (LdapConstants.isPosixGroups(attrName)) {
                Set<String> posixRefAttrs = getStringAttrValues(entry.getAttributes(), GroupHelper.getPosixRefAttribute());
                List<String> posixGroups = groupHelper.getPosixGroups(posixRefAttrs);
                attribute = AttributeBuilder.build(LdapConstants.POSIX_GROUPS_NAME, posixGroups);
			} else if (LdapConstants.PASSWORD.is(attrName)) {
				try {
					String hashAlgorithm = conn.getConfiguration()
							.getPasswordHashAlgorithm();
					if (isBlank(hashAlgorithm) || "NONE".equalsIgnoreCase(hashAlgorithm)) {
						javax.naming.directory.Attribute passwordAttribute = entry.getAttributes().get(conn.getConfiguration().getPasswordAttribute());
						if (passwordAttribute != null){
							byte[] passwordVal = (byte[]) passwordAttribute.get();
							String stringPwdValue = new String(passwordVal);
							if (stringPwdValue.startsWith("{")){
								log.warn("Could not read password value. Password is in unsupported format.");
								attribute = AttributeBuilder.build(attrName, new GuardedString());
							}
							attribute = AttributeBuilder.buildPassword(stringPwdValue.toCharArray());
						} else {
							log.warn("Could not read password value. Password is in unsupported format.");
							attribute = AttributeBuilder.build(attrName, new GuardedString());	
						}
						
					} else {
						log.warn("Could not read password value. Password is in unsupported format.");
						attribute = AttributeBuilder.build(attrName, new GuardedString());
					}
				} catch (NamingException e) {
	                log.error(e, "Can't read password attribute");
                }
            } else if (LdapConstants.MS_GUID_ATTR.equalsIgnoreCase(attrName)) {
                attribute = AttributeBuilder.build(LdapConstants.MS_GUID_ATTR, objectGUIDtoString(entry.getAttributes().get(LdapConstants.MS_GUID_ATTR)));
            } else if (oclass.equals(ObjectClass.ACCOUNT) && OperationalAttributes.OPERATIONAL_ATTRIBUTE_NAMES.contains(attrName)) {
                try {
                    switch (conn.getServerType()) {
                        case MSAD_GC:
                        case MSAD:
                            String controls = entry.getAttributes().get(ADUserAccountControl.MS_USR_ACCT_CTRL_ATTR).get().toString();
                            if (OperationalAttributeInfos.ENABLE.is(attrName)) {
                                builder.addAttribute(AttributeBuilder.buildEnabled(!ADUserAccountControl.isAccountDisabled(controls)));
                            } else if (OperationalAttributeInfos.LOCK_OUT.is(attrName)) {
                                builder.addAttribute(AttributeBuilder.buildLockOut(ADUserAccountControl.isAccountLockOut(controls)));
                            } else if (OperationalAttributeInfos.PASSWORD_EXPIRED.is(attrName)) {
                                builder.addAttribute(AttributeBuilder.buildPasswordExpired(ADUserAccountControl.isPasswordExpired(controls)));
                            }
                            break;
                        case MSAD_LDS:
                            if (OperationalAttributeInfos.ENABLE.is(attrName) && entry.getAttributes().get(LdapConstants.MS_DS_USER_ACCOUNT_DISABLED) != null) {
                                builder.addAttribute(AttributeBuilder.buildEnabled(!Boolean.parseBoolean(entry.getAttributes().get(LdapConstants.MS_DS_USER_ACCOUNT_DISABLED).get().toString())));
                            } else if (OperationalAttributeInfos.PASSWORD_EXPIRED.is(attrName) && entry.getAttributes().get(LdapConstants.MS_DS_USER_PASSWORD_EXPIRED) != null) {
                                builder.addAttribute(AttributeBuilder.buildPasswordExpired(Boolean.parseBoolean(entry.getAttributes().get(LdapConstants.MS_DS_USER_PASSWORD_EXPIRED).get().toString())));
                            } else if (OperationalAttributeInfos.LOCK_OUT.is(attrName) && entry.getAttributes().get(LdapConstants.MS_DS_USER_ACCOUNT_AUTOLOCKED) != null) {
                                builder.addAttribute(AttributeBuilder.buildLockOut(Boolean.parseBoolean(entry.getAttributes().get(LdapConstants.MS_DS_USER_ACCOUNT_AUTOLOCKED).get().toString())));
                            }
                            break;
                        default:
                            log.warn("Special Attribute {0} of object class {1} is not mapped to an LDAP attribute",
                                    attrName, oclass.getObjectClassValue());
                    }
                } catch (NamingException e) {
                    log.error(e, "Can't read " + ADUserAccountControl.MS_USR_ACCT_CTRL_ATTR);
                }
            } else {
                attribute = conn.getSchemaMapping().createAttribute(oclass, attrName, entry, emptyAttrWhenNotFound);
            }

            // Some AD specifics
            if (conn.getServerType().equals(ServerType.MSAD)) {
                if (oclass.equals(ObjectClass.ACCOUNT)) {
                    try {
                        if (ADLdapUtil.ACCOUNT_EXPIRES.equalsIgnoreCase(attrName)) {
                            String value = (String) entry.getAttributes().get(ADLdapUtil.ACCOUNT_EXPIRES).get(0);
                            if ("0".equalsIgnoreCase(value) || ADLdapUtil.ACCOUNT_NEVER_EXPIRES.equalsIgnoreCase(value)) {
                                // Let's set it to zero - this is equivalent: it means Never
                                attribute = AttributeBuilder.build(ADLdapUtil.ACCOUNT_EXPIRES, "0");
                            } else {
                                Date date = getJavaDateFromADTime(value);
                                attribute = AttributeBuilder.build(ADLdapUtil.ACCOUNT_EXPIRES, getADLdapDatefromJavaDate(date));
                            }
                        } else if (ADLdapUtil.PWD_LAST_SET.equalsIgnoreCase(attrName)) {
                            String value = (String) entry.getAttributes().get(ADLdapUtil.PWD_LAST_SET).get(0);
                            if ("0".equalsIgnoreCase(value)) {
                                attribute = AttributeBuilder.build(ADLdapUtil.PWD_LAST_SET, "0");
                            } else {
                                Date date = getJavaDateFromADTime(value);
                                attribute = AttributeBuilder.build(ADLdapUtil.PWD_LAST_SET, getADLdapDatefromJavaDate(date));
                            }
                        }
                    } catch (NamingException ex) {
                        log.warn("Special Attribute {0} of object class {1} can not be read from entry", attrName, oclass.getObjectClassValue());
                    }
                }
            }

            if (ObjectClass.GROUP.equals(oclass) && conn.getConfiguration().getGroupMemberAttribute().equalsIgnoreCase(attrName)) {
                if (ServerType.MSAD.equals(conn.getServerType())
                        || ServerType.MSAD_GC.equals(conn.getServerType())
                        || ServerType.MSAD_LDS.equals(conn.getServerType())) {
                    // Make sure we're not hitting AD large group issue
                    // see: http://msdn.microsoft.com/en-us/library/ms817827.aspx
                    if (entry.getAttributes().get("member;range=0-1499") != null) {
                        // we're in the limitation
                        attribute = AttributeBuilder.build(attrName, fetchGroupMembersByRange(conn, entry));
                    }
                }
                if (conn.getConfiguration().isGetGroupMemberId()) {
                    // create an extra _memberId attr for groups
                    builder.addAttribute(buildMemberIdAttribute(conn, attribute));
                }
            }
            if (attribute != null) {
                builder.addAttribute(attribute);
            }
        }

        return builder.build();
    }

    
    private PasswordDecryptor getPasswordDecryptor() {
        if (passwordDecryptor == null) {
            conn.getConfiguration().getPasswordDecryptionKey().access(new Accessor() {
                public void access(final byte[] decryptionKey) {
                    conn.getConfiguration().getPasswordDecryptionInitializationVector().access(new Accessor() {
                        public void access(byte[] decryptionIV) {
                            passwordDecryptor = new PasswordDecryptor(decryptionKey, decryptionIV);
                        }
                    });
                }
            });
        }
        assert passwordDecryptor != null;
        return passwordDecryptor;
    }
    
    /**
     * Creates a search filter which will filter to a given {@link ObjectClass}.
     * It will be composed of an optional filter to be applied before the object
     * class filters, the filters for all LDAP object classes for the given
     * {@code ObjectClass}, and an optional filter to be applied before the
     * object class filters.
     */
    private String getSearchFilter(String... optionalFilters) {
        StringBuilder builder = new StringBuilder();
        String ocFilter = getObjectClassFilter();
        int nonBlank = isBlank(ocFilter) ? 0 : 1;
        for (String optionalFilter : optionalFilters) {
            nonBlank += (isBlank(optionalFilter) ? 0 : 1);
        }
        if (nonBlank > 1) {
            builder.append("(&");
        }
        appendFilter(ocFilter, builder);
        for (String optionalFilter : optionalFilters) {
            appendFilter(optionalFilter, builder);
        }
        if (nonBlank > 1) {
            builder.append(')');
        }
        return builder.toString();
    }

    private String getObjectClassFilter() {
        StringBuilder builder = new StringBuilder();
        List<String> ldapClasses = conn.getSchemaMapping().getLdapClasses(oclass);
        boolean and = ldapClasses.size() > 1;
        if (and) {
            builder.append("(&");
        }
        for (String ldapClass : ldapClasses) {
            builder.append("(objectClass=");
            builder.append(ldapClass);
            builder.append(')');
        }
        if (and) {
            builder.append(')');
        }
        return builder.toString();
    }

    private static void appendFilter(String filter, StringBuilder toBuilder) {
        if (!isBlank(filter)) {
            String trimmedUserFilter = filter.trim();
            boolean enclose = filter.charAt(0) != '(';
            if (enclose) {
                toBuilder.append('(');
            }
            toBuilder.append(trimmedUserFilter);
            if (enclose) {
                toBuilder.append(')');
            }
        }
    }

    private List<String> getBaseDNs() {
        List<String> result;
        QualifiedUid container = options.getContainer();
        if (container != null) {
            result = singletonList(LdapSearches.findEntryDN(conn, container.getObjectClass(), container.getUid()));
        } else {
            result = Arrays.asList(baseDNs);
        }
        assert result != null;
        return result;
    }

    private LdapSearchStrategy getSearchStrategy() {
        LdapSearchStrategy strategy = null;

        Boolean useBlocks = conn.getConfiguration().getUseBlocks();
        Boolean usePagedResultsControl = conn.getConfiguration().getUsePagedResultControl();
        String pagingStrategy = conn.getConfiguration().getPagingStrategy();
        if (pagingStrategy == null) {
        	if (useBlocks != null && !useBlocks) {
        		pagingStrategy = LdapConfiguration.PAGING_STRATEGY_NONE;
        	} else if (usePagedResultsControl != null && usePagedResultsControl) {
        		pagingStrategy = LdapConfiguration.PAGING_STRATEGY_SPR;
        	} else if (usePagedResultsControl != null && !usePagedResultsControl) {
        		pagingStrategy = LdapConfiguration.PAGING_STRATEGY_VLV;
        	} else {
        		pagingStrategy = LdapConfiguration.PAGING_STRATEGY_AUTO;
        	}
        }
               
        int blockSize = conn.getConfiguration().getBlockSize();
        SortKey[] sortKeys = null;

        if (options.getSortKeys() != null && options.getSortKeys().length > 0) {
            if (conn.supportsControl(SortControl.OID)) {
                sortKeys = options.getSortKeys();
            }
        }
        
        if (options != null && options.getAllowPartialResults() != null && options.getAllowPartialResults() && 
        		options.getPagedResultsOffset() == null && options.getPagedResultsCookie() == null &&
        		options.getPageSize() == null) {
    		// Seach that allow partial results, no need for paging. Regardless of the configured strategy.
        	return new DefaultSearchStrategy(false, sortKeys);
    	}
        
        if (LdapConfiguration.PAGING_STRATEGY_NONE.equals(pagingStrategy)) {
        	// This may fail on a sizeLimit. But this is what has been configured so we are going to do it anyway.
        	log.ok("Selecting default search strategy because strategy setting is set to {0}", pagingStrategy);
        	strategy = new DefaultSearchStrategy(false, sortKeys);
        	
        } else if (LdapConfiguration.PAGING_STRATEGY_SPR.equals(pagingStrategy)) {
    		if (conn.supportsControl(PagedResultsControl.OID)) {
    			log.ok("Selecting SimplePaged search strategy because strategy setting is set to {0}", pagingStrategy);
    			strategy = new SimplePagedSearchStrategy(options, blockSize, sortKeys);
    		} else {
    			throw new ConfigurationException("Configured paging strategy "+pagingStrategy+", but the server does not support PagedResultsControl.");
    		}
    		
        } else if (LdapConfiguration.PAGING_STRATEGY_VLV.equals(pagingStrategy)) {
    		if (conn.supportsControl(VirtualListViewRequestControl.OID)) {
    			log.ok("Selecting VLV search strategy because strategy setting is set to {0}", pagingStrategy);
    			String vlvSortAttr = conn.getConfiguration().getVlvSortAttribute();
                String vlvSortOrderingRule = conn.getConfiguration().getVlvSortOrderingRule();
				strategy = new VlvIndexSearchStrategy(options, vlvSortAttr, vlvSortOrderingRule, blockSize);
    		} else {
    			throw new ConfigurationException("Configured paging strategy "+pagingStrategy+", but the server does not support VLV.");
    		}
    		
        } else if (LdapConfiguration.PAGING_STRATEGY_AUTO.equals(pagingStrategy)) {
        	if (options.getPagedResultsOffset() != null) {
        		// VLV is the only option here
        		if (conn.supportsControl(VirtualListViewRequestControl.OID)) {
        			log.ok("Selecting VLV search strategy because strategy setting is set to {0} and the request specifies an offset", pagingStrategy);
        			String vlvSortAttr = conn.getConfiguration().getVlvSortAttribute();
                    String vlvSortOrderingRule = conn.getConfiguration().getVlvSortOrderingRule();
    				strategy = new VlvIndexSearchStrategy(options, vlvSortAttr, vlvSortOrderingRule, blockSize);
        		} else {
        			throw new UnsupportedOperationException("Requested search from offset ("+options.getPagedResultsOffset()+"), but the server does not support VLV. Unable to execute the search.");
        		}
        	} else {
        		if (conn.supportsControl(PagedResultsControl.OID)) {
        			// SPR is usually a better choice if no offset is specified. Less overhead on the server.
        			log.ok("Selecting SimplePaged search strategy because strategy setting is set to {0} and the request does not specify an offset", pagingStrategy);
        			strategy = new SimplePagedSearchStrategy(options, blockSize, sortKeys);
        		} else if (conn.supportsControl(VirtualListViewRequestControl.OID)) {
        			String vlvSortAttr = conn.getConfiguration().getVlvSortAttribute();
                    String vlvSortOrderingRule = conn.getConfiguration().getVlvSortOrderingRule();
    				strategy = new VlvIndexSearchStrategy(options, vlvSortAttr, vlvSortOrderingRule, blockSize);        			
        		} else {
        			throw new UnsupportedOperationException("Requested paged search, but the server does not support VLV or PagedResultsControl. Unable to execute the search.");
        		}
        	}
        }
        
        if (strategy == null) {
        	// Failsafe. This should not be reached.
        	log.warn("Fallback to default strategy, strategy setting is set to {0}", pagingStrategy);
            strategy = new DefaultSearchStrategy(false, sortKeys);
        }
        return strategy;
    }

    private Set<String> getAttributesToGet(String[] attributesToGet) {
        Set<String> result;
        if (attributesToGet != null) {
            result = newCaseInsensitiveSet();
            result.addAll(Arrays.asList(attributesToGet));
            removeNonReadableAttributes(result);
            result.add(Name.NAME);
        } else {
            // This should include Name.NAME, so no need to include it explicitly.
            result = getAttributesReturnedByDefault(conn, oclass);
        }
        // Since Uid is not in the schema, but it is required to construct a ConnectorObject.
        result.add(Uid.NAME);
        // Our password is marked as readable because of sync(). We really can't return it from search.
        if (result.contains(OperationalAttributes.PASSWORD_NAME)) {
            log.warn("Reading passwords not supported");
            // throw new ConnectorException(conn.format("readingPasswordsNotSupported", null));
        }
        return result;
    }

    private void removeNonReadableAttributes(Set<String> attributes) {
        // Since the groups attributes are fake attributes, we don't want to
        // send them to LdapSchemaMapping. This, for example, avoid an (unlikely)
        // conflict with a custom attribute defined in the server schema.
        boolean ldapGroups = attributes.remove(LdapConstants.LDAP_GROUPS_NAME);
        boolean posixGroups = attributes.remove(LdapConstants.POSIX_GROUPS_NAME);
        conn.getSchemaMapping().removeNonReadableAttributes(oclass, attributes);
        if (ldapGroups) {
            attributes.add(LdapConstants.LDAP_GROUPS_NAME);
        }
        if (posixGroups) {
            attributes.add(LdapConstants.POSIX_GROUPS_NAME);
        }
    }

    private int getLdapSearchScope() {
        String scope = options.getScope();
        if (OperationOptions.SCOPE_OBJECT.equals(scope)) {
            return SearchControls.OBJECT_SCOPE;
        } else if (OperationOptions.SCOPE_ONE_LEVEL.equals(scope)) {
            return SearchControls.ONELEVEL_SCOPE;
        } else if (OperationOptions.SCOPE_SUBTREE.equals(scope) || scope == null) {
            return SearchControls.SUBTREE_SCOPE;
        } else {
            throw new IllegalArgumentException("Invalid search scope " + scope);
        }
    }
}
