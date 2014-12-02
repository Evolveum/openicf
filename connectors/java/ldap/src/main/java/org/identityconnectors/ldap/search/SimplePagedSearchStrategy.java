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
 * "Portions Copyrighted 2014 ForgeRock AS"
 * Portions Copyrighted 2014 Evolveum
 */
package org.identityconnectors.ldap.search;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.naming.LimitExceededException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.naming.ldap.SortControl;

import org.identityconnectors.common.Base64;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.ldap.LdapConnection;

public class SimplePagedSearchStrategy extends LdapSearchStrategy {

    private static final Log log = Log.getLog(SimplePagedSearchStrategy.class);

    private OperationOptions options;
    private final int defaultPageSize;
    private final SortKey[] sortKeys;
    private byte[] cookie = null;
    private int lastListSize = -1;

    public SimplePagedSearchStrategy(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
        this.sortKeys = null;
    }
    
    public SimplePagedSearchStrategy(OperationOptions options, int defaultPageSize, SortKey[] sortKeys) {
    	this.options = options;
        this.defaultPageSize = defaultPageSize;
        this.sortKeys = sortKeys;
    }

    @Override
    public void doSearch(LdapConnection conn, List<String> baseDNs, String query, SearchControls searchControls, LdapSearchResultsHandler handler) throws IOException, NamingException {
        log.ok("Searching in {0} with filter {1} and {2}", baseDNs, query, searchControlsToString(searchControls));

        LdapContext ctx = conn.getInitialContext().newInstance(null);
        SortControl sortControl = null;
        
        if (sortKeys != null && sortKeys.length > 0){
            javax.naming.ldap.SortKey[] skis = new javax.naming.ldap.SortKey[sortKeys.length];
            for(int i = 0; i < sortKeys.length; i++){
                skis[i] = new javax.naming.ldap.SortKey(sortKeys[i].getField(),sortKeys[i].isAscendingOrder(),null);
            }
            // We don't want to make this critical... better return unsorted results than nothing.
            sortControl = new SortControl(skis, Control.NONCRITICAL);
        }
        
        int pageSize = defaultPageSize;
        int offset = 0;
        if (options != null && options.getPagedResultsOffset() != null) {
        	offset = options.getPagedResultsOffset();
        	if (offset != 0) {
        		log.info("Inefficient search using SimplePaged control and offset {0}",offset);
        	}
        }
        try {
            Iterator<String> baseDNIter = baseDNs.iterator();
            boolean proceed = true;

            while (baseDNIter.hasNext() && proceed) {
                String baseDN = baseDNIter.next();
                cookie = null;
                if (options != null && options.getPagedResultsCookie() != null) {
                	cookie = Base64.decode(options.getPagedResultsCookie());
                }
                pageSize = defaultPageSize;
                int numberOfResutlsHandled = 0;
                int numberOfResultsSkipped = 0;
                do {
                	if (options != null && options.getPageSize() != null && 
                			((numberOfResutlsHandled + numberOfResultsSkipped + pageSize) > offset + options.getPageSize())) {
                		pageSize = offset + options.getPageSize() - (numberOfResutlsHandled + numberOfResultsSkipped);
                    }
                    if (sortControl != null) {
                        ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.CRITICAL), sortControl});
                    } else {
                        ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
                    }
                    if (log.isOk()) {
                    	log.ok("LDAP search request: PagedResults( pageSize = {0}, cookie = {1} )", 
                    			pageSize, Base64.encode(cookie));
                    }
                    int responseResultCount = 0;
                    NamingEnumeration<SearchResult> results = null;
                    try {
                    	results = ctx.search(baseDN, query, searchControls);
                        while (proceed && results.hasMore()) {
                        	responseResultCount++;
                        	SearchResult searchResult = results.next();
                        	if (offset > numberOfResultsSkipped) {
                        		numberOfResultsSkipped++;
                        	} else {
	                        	numberOfResutlsHandled++;
	                            proceed = handler.handle(baseDN, searchResult);
	                            if (!proceed) {
	                            	log.ok("Ending search because handler returned false");
	                            }
                        	}
                        }
                    } catch (PartialResultException e) {
                        log.ok("PartialResultException caught: {0}",e.getRemainingName());
                        if (results != null) {
                        	results.close();
                        }
                    } catch (NamingException e) {
                    	log.error("Unexpected search exception: {0}", e.getMessage(), e);
                    	throw e;
                    } catch (RuntimeException e) {
                    	log.error("Unexpected search exception: {0}", e.getMessage(), e);
                    	throw e;
                    }
                    PagedResultsResponseControl pagedControlResponse = getPagedControlResponse(ctx.getResponseControls());
                    if (pagedControlResponse != null) {
	                    cookie = pagedControlResponse.getCookie();
	                    lastListSize = pagedControlResponse.getResultSize();
                    } else {
                    	cookie = null;
	                    lastListSize = -1;
                    }
                    if (log.isOk()) {
                    	log.ok("LDAP search response: {0} results returned, PagedResults( resultSize = {1}, cookie = {2} )", responseResultCount, 
                    			pagedControlResponse==null?"noControl":pagedControlResponse.getResultSize(),
                    			pagedControlResponse==null?"noControl":Base64.encode(pagedControlResponse.getCookie()));
                    }
                    if (responseResultCount == 0) {
                    	// Zero results returned. This is either a hidded error or end of search.
                    	log.warn("Zero results returned from paged search");
                    	break;
                    }
                    if (!proceed) {
                    	break;
                    }
                    if (options != null && options.getPageSize() != null && 
                			((numberOfResutlsHandled + numberOfResultsSkipped) >= offset + options.getPageSize())) {
                    	break;
                    }
                } while (cookie != null);
            }
        } catch (LimitExceededException e) {
        	// This should not happen!
        	log.error("Server size limit exceeded even though SimplePaged control was used (page size {0})", pageSize, e);
        	throw e;
        } finally {
            ctx.close();
        }
    }

    private PagedResultsResponseControl getPagedControlResponse(Control[] controls) {
        if (controls != null) {
            for (Control control : controls) {
                if (control instanceof PagedResultsResponseControl) {
                	return (PagedResultsResponseControl) control;
                }
            }
        }
        return null;
    }
    
    @Override
	public int getRemainingPagedResults() {
		return lastListSize;
	}
        
    @Override
	public String getPagedResultsCookie() {
		if (cookie == null) {
			return null;
		}
		return Base64.encode(cookie);
	}
}
