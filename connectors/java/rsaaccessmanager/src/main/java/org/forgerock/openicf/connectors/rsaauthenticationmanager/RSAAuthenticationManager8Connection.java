/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
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
package org.forgerock.openicf.connectors.rsaauthenticationmanager;

import com.rsa.admin.SearchRealmsCommand;
import com.rsa.admin.data.IdentitySourceDTO;
import com.rsa.admin.data.RealmDTO;
import com.rsa.admin.data.SecurityDomainDTO;
import com.rsa.command.ClientSession;
import com.rsa.command.CommandException;
import com.rsa.command.CommandTargetPolicy;
import com.rsa.command.Connection;
import com.rsa.command.ConnectionFactory;
import com.rsa.command.exception.DataNotFoundException;
import com.rsa.command.exception.InsufficientPrivilegeException;
import com.rsa.command.exception.InvalidArgumentException;
import com.rsa.common.search.Filter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.misc.BASE64Encoder;
import javax.crypto.Cipher;


import org.identityconnectors.common.logging.Log;

/**
 * Class to represent a RSA AM 8 Connector Connection.
 *
 * @author Alex Babeanu (ababeanu@nulli.com)
 * www.nulli.com - Identity Solution Architects
 * 
 * @version 1.1
 * @since 1.0
 */
public class RSAAuthenticationManager8Connection {

    /**
     * An instance of the RSA Configuration
     */
    private RSAAuthenticationManager8Configuration configuration;
    /**
     * An instance of a Connection to the RSA Server
     */
    private ClientSession RSAsession;
    /**
     * The RSA Security Domain
     */
    private final SecurityDomainDTO domain;
    /**
     * The RSA IS Source
     */
    private final IdentitySourceDTO idSource;
    /**
     * Setup logging for the {@link RSAAuthenticationManager8Connection}.
     */
    private static final Log logger = Log.getLog(RSAAuthenticationManager8Connection.class);

    /**
     * Constructor of RSAAuthenticationManager8Connection class.
     *
     * @param configuration the actual {@link RSAAuthenticationManager8Configuration}
     */
    public RSAAuthenticationManager8Connection(RSAAuthenticationManager8Configuration configuration) {
        this.configuration = configuration;
        // Validate the configuration
        try {
            logger.info("Validating configuration...");
            this.configuration.validate();
            logger.info("The configuration is valid.");
        } catch (IllegalArgumentException iae) {
            logger.error("The given configuration failed validation with exception: " + iae.getMessage() + " - " + iae.getCause());
        }
        
        // Create a config.properties file for RSA SDK, uncommente if needed.
	/* */
        if (!(this.configuration.getConfigFileExists())) {
            logger.info("Missing configuration file, creating a new one...");
            createConfigPropertiesFile();
            logger.info("Config file created.");
	}
	/* */
        
        // establish a connected session with given credentials
        logger.info("Attempting to connect to the RSA AM Server...");
        Connection conn = ConnectionFactory.getConnection("CommandAPIConnection"); // "CommandAPIConnection"  // createConfigProperties()
        logger.info ("Connection instantiated. Attempting to login...");
        String PlainPwd = RSAAuthenticationManager8Utils.getPlainPassword(this.configuration.getUserMgrPwd());
        try {
            this.RSAsession = conn.connect(this.configuration.getUserMgrPrincipal(), PlainPwd);
            logger.info("Connection succeeded!");
        } catch (CommandException e) {
            logger.error("Failed to connect to the RSA server. Error: " + e.getMessage() + " key: " +e.getMessageKey() + " cause: " + e.getCause() 
                         + "\n User: " + this.configuration.getUserMgrPrincipal() + " - Pwd: " + PlainPwd);
            throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException(e);
        }
        // make all commands execute using this target automatically
        CommandTargetPolicy.setDefaultCommandTarget(RSAsession);
        
        // Fetch the Security Domain DTO and search for the ID Source by querying the RSA Security realm
        // First determine whether the connector was instantiated with a specific Domain ID, else use
        // the default ("SystemDomain").
        String DomainString = null;
        String CfgDomainString = this.configuration.getSecurityDomain();
        if (CfgDomainString == null) 
            DomainString = RSAAuthenticationManager8Configuration.RSA_DOMAIN;
        else {
            logger.info ("Using Security Domain '{0}'...", CfgDomainString);
            DomainString = CfgDomainString;
        }
        
        SearchRealmsCommand searchRealmCmd = new SearchRealmsCommand();
        searchRealmCmd.setFilter(Filter.equal(RealmDTO.NAME_ATTRIBUTE, DomainString));
        logger.info("Searching for RSA SecurityDomain with filter: " + searchRealmCmd.getFilter().toString());

        try {
            searchRealmCmd.execute();
        } catch (InsufficientPrivilegeException e) {
            logger.error("Insufficient Privileges to create Principal: " + e.getMessage() + " User ID: " + configuration.getCmdclientUser());
        } catch (DataNotFoundException e) {
            logger.error("Could not find the RSA Security Domain: " + DomainString + " - " + e.getMessage() + " key: " +e.getMessageKey() + " cause: " + e.getCause());
        } catch (InvalidArgumentException e) {
            logger.error("Invalid Argument for Domain search: " + DomainString + " - " + e.getMessage() + " key: " +e.getMessageKey() + " cause: " + e.getCause());
        } catch (CommandException e) {
            logger.error("An exception was thrown by the RSA command: " + e.getMessage() + " key: " +e.getMessageKey() + " cause: " + e.getCause());
        }
        RealmDTO[] realms = searchRealmCmd.getRealms();
        if (realms.length == 0) {
            // ERROR: TODO: throw new Exception("ERROR: Could not find realm SystemDomain");
            domain = null;
            idSource = null;
            logger.error("Unable to find any RSA Security Realm");
            throw new IllegalArgumentException ("Failed to find the requested RSA Security Domain: " + DomainString + " - with filter: " + searchRealmCmd.getFilter().toString());

        } else {
            domain = realms[0].getTopLevelSecurityDomain();
            idSource = realms[0].getIdentitySources()[0];
            logger.info("Found RSA SecurityDomain: " + domain.getName());
            logger.info("Found RSA ID Source: " + idSource.getName());
        }
    }
    
    /**
     * Gets the RSA Session object from the connection.
     * 
     * @return a RSA Session object.
     */
    public ClientSession getRSASession () {
        return this.RSAsession;
    }
    /**
     * Gets the RSA ID Source for the current connection
     * 
     * @return An Identity Source object
     */
    public IdentitySourceDTO getIdSource () {
        return this.idSource;
    }
    /**
     * Gets the RSA Security Domain for the current connection
     * @return an instance of a Domain
     */
    public SecurityDomainDTO getDomain () {
        return this.domain;
    }
    
    /**
     * Release internal resources.
     */
    public void dispose() {
        try {
            this.RSAsession.logout();
        } catch (CommandException e) {
            logger.error("Failed to Logout of the RSA server. Error: " + e.getMessage() + " key: " +e.getMessageKey() + " cause: " + e.getCause());
        }
    }
    
    /**
     * If internal connection is not usable, throw IllegalStateException.
     */
    public void test() {
        // establish a connected session with given credentials
        Connection conn = ConnectionFactory.getConnection("CommandAPIConnection" );   // "CommandAPIConnection"  // createConfigProperties()
        try {
            this.RSAsession =
                conn.connect(this.configuration.getUserMgrPrincipal(),
                             RSAAuthenticationManager8Utils.getPlainPassword(this.configuration.getUserMgrPwd()));
        } catch (CommandException e) {
            logger.error("Failed to connect to the RSA server. Error: " + e.getMessage() + " key: " +
                         e.getMessageKey() + " cause: " + e.getCause());
            throw new IllegalStateException(e); //org.identityconnectors.framework.common.exceptions.ConnectionFailedException(e);
        }
    }
    
    /**
     * Private Methods
     */
    
    /**
     * Creates a RSA connection configuration Properties file using the properties manually set in the Connector \
     * Configuration object. It also encrypts all the configuration properties.
     */
    private void createConfigPropertiesFile () {
        Properties prop = new Properties();

        try {
            //set the encrypted properties value
            prop.setProperty("java.naming.factory.initial", RSAAuthenticationManager8Utils.encrypt(this.configuration.getInitialNamingFactory()));
            prop.setProperty("java.naming.provider.url", RSAAuthenticationManager8Utils.encrypt(this.configuration.getNamingProviderUrl()));
            prop.setProperty("com.rsa.cmdclient.user", RSAAuthenticationManager8Utils.encrypt(this.configuration.getCmdclientUser()));
            prop.setProperty("com.rsa.cmdclient.user.password",
                             RSAAuthenticationManager8Utils.encrypt(RSAAuthenticationManager8Utils.getPlainPassword(this.configuration.getCmdClientUserPwd())));
            prop.setProperty("com.rsa.ssl.client.id.store.password",
                             RSAAuthenticationManager8Utils.encrypt(RSAAuthenticationManager8Utils.getPlainPassword(this.configuration.getRsaSslClientIdStorePwd())));
            prop.setProperty("com.rsa.ssl.client.id.key.password",
                             RSAAuthenticationManager8Utils.encrypt(RSAAuthenticationManager8Utils.getPlainPassword(this.configuration.getRsaSslClientIdKeyPwd())));
            prop.setProperty("ims.ssl.client.provider.url", RSAAuthenticationManager8Utils.encrypt(this.configuration.getImsSslClientProviderUrl()));
            prop.setProperty("ims.ssl.client.identity.keystore.filename", RSAAuthenticationManager8Utils.encrypt(this.configuration.getImsSslClientIdentityKeystoreFilename()));
            prop.setProperty("ims.ssl.client.identity.key.alias", RSAAuthenticationManager8Utils.encrypt(this.configuration.getImsSslClientIdentityKeyAlias()));
            prop.setProperty("ims.ssl.client.root.ca.alias", RSAAuthenticationManager8Utils.encrypt(this.configuration.getImsSslClientRootCaAlias()));
            //prop.setProperty("ims.soap.client.provider.url", RSAAuthenticationManager8Utils.encrypt(this.configuration.getImsSoapClientProviderUrl()));
            prop.setProperty("ims.httpinvoker.client.provider.url", RSAAuthenticationManager8Utils.encrypt(this.configuration.getImsHttpinvokerClientProviderUrl()));

            //save properties to project root folder
            prop.store(new FileOutputStream(RSAAuthenticationManager8Configuration.CONFIG_PROPERTIES_FILE), "Auto-generated properties file storing the RSA Connectivity params.");
            logger.info("Succesfully created the RSA connectivity config properties file!");

        } catch (IOException ex) {
            logger.error("Unable to create configuration properties file: " + RSAAuthenticationManager8Configuration.CONFIG_PROPERTIES_FILE);
        } catch (GeneralSecurityException ex) {
            logger.error("An exception occured while encrypting the configuration properties.");
            throw new RuntimeException ("An Encryption exception occured.", ex);
        }
    }

    /**
     * Creates a Properties object that contains the RSA connection configuration.
     * This method DOES NOT encrypt the properties as the resulting Properties object
     * is not expected to be persisted but used right away as a memory construct.
     * 
     * @return a Properties object.
     */
    private Properties createConfigProperties() {
        Properties prop = new Properties();

        //set the properties value
        prop.setProperty("java.naming.factory.initial", this.configuration.getInitialNamingFactory());
        prop.setProperty("java.naming.provider.url", this.configuration.getNamingProviderUrl());
        prop.setProperty("com.rsa.cmdclient.user", this.configuration.getCmdclientUser());
        prop.setProperty("com.rsa.cmdclient.user.password",
                RSAAuthenticationManager8Utils.getPlainPassword(this.configuration.getCmdClientUserPwd()));
        prop.setProperty("com.rsa.ssl.client.id.store.password",
                RSAAuthenticationManager8Utils.getPlainPassword(this.configuration.getRsaSslClientIdStorePwd()));
        prop.setProperty("com.rsa.ssl.client.id.key.password",
                RSAAuthenticationManager8Utils.getPlainPassword(this.configuration.getRsaSslClientIdKeyPwd()));
        prop.setProperty("ims.ssl.client.provider.url", this.configuration.getImsSslClientProviderUrl());
        prop.setProperty("ims.ssl.client.identity.keystore.filename", this.configuration.getImsSslClientIdentityKeystoreFilename());
        prop.setProperty("ims.ssl.client.identity.key.alias", this.configuration.getImsSslClientIdentityKeyAlias());
        prop.setProperty("ims.ssl.client.root.ca.alias", this.configuration.getImsSslClientRootCaAlias());
        //prop.setProperty("ims.soap.client.provider.url", this.configuration.getImsSoapClientProviderUrl());
        //prop.setProperty("ims.httpinvoker.client.provider.url", this.configuration.getImsHttpinvokerClientProviderUrl());


        logger.info("Succesfully created the RSA connectivity config properties.");

        return prop;
    }

}
