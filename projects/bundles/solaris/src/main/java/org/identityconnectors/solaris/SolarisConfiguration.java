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
 */
package org.identityconnectors.solaris;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.solaris.operation.search.SolarisEntries;

public final class SolarisConfiguration extends AbstractConfiguration {

    public static final int DEFAULT_MUTEX_ACQUIRE_TIMEOUT = 60;

    private String host;

    /**
     * Port number used to communicate with the resource. This value may depend
     * on the value of Connection Type. The standard value for telnet is 23; the
     * standard value for SSH is 22.
     */
    private int port = 23;

    /**
     * <b>Login User</b> <br>
     * Enter the name of a user account that has permission to connect to the
     * resource remotely. If this account does not have permission to manage
     * this resource (create user accounts), then you must enter account
     * information for an account that does in the Root User field.<br>
     * <br>
     * When connecting to this resource, the initial login is done as the Login
     * User. If a Root User is specified, the su command is used to log in as
     * the Root User before managing the resource.
     * 
     * This property is coupled with {@link SolarisConfiguration#password},
     * {@link SolarisConfiguration#loginShellPrompt}.
     */
    private String loginUser; // NEW

    /**
     * <b>Login Password</b><br>
     * Enter a password for the login account.
     * 
     * This property is coupled with {@link SolarisConfiguration#loginUser}.
     */
    private GuardedString password;

    /**
     * <b>Login Shell Prompt</b><br>
     * Enter the full shell prompt for the login account. This is used by the
     * adapter to determine when to send a command and when to stop capturing
     * output.
     * 
     * This property is coupled with {@link SolarisConfiguration#loginUser}.
     */
    private String loginShellPrompt; // NEW

    /**
     * <b>Root User</b>
     * 
     * <br>
     * Leave this field blank if the user account entered in the Login User
     * field has permission to manage this resource (create user accounts).
     * Otherwise, enter the name of the user account that does have permission
     * to manage this resource. This is often the root account.<br>
     * 
     * <br>
     * When connecting to this resource, the initial login is done as the Login
     * User. If a Root User is specified, the su command is used to log in as
     * the Root User before managing the resource.
     * 
     * This property is coupled with {@link SolarisConfiguration#credentials},
     * {@link SolarisConfiguration#rootShellPrompt}.
     */
    private String rootUser;

    /**
     * <b>credentials</b>
     * 
     * <br>
     * Password for the Root User account. Leave blank if the Root User field is
     * blank.
     * 
     * This property is coupled with {@link SolarisConfiguration#rootUser}.
     */
    private GuardedString credentials;

    /**
     * <b>Root Shell Prompt</b>
     * 
     * <br>
     * Enter the full shell prompt for the root account. This is used by the
     * adapter to determine when to send a command and when to stop capturing
     * output. Leave blank if the Root User field is blank.
     * 
     * This property is coupled with {@link SolarisConfiguration#rootUser}.
     */
    private String rootShellPrompt;

    /**
     * RA_SUDO_AUTH
     * 
     * <b>Sudo Authorization</b><br>
     * Indicate whether the admin commands are to authorize the user through the
     * sudo utility. Enter a value of TRUE to use sudo or FALSE for standard
     * authorization.
     */
    private boolean sudoAuthorization = false;

    /**
     * <b>Connection Type</b>
     * 
     * <br>
     * Specify the script connection protocol type. The default protocol is
     * Telnet. Supported protocols are Telnet, SSH, and SSHPubKey.
     */
    private String connectionType = ConnectionType.TELNET.toString();

    /**
     * <b>Private Key</b><br>
     * 
     * Specify the private key for SSH connection. A private key is required for
     * key/pair based authentication. If a private key is specified, you must
     * enter SSHPubKey as the value for the <b>Connection Type</b> field. Do not
     * use this field if you implement password-based authentication.
     * 
     * This attribute makes sense for {@link ConnectionType#SSHPUBKEY}. See
     * {@link SolarisConfiguration#passphrase}.
     */
    private GuardedString privateKey;

    /**
     * <b>Key Passphrase</b><br>
     * 
     * Specify the passphrase, used during key generation.
     * 
     * This attribute makes sense for {@link ConnectionType#SSHPUBKEY}. See
     * {@link SolarisConfiguration#privateKey}.
     */
    private GuardedString passphrase;

    /**
     * <b>Block size</b><br>
     * 
     * when performing full scan (
     * {@link SolarisEntries#getAllAccounts(java.util.Set, SolarisConnection)})
     * on all the accounts we can limit the number of accounts fetched at once.
     */
    private int blockSize = 100;

    /**
     * RA_BLOCK_FETCH_TIMEOUT
     * 
     * <b>Block Fetch Timeout</b><br>
     * Specify the number of seconds a block fetch operation is to execute
     * before timing out. The default value is 600 seconds.
     * 
     * Unit: seconds
     */
    private int blockFetchTimeout = 600;

    /*
     * RA_MUTEX_ACQUIRE_TIMEOUT Mutex Acquire Timeout
     * 
     * Unit: seconds
     */
    private int mutexAcquireTimeout = DEFAULT_MUTEX_ACQUIRE_TIMEOUT;

    /**
     * RA_MAKE_DIR
     * 
     * <b>Make Directory</b><br>Indicate whether the user''s home directory
     * should be created. Enter a value of TRUE to create the user''s home
     * directory or FALSE to do nothing.
     */ 
    /*<br>The user''s home directory will be
     * created in /home unless set by using a <b>dir</b> attribute setting in
     * the schema map to set the home directory path.
     */
    private boolean makeDirectory;// (if to make home directory)

    /**
     * <b>Home Base Directory</b>
     * 
     * <br>Indicate the home directory base to be used
     * when creating user home directories. The accountID will be appended to
     * this value to form the user home directory.
     */
    private String homeBaseDirectory;

    /**
     * <b>Default Primary Group</b>
     * 
     * <br>Default group a new user will be placed
     * in.
     */
    private String defaultPrimaryGroup;

    /**
     * <b>Login Shell</b><br>
     * 
     * Default shell a new user will be given.
     */
    private String loginShell;

    /**
     * <b>Skeleton Directory</b><br>
     * 
     * Specify a directory to use to copy default
     * files to the user''s home directory. Typically this is /etc/skel. This
     * directory must already exist.
     */
    private String skeletonDirectory;

    /**
     * <b>Delete Home Directory</b><br>
     * 
     * Specifies whether an
     * accounts home directory should be deleted when the account is deleted.
     */
    private boolean deleteHomeDirectory = false;

    /**
     * <b>System Database Type</b><br>
     * 
     * Specify the system database type in use.
     * The default type is FILES. Supported types are NIS.
     */
    private String systemDatabaseType = "FILES";

    /** 
     * <b>NIS Build Directory</b><br>
     * 
     * Enter the directory name where the NIS
     * build files are located.
     */
    private String nisBuildDirectory = "/var/yp";

    static final String DEFAULT_NISPWDDIR = "/etc";
    
    /**
     * <b>NIS Password Source Directory</b><br>
     * 
     * Enter the directory name where
     * the NIS password source files are located.
     */
    private String nisPwdDir = DEFAULT_NISPWDDIR;

    /** 
     * <b>NIS Shadow Password Support</b><br>
     * 
     * Specify TRUE if the NIS database
     * used shadow passwords, FALSE otherwise.
     */
    private boolean nisShadowPasswordSupport = false;
    
    

    /*            ********** CONSTRUCTOR ************ */
    public SolarisConfiguration() {
        // default constructor
    }

    /*            ********** GET / SET ************ */
    @ConfigurationProperty(order = 1, required = true)
    public String getHost() {
        return host;
    }

    public void setHost(String nameOrIpAddr) {
        host = nameOrIpAddr;
    }

    @ConfigurationProperty(order = 2)
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    @ConfigurationProperty(order = 3, required = true)
    public String getLoginUser() {
        return loginUser;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(order = 4, required = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setLoginShellPrompt(String loginShellPrompt) {
        this.loginShellPrompt = loginShellPrompt;
    }

    @ConfigurationProperty(order = 5, required = true)
    public String getLoginShellPrompt() {
        return loginShellPrompt;
    }

    @ConfigurationProperty(order = 6)
    public String getRootUser() {
        return rootUser;
    }

    public void setRootUser(String name) {
        rootUser = name;
    }

    @ConfigurationProperty(order = 7)
    public GuardedString getCredentials() {
        return credentials;
    }

    public void setCredentials(GuardedString password) {
        credentials = password;
    }

    @ConfigurationProperty(order = 8)
    public String getRootShellPrompt() {
        return rootShellPrompt;
    }

    public void setRootShellPrompt(String rootShellPrompt) {
        this.rootShellPrompt = rootShellPrompt;
    }

    @ConfigurationProperty(order = 9)
    public boolean isSudoAuthorization() {
        return sudoAuthorization;
    }

    public void setSudoAuthorization(boolean sudoAuth) {
        this.sudoAuthorization = sudoAuth;
    }

    @ConfigurationProperty(order = 10)
    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = ConnectionType.toConnectionType(connectionType).toString();
    }
    
    @ConfigurationProperty(order = 11)
    public GuardedString getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(GuardedString privateKey) {
        this.privateKey = privateKey;
    }
    
    @ConfigurationProperty(order = 12)
    public GuardedString getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(GuardedString keyPassphrase) {
        this.passphrase = keyPassphrase;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    @ConfigurationProperty(order = 13)
    public int getBlockSize() {
        return blockSize;
    }
    
    public void setBlockFetchTimeout(int blockFetchTimeout) {
        this.blockFetchTimeout = blockFetchTimeout;
    }

    @ConfigurationProperty(order = 14)
    public int getBlockFetchTimeout() {
        return blockFetchTimeout;
    }

    @ConfigurationProperty(order = 15)
    public int getMutexAcquireTimeout() {
        return mutexAcquireTimeout;
    }

    public void setMutexAcquireTimeout(int mutexAcquireTimeout) {
        this.mutexAcquireTimeout = mutexAcquireTimeout;
    }
    
    @ConfigurationProperty(order = 16)
    public boolean isMakeDirectory() {
        return makeDirectory;
    }

    public void setMakeDirectory(boolean makeDir) {
        makeDirectory = makeDir;
    }
    
    @ConfigurationProperty(order = 17)
    public String getHomeBaseDirectory() {
        return homeBaseDirectory;
    }

    public void setHomeBaseDirectory(String homeBaseDir) {
        homeBaseDirectory = homeBaseDir;
    }
    
    @ConfigurationProperty(order = 18)
    public String getDefaultPrimaryGroup() {
        return defaultPrimaryGroup;
    }

    public void setDefaultPrimaryGroup(String defaultPrimaryGroup) {
        this.defaultPrimaryGroup = defaultPrimaryGroup;
    }
    
    @ConfigurationProperty(order = 19)
    public String getLoginShell() {
        return loginShell;
    }

    public void setLoginShell(String loginShell) {
        this.loginShell = loginShell;
    }

    @ConfigurationProperty(order = 20)
    public String getSkeletonDirectory() {
        return skeletonDirectory;
    }

    public void setSkeletonDirectory(String skelDir) {
        skeletonDirectory = skelDir;
    }

    @ConfigurationProperty(order = 21)
    public boolean isDeleteHomeDirectory() {
        return deleteHomeDirectory;
    }

    public void setDeleteHomeDirectory(boolean delHomeDir) {
        this.deleteHomeDirectory = delHomeDir;
    }

    @ConfigurationProperty(order = 22)
    public String getSystemDatabaseType() {
        return systemDatabaseType;
    }

    public void setSystemDatabaseType(String sysDbType) {
        systemDatabaseType = sysDbType;
    }
    
    @ConfigurationProperty(order = 23)
    public String getNisBuildDirectory() {
        return nisBuildDirectory;
    }

    public void setNisBuildDirectory(String nisDir) {
        this.nisBuildDirectory = nisDir;
    }

    @ConfigurationProperty(order = 24)
    public String getNisPwdDir() {
        return nisPwdDir;
    }

    public void setNisPwdDir(String nisPwdDir) {
        this.nisPwdDir = nisPwdDir;
    }

    @ConfigurationProperty(order = 25)
    public boolean isNisShadowPasswordSupport() {
        return nisShadowPasswordSupport;
    }

    public void setNisShadowPasswordSupport(boolean nisShadow) {
        nisShadowPasswordSupport = nisShadow;
    }

    /*            *********** AUXILIARY METHODS ***************** */
    /**
     * Get the Localization message for the current key
     */
    public String getMessage(String key) {
        return getConnectorMessages().format(key, key);
    }

    /**
     * See {@link SolarisConfiguration#getMessage(String)}.
     */
    public String getMessage(String key, Object... objects) {
        return getConnectorMessages().format(key, key, objects);
    }

    @Override
    public void validate() {
        String msg = "'%s' cannot be null or empty.";
        boolean isLoginUserCredentials = !StringUtil.isBlank(loginUser) && !StringUtil.isBlank(loginShellPrompt) && password != null;
        if (!isLoginUserCredentials) {
            throw new ConfigurationException(String.format(msg, "[loginUser, loginShellPrompt, password]")); 
        } 
        
        if (connectionType.equals(ConnectionType.SSHPUBKEY.toString()) && (passphrase == null || privateKey == null)) {
            throw new ConfigurationException(String.format(msg, "[passphares, privateKey]"));
        }
        
        if (isSudoAuthorization() && getCredentials() == null) {
            throw new ConfigurationException("Root Password missing. In case of sudo authorization for every command you should provide root password too ('credentials' property).");
        }

        if (StringUtil.isBlank(getHost())) {
            throw new ConfigurationException(String.format(msg, "Hostname/IP address"));
        }

        if (port < 0 && port <= 65535) {
            throw new ConfigurationException(String.format(msg, "Port"));
        }

        if (StringUtil.isBlank(connectionType)) {
            throw new ConfigurationException(String.format(msg, "Connection type"));
        }

        

    }
}
