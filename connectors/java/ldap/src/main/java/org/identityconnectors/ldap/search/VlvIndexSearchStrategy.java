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

import com.sun.jndi.ldap.Ber;
import com.sun.jndi.ldap.BerDecoder;

import static org.identityconnectors.common.StringUtil.isNotBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.BasicControl;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.SortControl;
import javax.naming.ldap.SortResponseControl;

import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.controls.VirtualListViewRequestControl;
import org.forgerock.opendj.ldap.controls.VirtualListViewResponseControl;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SortKey;


public class VlvIndexSearchStrategy extends LdapSearchStrategy {

    private static Log log;

    private OperationOptions options;
    private final String vlvDefaultSortAttr;
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

    public VlvIndexSearchStrategy(OperationOptions options, String vlvDefaultSortAttr, int blockSize) {
    	this.options = options;
        this.vlvDefaultSortAttr = isNotBlank(vlvDefaultSortAttr) ? vlvDefaultSortAttr : "uid";
        this.blockSize = blockSize;
    }

    @Override
    public void doSearch(LdapContext initCtx, List<String> baseDNs, String query, SearchControls searchControls, LdapSearchResultsHandler handler) throws IOException, NamingException {
        getLog().ok("Searching in {0} with filter {1} and {2}", baseDNs, query, searchControlsToString(searchControls));

        Iterator<String> baseDNIter = baseDNs.iterator();
        boolean proceed = true;

        LdapContext ctx = initCtx.newInstance(null);
        try {
            while (baseDNIter.hasNext() && proceed) {
                proceed = searchBaseDN(ctx, baseDNIter.next(), query, searchControls, handler);
            }
        } finally {
            ctx.close();
        }
    }

    private boolean searchBaseDN(LdapContext ctx, String baseDN, String query, SearchControls searchControls, LdapSearchResultsHandler handler) throws IOException, NamingException {
        getLog().ok("Searching in {0}", baseDN);
        
        index = 1;
        if (options != null && options.getPagedResultsOffset() != null) {
        	index = options.getPagedResultsOffset() + 1;
        }
        Integer numberOfEntriesToReturn = null; // null means "as many as there are"
        if (options != null && options.getPageSize() != null) {
        	numberOfEntriesToReturn = options.getPageSize();
        }
        String vlvSortAttr = vlvDefaultSortAttr;
        if (options != null && options.getSortKeys() != null && options.getSortKeys().length > 0) {
        	if (options.getSortKeys().length > 1) {
        		log.warn("Multiple sort keys are not supported");
        	}
        	SortKey sortKey = options.getSortKeys()[0];
        	vlvSortAttr = sortKey.getField();
        }
        
        lastListSize = 0;
        cookie = new byte[0];

        String lastResultName = null;
        int numberOfResutlsReturned = 0;

        for (;;) {
            SortControl sortControl = new SortControl(vlvSortAttr, Control.CRITICAL);
            
            int afterCount = blockSize - 1;
            if (numberOfEntriesToReturn != null && (numberOfResutlsReturned + afterCount + 1 > numberOfEntriesToReturn)) {
            	afterCount = numberOfEntriesToReturn - numberOfResutlsReturned - 1;
            }
            
            VirtualListViewRequestControl vlvreq = VirtualListViewRequestControl.newOffsetControl(Control.CRITICAL, index, lastListSize, 0, afterCount, ByteString.valueOf(cookie));
            BasicControl vlvControl = new BasicControl(VirtualListViewRequestControl.OID, Control.CRITICAL, vlvreq.getValue().toByteArray());
            
            getLog().ok("New search: target = {0}, afterCount = {1}", index, afterCount);
            ctx.setRequestControls(new Control[] { sortControl, vlvControl });

            // Need to process the response controls, which are available after
            // all results have been processed, before sending anything to the caller
            // (because processing the response controls might throw exceptions that
            // invalidate anything we might have sent otherwise).
            // So storing the results before actually sending them to the handler.
            List<SearchResult> resultList = new ArrayList<SearchResult>(blockSize);

            NamingEnumeration<SearchResult> results = ctx.search(baseDN, query, searchControls);
            try {
                while (results.hasMore()) {
                    SearchResult result = results.next();

                    boolean overlap = false;
                    if (lastResultName != null) {
                        if (lastResultName.equals(result.getName())) {
                            getLog().warn("Working around rounding error overlap at index " + index);
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

            processResponseControls(ctx.getResponseControls());

            SearchResult result = null;
            Iterator<SearchResult> resultIter = resultList.iterator();
            while (resultIter.hasNext()) {
                result = resultIter.next();
                index++;
                numberOfResutlsReturned++;
                if (!handler.handle(baseDN, result)) {
                    return false;
                }
            }
            if (result != null) {
                lastResultName = result.getName();
            }

            if (index > lastListSize) {
                break;
            }
            if (numberOfEntriesToReturn != null && numberOfEntriesToReturn <= numberOfResutlsReturned) {
            	break;
            }

            // DSEE seems to only have a single VLV index (although it claims to support more).
            // It returns at the server content count the sum of sizes of all indexes,
            // but it only returns the entries in the base context we are asking for.
            // So, in this case, index will never reach lastListSize. To avoid an infinite loop,
            // ending search if we received no results in the last iteration.
            if (resultList.isEmpty()) {
                getLog().warn("Ending search because received no results");
                break;
            }
        }
        return true;
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
                    byte[] value = control.getEncodedValue();
                    if ((value != null) && (value.length > 0)) {
                        BerDecoder decoder = new BerDecoder(value, 0, value.length);
                        
                        try {
                            decoder.parseSeq(null);
                            int offset = decoder.parseInt();
                            lastListSize = decoder.parseInt();
                            getLog().ok("Response control: lastListSize = {0}", lastListSize);
                            int code = decoder.parseEnumeration();
                            if ((decoder.bytesLeft() > 0) && (decoder.peekByte() == Ber.ASN_OCTET_STR)) {
                                cookie = decoder.parseOctetString(Ber.ASN_OCTET_STR, null);
                            }
                            if (code != 0) {
                                throw new NamingException("The view operation has failed on LDAP server");
                            }
                        } catch (IOException ex) {
                            getLog().error("Can't decode response control");
                        }
                    }
                }
            }
        }
    }
}
