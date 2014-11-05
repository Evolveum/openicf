/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
* Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 * 
* The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
* You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html See the License for the specific
 * language governing permission and limitations under the License.
 * 
* When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://forgerock.org/license/CDDLv1.0.html If
 * applicable, add the following below the CDDL Header, with the fields enclosed
 * by brackets [] replaced by your own identifying information: " Portions
 * Copyrighted [year] [name of copyright owner]"
 * 
*/
package org.identityconnectors.ldap.search;

import java.io.IOException;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.PartialResultException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.naming.ldap.SortControl;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.SortKey;
import static org.identityconnectors.ldap.search.LdapSearchStrategy.searchControlsToString;

/**
 *
 * @author gael.allioux@forgerock.com
 */
public class PagedSearchStrategy extends LdapSearchStrategy {

    private static final Log log = Log.getLog(SimplePagedSearchStrategy.class);

    private final int pageSize;
    private final int pagedResultsOffset;
    private final String pagedResultsCookie;
    private final SearchResultsHandler searchResultHandler;
    private final SortKey[] sortKeys;

    public PagedSearchStrategy(int pageSize, String pagedResultsCookie, int pagedResultsOffset, SearchResultsHandler searchResultHandler, SortKey[] sortKeys) {
        this.pageSize = pageSize;
        this.pagedResultsOffset = pagedResultsOffset;
        this.pagedResultsCookie = pagedResultsCookie;
        this.searchResultHandler = searchResultHandler;
        this.sortKeys = sortKeys;
    }

    @Override
    public void doSearch(LdapContext initCtx, List<String> baseDNs, String query, SearchControls searchControls, LdapSearchResultsHandler handler) throws IOException, NamingException {
        log.ok("Searching in {0} with filter {1} and {2}", baseDNs, query, searchControlsToString(searchControls));

        LdapContext ctx = initCtx.newInstance(null);
        String returnedCookie = null;
        int context = 0;
        int records = 0;
        int remainingResults = -1;
        boolean proceed = true;
        boolean needMore = false;
        byte[] cookie = null;
        PagedResultsResponseControl pagedControl = null;
        SortControl sortControl = null;
        
        if (sortKeys != null && sortKeys.length > 0){
            javax.naming.ldap.SortKey[] skis = new javax.naming.ldap.SortKey[sortKeys.length];
            for(int i = 0; i < sortKeys.length; i++){
                skis[i] = new javax.naming.ldap.SortKey(sortKeys[i].getField(),sortKeys[i].isAscendingOrder(),null);
            }
            // We don't want to make this critical... better return unsorted results than nothing.
            sortControl = new SortControl(skis, Control.NONCRITICAL);
        }

        if (StringUtil.isNotBlank(pagedResultsCookie)) {
            // we need to determine which base context we're dealing with...
            // The cookie value is <base64 encoded LDAP cookie>:<index in baseDNs>
            String[] split = pagedResultsCookie.split(":", 2);
            // bit of sanity check...
            if (split.length == 2) {
                try{
                    cookie = Base64.decode(split[0]);
                } catch (RuntimeException e) {
                    throw new ConnectorException("PagedResultsCookie is not properly encoded",e);
                }
                context = Integer.valueOf(split[1]);
            } else {
                throw new ConnectorException("PagedResultsCookie is not properly formatted");
            }
        }

        try {
            do {
                if (sortControl != null) {
                    ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize - records, cookie, Control.CRITICAL), sortControl});
                } else {
                    ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize - records, cookie, Control.CRITICAL)});
                }
                NamingEnumeration<SearchResult> results = ctx.search(baseDNs.get(context), query, searchControls);
                while (proceed && results.hasMore()) {
                    SearchResult res = results.next();
                    records++;
                    if (records > pagedResultsOffset) {
                        proceed = handler.handle(baseDNs.get(context), res);
                    }
                }
                if ((records < pageSize) && (context + 1 < baseDNs.size())) {
                    needMore = true;
                    context++;
                    cookie = null;
                } else {
                    needMore = false;
                    pagedControl = getPagedControl(ctx.getResponseControls());
                    if (pagedControl != null) {
                        cookie = pagedControl.getCookie();
                        if (pagedControl.getResultSize() > 0) {
                            remainingResults = pagedControl.getResultSize();
                        }
                    }
                }

                results.close();
            } while (needMore);
        } catch (OperationNotSupportedException e) {
            log.ok("OperationNotSupportedException caught: {0}. Check the Cookie validity", e.getRemainingName());
            throw new ConnectorException("Operation Not Supported. Bad cookie");
        } catch (PartialResultException e) {
            log.ok("PartialResultException caught: {0}", e.getRemainingName());
        } finally {
            ctx.close();
        }
        if (cookie != null) {
            returnedCookie = Base64.encode(cookie).concat(":" + context);
        }
        searchResultHandler.handleResult(new org.identityconnectors.framework.common.objects.SearchResult(returnedCookie, remainingResults));
    }

    private byte[] getResponseCookie(Control[] controls) {
        if (controls != null) {
            for (Control control : controls) {
                if (control instanceof PagedResultsResponseControl) {
                    PagedResultsResponseControl pagedControl = (PagedResultsResponseControl) control;
                    pagedControl.getResultSize();
                    return pagedControl.getCookie();
                }
            }
        }
        return null;
    }

    private PagedResultsResponseControl getPagedControl(Control[] controls) {
        if (controls != null) {
            for (Control control : controls) {
                if (control instanceof PagedResultsResponseControl) {
                    return (PagedResultsResponseControl) control;
                }
            }
        }
        return null;
    }

}
