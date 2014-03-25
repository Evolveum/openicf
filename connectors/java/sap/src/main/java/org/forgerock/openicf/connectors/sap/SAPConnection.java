/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 */
package org.forgerock.openicf.connectors.sap;

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import java.util.HashMap;
import java.util.Properties;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/**
 * Class to represent an SAP Connection
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 * @version $Revision$ $Date$
 */
public class SAPConnection {

    private SAPConfiguration configuration;
    private JCoDestination destination = null;
    private Properties connectionProperties = null;
    private ConnectorDestinationDataProvider cddProvider = null;
    /**
     * Setup logging for the {@link ScriptedSQLConnection}.
     */
    static Log log = Log.getLog(SAPConnection.class);

    public SAPConnection(SAPConfiguration configuration) {
        this.configuration = configuration;
        this.connectionProperties = initializeProperties(configuration);
        this.cddProvider = ConnectorDestinationDataProvider.getInstance();

        if (cddProvider.getDestinationProperties(this.configuration.getDestination()) == null) {
            cddProvider.createDestination(this.configuration.getDestination(), initializeProperties(this.configuration));
            log.info("Destination {0} has been created", this.configuration.getDestination());
        }
        try {
            destination = JCoDestinationManager.getDestination(this.configuration.getDestination());
        } catch (JCoException jcoe) {
            throw new ConnectorException(jcoe);
        }

    }

    /**
     * Release internal resources
     */
    public void dispose() {
        if ((this.destination != null) && (JCoContext.isStateful(this.destination))) {
            try {
                JCoContext.end(this.destination);
            } catch (JCoException jcoe) {
                //log
                throw new ConnectorException(jcoe);
            }
        }
    }

    /**
     * If internal connection is not usable, throw IllegalStateException
     */
    public void test() {
        try {
            this.destination.ping();
        } catch (JCoException jcoe) {
            throw new ConnectorException(jcoe);
        }
    }
    
    public JCoDestination getDestination(){
        return destination;
    }

    /**
     * Initialize the connection
     */
    private Properties initializeProperties(SAPConfiguration configuration) {
        final Properties connProperties = new Properties();
        connProperties.setProperty(DestinationDataProvider.JCO_USE_SAPGUI, "0");
        connProperties.setProperty(DestinationDataProvider.JCO_TRACE, configuration.getTrace());
        connProperties.setProperty(DestinationDataProvider.JCO_CPIC_TRACE, configuration.getCpicTrace());
        connProperties.setProperty(DestinationDataProvider.JCO_LANG, configuration.getLanguage());
        connProperties.setProperty(DestinationDataProvider.JCO_CLIENT, configuration.getClient());

        if (configuration.getSncMode().equals("0")) {
            connProperties.setProperty(DestinationDataProvider.JCO_SNC_MODE, configuration.getSncMode());
            connProperties.setProperty(DestinationDataProvider.JCO_USER, configuration.getUser());
            configuration.getPassword().access(new Accessor() {
                @Override
                public void access(char[] clearChars) {
                    connProperties.setProperty(DestinationDataProvider.JCO_PASSWD, new String(clearChars));
                }
            });
        } else { // using secure channel
            connProperties.setProperty(DestinationDataProvider.JCO_SNC_MODE, configuration.getSncMode());
            connProperties.setProperty(DestinationDataProvider.JCO_SNC_LIBRARY, configuration.getSncLibrary());
            connProperties.setProperty(DestinationDataProvider.JCO_SNC_MYNAME, configuration.getSncMyName());
            connProperties.setProperty(DestinationDataProvider.JCO_SNC_PARTNERNAME, configuration.getSncPartnerName());
            connProperties.setProperty(DestinationDataProvider.JCO_SNC_QOP, configuration.getSncQoP());
            connProperties.setProperty(DestinationDataProvider.JCO_X509CERT, configuration.getX509Cert());
//            connectionProperties.setProperty(DestinationDataProvider.JCO_SNC_SSO, configuration.getSncSSO());
        }

        if (configuration.isDirectConnection()) {
            connProperties.setProperty(DestinationDataProvider.JCO_SYSNR, configuration.getSystemNumber());
            connProperties.setProperty(DestinationDataProvider.JCO_ASHOST, configuration.getHost());
        } else {
            connProperties.setProperty(DestinationDataProvider.JCO_GROUP, configuration.getGroup());
            connProperties.setProperty(DestinationDataProvider.JCO_MSHOST, configuration.getMsHost());
            connProperties.setProperty(DestinationDataProvider.JCO_MSSERV, configuration.getMsServ());
            connProperties.setProperty(DestinationDataProvider.JCO_R3NAME, configuration.getR3Name());
        }

        if (configuration.getSapRouter() != null) {
            connProperties.setProperty(DestinationDataProvider.JCO_SAPROUTER, configuration.getSapRouter());
        }

        connProperties.setProperty(DestinationDataProvider.JCO_EXPIRATION_PERIOD, configuration.getExpirationPeriod());
        connProperties.setProperty(DestinationDataProvider.JCO_EXPIRATION_TIME, configuration.getExpirationTime());
        connProperties.setProperty(DestinationDataProvider.JCO_MAX_GET_TIME, configuration.getMaxGetTime());
        connProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,configuration.getPeakLimit());
        connProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY,configuration.getPoolCapacity());

        // Not used for now

//        connectionProperties.setProperty(DestinationDataProvider.JCO_ALIAS_USER,
//        connectionProperties.setProperty(DestinationDataProvider.JCO_AUTH_TYPE,
//        connectionProperties.setProperty(DestinationDataProvider.JCO_AUTH_TYPE_CONFIGURED_USER,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_AUTH_TYPE_CURRENT_USER,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_CODEPAGE,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_DELTA,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_DENY_INITIAL_PASSWORD,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_DEST,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_EXTID_DATA,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_EXTID_TYPE,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_GETSSO2,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_GWHOST,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_GWSERV,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_LCHECK,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_MYSAPSSO2,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_PCS,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_REPOSITORY_DEST,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_REPOSITORY_PASSWD,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_REPOSITORY_ROUNDTRIP_OPTIMIZATION,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_REPOSITORY_SNC,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_REPOSITORY_USER,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_TPHOST,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_TPNAME,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_TYPE,configuration);
//        connectionProperties.setProperty(DestinationDataProvider.JCO_USER_ID,configuration);


        return connProperties;
    }

    static class ConnectorDestinationDataProvider implements DestinationDataProvider {

        private DestinationDataEventListener eL;
        private HashMap<String, Properties> connectorProps = new HashMap<String, Properties>();
        private static ConnectorDestinationDataProvider instance = null;

        public ConnectorDestinationDataProvider() {
        }

        public Properties getDestinationProperties(String destinationName) {
            return connectorProps.get(destinationName);
        }

        public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
            this.eL = eventListener;
        }

        public boolean supportsEvents() {
            return true;
        }

        public static synchronized ConnectorDestinationDataProvider getInstance() {
            if (instance == null) {
                instance = new ConnectorDestinationDataProvider();
            }
            try {
                com.sap.conn.jco.ext.Environment.registerDestinationDataProvider(instance);
            } catch (IllegalStateException providerAlreadyRegisteredException) {
                //somebody else registered its implementation, 
            }
            return instance;
        }

        public void createDestination(String destination, Properties connectionProperties) {
            connectorProps.put(destination, connectionProperties);
        }
    }
}