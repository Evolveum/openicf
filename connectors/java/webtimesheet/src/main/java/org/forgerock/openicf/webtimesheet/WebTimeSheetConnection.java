/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.openicf.webtimesheet;

/**
 * Class to represent a WebTimeSheet Connection
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class WebTimeSheetConnection {

    

        private final WebTimeSheetConfiguration _configuration;
    //private RTAPIClient rtapi;
    private RepliConnectClient rcc;
    
    //TODO create a _SESSION_ with RepliConnect API begin/end commands
    

/*
    public WebTimeSheetConnection(WebTimeSheetConfiguration cfg) {
        _configuration = cfg;
        rtapi = new RTAPIClient(_configuration.getURLProperty(),
                _configuration.getAppNameProperty(),
                _configuration.getAppPasswordProperty(),
                _configuration.getAdminUidProperty(),
                _configuration.getAdminPasswordProperty());

    }
 *
 *
 */

     public WebTimeSheetConnection(WebTimeSheetConfiguration cfg) {
        _configuration = cfg;
        int port = Integer.parseInt(_configuration.getWtsPort());
        rcc = new RepliConnectClient(_configuration.getWtsHost(),port,_configuration.getWtsURI(),_configuration.getAdminUid(),_configuration.getAdminPassword());



    }

    /**
     * Release internal resources
     */
    public void dispose() {
       rcc = null;
    }

    /**
     * If internal connection is not usable, throw IllegalStateException
     */
    public void test() {
       rcc.testConnection();
    }

    /**
     * get client
     */
    public RepliConnectClient getClient() {
       return rcc;
    }

}
