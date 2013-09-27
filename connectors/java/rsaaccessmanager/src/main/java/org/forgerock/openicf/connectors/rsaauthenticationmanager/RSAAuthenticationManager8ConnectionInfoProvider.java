
package org.forgerock.openicf.connectors.rsaauthenticationmanager;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.identityconnectors.common.logging.Log;

import org.identityconnectors.common.security.GuardedString;

/**
 * The connector's own implementation of RSA's ConnectInfoProvider, implemented
 * in order to decrypt the properties of the config.properties file.
 * 
 * @author Alex Babeanu (ababeanu@nulli.com)
 * www.nulli.com - Identity Solution Architects
 * 
 * @version 1.1
 * @since 1.1
 */
public class RSAAuthenticationManager8ConnectionInfoProvider implements com.rsa.command.ConnectionInfoProvider {

    /**
     * The file holding the connection properties.
     * Not really used in this implementation but expected by RSA SDK framework.
     */
    private String propertiesFile = null;   //using a file to store the properties
    /**
     * The configuration properties
     */
    private Properties properties = new Properties();
    /**
     * The logger.
     */
    private static final Log logger = Log.getLog(RSAAuthenticationManager8Connector.class);

    /**
     * Setter for the Properties File property, also reads the 
     * config.properties file and sets the properties property. This is to only
     * read the file once instead of once per property in order to limit I/O.
     * This is invoked by the the Spring fwk if specified in the Bean definition.
     * 
     * @param propertiesFile a String representing the path/filename of the 
     * configuration properties.
     */
    public void setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
        
        // Load properties file once at the time its location is set.
        try {
            //load a properties file
            properties.load(new FileInputStream(propertiesFile));
        } catch (IOException ex) {
            logger.error("Unable to read Properties File: " + propertiesFile + " - " + ex.getMessage() + " - " + ex.getCause());
            throw new RuntimeException("Unable to find or read the config.properties file...", ex);
        }
        
    }

    /**
     * {@inheritDoc}
     */
    public String getStringValue(String key) {

        //retrieve (and decrypt) the specified property from propertiesFile
        String prop = properties.getProperty(key);
        if (prop == null)
            prop = "";
        try {
            return RSAAuthenticationManager8Utils.decrypt(prop);
        } catch (GeneralSecurityException ex) {
            logger.error("Unable to decrypt the configuration properties.");
            throw new RuntimeException ("An error occured while decrypting the config.properties file.", ex);
        } catch (IOException ex) {
            logger.error("An I/O exception occured while trying to decrypt the configuration properties.");
            throw new RuntimeException ("An I/O exception occured.", ex);
        }
    }
}

