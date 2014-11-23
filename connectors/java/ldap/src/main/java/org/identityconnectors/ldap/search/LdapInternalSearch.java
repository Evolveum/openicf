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
 */
package org.identityconnectors.ldap.search;

import static org.identityconnectors.common.StringUtil.isNotBlank;

import java.io.IOException;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.SearchControls;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.ldap.LdapConnection;
import org.identityconnectors.ldap.LdapConnection.ServerType;

/**
 * A class to perform an LDAP search against a {@link LdapConnection}.
 *
 * @author Andrei Badea
 */
public class LdapInternalSearch {

    private final LdapConnection conn;
    private final String filter;
    private final List<String> baseDNs;
    private final LdapSearchStrategy strategy;
    private final SearchControls controls;

    public LdapInternalSearch(LdapConnection conn, String filter, List<String> baseDNs, LdapSearchStrategy strategy, SearchControls controls) {
        this.conn = conn;
        this.filter = filter;
        this.baseDNs = baseDNs;
        this.strategy = strategy;
        this.controls = controls;
    }

    public void execute(LdapSearchResultsHandler handler) {
        String filter = blankAsAllObjects(this.filter);
        try {
            strategy.doSearch(conn, baseDNs, filter, controls, handler);
        } catch (IOException e) {
            throw new ConnectorException(e);
        } catch (PartialResultException e) {
            // AD issue: The default naming context on the DC is used  as the baseContexts, hence this PartialResultException.
            // Let's just silently catch it. It is thrown at the end of the search anyway...
            if (!(ServerType.MSAD.equals(conn.getServerType()) || ServerType.MSAD_GC.equals(conn.getServerType()))) {
                throw new ConnectorException(e);
            }
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }

    private static String blankAsAllObjects(String query) {
        return isNotBlank(query) ? query : "(objectClass=*)";
    }

    public static SearchControls createDefaultSearchControls() {
        SearchControls result = new SearchControls();
        result.setCountLimit(0);
        // Setting true to be consistent with the adapter. However, the
        // comment in the adapter that this flag causes the referrals to be
        // followed is wrong. Cf. http://java.sun.com/products/jndi/tutorial/ldap/misc/aliases.html.
        result.setDerefLinkFlag(true);
        result.setReturningObjFlag(false);
        result.setTimeLimit(0);
        return result;
    }

	public String getPagedResultsCookie() {
		return strategy.getPagedResultsCookie();
	}

	public int getRemainingPagedResults() {
		return strategy.getRemainingPagedResults();
	}
}
