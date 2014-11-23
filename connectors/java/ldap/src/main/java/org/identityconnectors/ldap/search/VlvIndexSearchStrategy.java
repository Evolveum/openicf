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
 *  Portions Copyrighted 2013-2014 ForgeRock AS
 *  Portions Copyrighted 2014 Evolveum
 */
package org.identityconnectors.ldap.search;

import static org.identityconnectors.common.StringUtil.isNotBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.SortControl;
import javax.naming.ldap.SortResponseControl;

import org.identityconnectors.common.Base64;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.ldap.LdapConnection;


public class VlvIndexSearchStrategy extends LdapSearchStrategy {

    private static Log log;

    private OperationOptions options;
    private final String vlvDefaultSortAttr;
    private String sortOrderingRuleID;
    private final int blockSize;

    private int index;
    private int lastListSize;
    private byte[] cookie;

    static synchronized void setLog(Log log) {
        VlvIndexSearchStrategy.log = log;
    }

    synchronized static Log getLog() {
        if (log == null) {
            log = Log.getLog(VlvIndexSearchStrategy.class);
        }
        return log;
    }

    public VlvIndexSearchStrategy(OperationOptions options, String vlvDefaultSortAttr, String sortOrderingRuleID, int blockSize) {
    	this.options = options;
        this.vlvDefaultSortAttr = isNotBlank(vlvDefaultSortAttr) ? vlvDefaultSortAttr : "uid";
        this.sortOrderingRuleID = sortOrderingRuleID;
        this.blockSize = blockSize;
    }

    @Override
    public void doSearch(LdapConnection conn, List<String> baseDNs, String query, SearchControls searchControls, LdapSearchResultsHandler handler) throws IOException, NamingException {
        getLog().ok("Searching in {0} with filter {1} and {2}", baseDNs, query, searchControlsToString(searchControls));

        Iterator<String> baseDNIter = baseDNs.iterator();
        boolean proceed = true;

        LdapContext ctx = conn.getInitialContext().newInstance(null);
        try {
            while (baseDNIter.hasNext() && proceed) {
                proceed = searchBaseDN(conn, ctx, baseDNIter.next(), query, searchControls, handler);
            }
        } finally {
            ctx.close();
        }
    }

    private boolean searchBaseDN(LdapConnection conn, LdapContext ctx, String baseDN, String query, SearchControls searchControls, LdapSearchResultsHandler handler) throws IOException, NamingException {
        getLog().ok("New VLV search in {0}", baseDN);
        
        boolean continueFlag = true;
        index = 1;
        if (options != null && options.getPagedResultsOffset() != null) {
        	index = options.getPagedResultsOffset();
        }
        Integer numberOfEntriesToReturn = null; // null means "as many as there are"
        if (options != null && options.getPageSize() != null) {
        	numberOfEntriesToReturn = options.getPageSize();
        }
        String vlvSortAttr = vlvDefaultSortAttr;
        boolean ascendingOrder = true;
        if (options != null && options.getSortKeys() != null && options.getSortKeys().length > 0) {
        	if (options.getSortKeys().length > 1) {
        		log.warn("Multiple sort keys are not supported");
        	}
        	SortKey sortKey = options.getSortKeys()[0];
        	vlvSortAttr = sortKey.getField();
        	ascendingOrder = sortKey.isAscendingOrder();
        }
        
        lastListSize = 0;
        cookie = null;
        if (options != null && options.getPagedResultsCookie() != null) {
        	cookie = Base64.decode(options.getPagedResultsCookie());
        }

        String lastResultName = null;
        int numberOfResutlsReturned = 0;

        for (;;) {
			javax.naming.ldap.SortKey ldapSortKey = new javax.naming.ldap.SortKey(vlvSortAttr, ascendingOrder, sortOrderingRuleID);
            SortControl sortControl = new SortControl(new javax.naming.ldap.SortKey[]{ldapSortKey}, Control.CRITICAL);
            
            int afterCount = blockSize - 1;
            if (numberOfEntriesToReturn != null && (numberOfResutlsReturned + afterCount + 1 > numberOfEntriesToReturn)) {
            	afterCount = numberOfEntriesToReturn - numberOfResutlsReturned - 1;
            }
            
            VirtualListViewRequestControl vlvControl = new VirtualListViewRequestControl(0, afterCount, index, lastListSize, cookie, Control.CRITICAL);
            
            ctx.setRequestControls(new Control[] { sortControl, vlvControl });

            // Need to process the response controls, which are available after
            // all results have been processed, before sending anything to the caller
            // (because processing the response controls might throw exceptions that
            // invalidate anything we might have sent otherwise).
            // So storing the results before actually sending them to the handler.
            List<SearchResult> resultList = new ArrayList<SearchResult>(blockSize);

            if (getLog().isOk()) {
            	getLog().ok("LDAP search request: VLV( target = {0}, lastListSize = {1}, afterCount = {2}, cookie = {3} ),"
            			+ " SSS( attr = {4}, ascending = {5}, ordering = {6} )", 
            			index, lastListSize, afterCount, Base64.encode(cookie), 
            			vlvSortAttr, ascendingOrder, sortOrderingRuleID);
            }
            NamingEnumeration<SearchResult> results = ctx.search(baseDN, query, searchControls);
            int resultCount = 0;
            try {
                while (results.hasMore()) {
                    SearchResult result = results.next();
                    resultCount++;

                    boolean overlap = false;
                    if (lastResultName != null) {
                        if (lastResultName.equals(result.getName())) {
                            getLog().warn("Working around rounding error overlap at index {0} (name={1})", index, lastResultName);
                            overlap = true;
                        }
                        lastResultName = null;
                    }

                    if (!overlap) {
                        resultList.add(result);
                    }
                }
            } finally {
                results.close();
            }
            getLog().ok("LDAP search response: {0} results returned, reduced to {1}", resultCount, resultList.size());

            processResponseControls(ctx.getResponseControls());

            SearchResult result = null;
            Iterator<SearchResult> resultIter = resultList.iterator();
            while (resultIter.hasNext()) {
                result = resultIter.next();
                index++;
                numberOfResutlsReturned++;
                if (!handler.handle(baseDN, result)) {
                	getLog().ok("Ending VLV search because handler returned false");
                	continueFlag = false;
                    break;
                }
            }
            if (!continueFlag) {
            	break;
            }
            if (result != null) {
                lastResultName = result.getName();
            }
            getLog().ok("Handling of results completed, {0} resutls handled, index {1} (lastResultName={2})", numberOfResutlsReturned, index, lastResultName);

            if (index > lastListSize) {
            	getLog().ok("Ending VLV search because index ({0}) went over list size ({1})", index, lastListSize);
                break;
            }
            if (numberOfEntriesToReturn != null && numberOfEntriesToReturn <= numberOfResutlsReturned) {
            	getLog().ok("Ending VLV search because enough entries already returned");
            	break;
            }

            // DSEE seems to only have a single VLV index (although it claims to support more).
            // It returns at the server content count the sum of sizes of all indexes,
            // but it only returns the entries in the base context we are asking for.
            // So, in this case, index will never reach lastListSize. To avoid an infinite loop,
            // ending search if we received no results in the last iteration.
            if (resultList.isEmpty()) {
                getLog().warn("Ending VLV search because received no results");
                break;
            }
        }
        
        if (options != null && options.getPagedResultsOffset() != null) {
        	// If there was an offset then it is likely that this search will continue. Therefore do NOT close the search yet.
        } else  {
        	// Close the connection so the server can free any allocated resources.
        	// The connection will automatically reconnect on the next use.
        	conn.close();
        }
        
        return continueFlag;
    }

    private void processResponseControls(Control[] controls) throws NamingException {
        if (controls != null) {
            for (Control control : controls) {
                if (control instanceof SortResponseControl) {
                    SortResponseControl sortControl = (SortResponseControl) control;
                    if (!sortControl.isSorted() || (sortControl.getResultCode() != 0)) {
                        throw sortControl.getException();
                    }
                }
                if (control.getID().equalsIgnoreCase(VirtualListViewResponseControl.OID)) {
                	try {
                		VirtualListViewResponseControl vlvResponse = new VirtualListViewResponseControl(control.getID(), control.isCritical(), control.getEncodedValue());
                		byte[] value = control.getEncodedValue();
                        int offset = vlvResponse.getTargetPosition();
                        lastListSize = vlvResponse.getContentCount();
                        int code = vlvResponse.getVirtualListViewResult();
                        cookie = vlvResponse.getContextID();
                        if (getLog().isOk()) {
                        	getLog().ok("Response control: offset = {0}, lastListSize = {1}, cookie = {2}", offset, lastListSize, Base64.encode(cookie));
                        }
                        if (code != 0) {
                            throw new NamingException("The view operation has failed on LDAP server, error="+code);
                        }
                    } catch (IOException ex) {
                        getLog().error("Can't decode response control");
                    }
                }
            }
        }
    }

	@Override
	public String getPagedResultsCookie() {
		if (cookie == null) {
			return null;
		}
		return Base64.encode(cookie);
	}

	@Override
	public int getRemainingPagedResults() {
		return lastListSize;
	}
    
    
}
