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
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class SolarisConfiguration extends AbstractConfiguration {

    // ALL OF CONFIGURATION COMES FROM SVIDResourceAdapter
    
    public static final long DEFAULT_MUTEX_ACQUIRE_TIMEOUT = 60;
    /*
     * RA_HOST
     */
    private String hostNameOrIpAddr;
    /*
     * RA_PORT
     * 
     * <b>Port</b><br>Enter the port number used to communicate with the
     * resource. This value may depend on the value of Connection Type. The
     * standard value for telnet is 23; the standard value for SSH is 22.
     */
    private Integer port = 23;
    
    /* 
     * all this info is from resource adapter's prototype xml
     * 
     * Supported objectclasses: GROUP, ACCOUNT
     */
    /**
     * basic configuration properties for SSH connection {@link SolarisConnection}
     */
    /*
     * "RA_LOGIN_USER"
     * 
     * <b>Login User</b><br>Enter the name of a user account that has permission
     * to connect to the resource remotely. If this account does not have
     * permission to manage this resource (create user accounts), then you must
     * enter account information for an account that does in the Root User
     * field.<br><br>When connecting to this resource, the initial login is done
     * as the Login User. If a Root User is specified, the su command is used to
     * log in as the Root User before managing the resource.
     */
    //TODO
    
    /*
     * <b>Login Password</b><br>Enter a password for the login account.
     * "LOGIN_PASSWORD
     */
    //TODO
    
    /*
     * RA_LOGIN_SHELL_PROMPT
     * 
     * <b>Login Shell Prompt</b><br>Enter the full shell prompt for the login
     * account. This is used by the adapter to determine when to send a command
     * and when to stop capturing output.
     */
    //TODO loginShellPrompt
    
    /*
     * <b>Root User</b><br>Leave this field blank if the user account entered in
     * the Login User field has permission to manage this resource (create user
     * accounts). Otherwise, enter the name of the user account that does have
     * permission to manage this resource. This is often the root
     * account.<br><br>When connecting to this resource, the initial login is
     * done as the Login User. If a Root User is specified, the su command is
     * used to log in as the Root User before managing the resource.
     */
    //TODO ROOT_USER
    private String userName;
    
    /*
     * <b>credentials</b><br>Enter a password for the Root User account. Leave
     * blank if the Root User field is blank.
     */
    //TODO ROOT_PASSWORD
    private GuardedString password;
    
    /*
     * RA_ROOT_SHELL_PROMPT
     * 
     * <b>Root Shell Prompt</b><br>Enter the full shell prompt for the root
     * account. This is used by the adapter to determine when to send a command
     * and when to stop capturing output. Leave blank if the Root User field is
     * blank.
     */
    private String rootShellPrompt;
    
    /*
     * RA_SUDO_AUTH
     * 
     * <b>Sudo Authorization</b><br>Indicate whether the admin commands are to
     * authorize the user through the sudo utility. Enter a value of TRUE to use
     * sudo or FALSE for standard authorization.
     */
    private boolean sudoAuth = false;

    /*
     * RA_CONN_TYPE
     * 
     * <b>Connection Type</b><br>Specify the script connection
     * protocol type. The default protocol is Telnet. Supported protocols are
     * Telnet, SSH, and SSHPubKey.
     */
    private String connectionType = ConnectionType.TELNET.toString();
    //private ConnectorMessages _connectorMessages;
    
    /*
     * RA_PRIVATE_KEY
     * 
     * <b>Private Key</b><br>Specify the private key for SSH connection. A
     * private key is required for key/pair based authentication. If a private
     * key is specified, you must enter SSHPubKey as the value for the
     * <b>Connection Type</b> field. Do not use this field if you implement
     * password-based authentication.
     */
    //TODO PRIVATE_KEY
    
    /*
     * RA_CONN_MAX
     * 
     * <b>Maximum Connections</b><br>Specify the maximum number of concurrent
     * connections to the resource. The default value is 10.
     * 
     * IMHO @deprecated
     */
    // CONN_MAX = 10;
    
    /*
     * RA_CONN_TIMEOUT
     * 
     *      * IMHO @deprecated
     * 
     * <b>Connection Idle Timeout</b><br>Specify the number of seconds an
     * established connection is to be idle before being released by the
     * connection pool. The default value is 900 seconds.
     * 
     * see description in RAMessages.properties. The property name is derived
     * from the SVIDResourceAdapter#svidPrototypeXml
     * 
     * For example: </ResourceAttribute>\n" +
     * "    <ResourceAttribute name='"+RA_CONN_TIMEOUT
     * +"' displayName='"+RAMessages
     * .RESATTR_CONN_TIMEOUT+"' type='string' multi='false'\n" +
     * "      description='" + RAMessages.RESATTR_HELP_73 + "'"
     * 
     * RA_CONN_TIMEOUT -> message in RAMessages.properties, at key:
     * RESATTR_HELP_73
     */
    // DEFAULT = 900
    
    /*
     * RA_BLOCK_FETCH_TIMEOUT
     * 
     * <b>Block Fetch Timeout</b><br>Specify the number of seconds a block fetch
     * operation is to execute before timing out. The default value is 600
     * seconds.
     * 
     * IMHO @deprecated
     */
    // default = 600
    /*
     * RA_MUTEX_ACQUIRE_TIMEOUT
     * Mutex Acquire Timeout
     */
    private long mutexAcquireTimeout = DEFAULT_MUTEX_ACQUIRE_TIMEOUT;
    

    
    /*
     * RA_HOME_BASE_DIR
     * 
     * <b>Home Base Directory</b><br>Indicate the home directory base to be used
     * when creating user home directories. The accountID will be appended to
     * this value to form the user home directory.
     */
    private String homeBaseDir;
    
    /*
     * RA_KEY_PASSPHRASE
     * 
     * <b>Passphrase</b><br>Specify the passphrase, used during key generation.
     */
    //TODO KEY_PASSPHRASE // ??????????????????

    /*
     * RA_DEFAULT_PRIMARY_GROUP
     * 
     * <b>Default Primary Group</b><br>Default group a new user will be placed
     * in.
     */
    private String defaultPrimaryGroup;
    
    /*
     * RA_LOGIN_SHELL
     * 
     * <b>Login Shell</b><br>Default shell a new user will be given.
     */
    private String loginShell;

    /*
     * RA_SKEL_DIR
     * 
     * <b>Skeleton Directory</b><br>Specify a directory to use to copy default
     * files to the user''s home directory. Typically this is /etc/skel. This
     * directory must already exist.
     */
    private String skelDir;

    /*
     * RA_MAKE_DIR
     * 
     * <b>Make Directory</b><br>Indicate whether the user''s home directory
     * should be created. Enter a value of TRUE to create the user''s home
     * directory or FALSE to do nothing. <br>The user''s home directory will be
     * created in /home unless set by using a <b>dir</b> attribute setting in
     * the schema map to set the home directory path.
     */
    private Boolean makeDir;// (if to make home directory)
    
    /*
     * RA_DEL_HOME_DIR
     * 
     * RESATTR_HELP_1=<b>Delete Home Directory</b><br>Specifies whether an
     * accounts home directory should be deleted when the account is deleted.
     */
    private boolean delHomeDir = false;
    
    /*
     * RA_SYS_DB_TYPE
     * 
     * <b>System Database Type</b><br>Specify the system database type in use.
     * The default type is FILES. Supported types are NIS.
     */
    private String sysDbType = "FILES";
    
    /*
     * RA_NISDIR
     * 
     * <b>NIS Build Directory</b><br>Enter the directory name where the NIS build files are located.
     */
    private String nisDir = "/var/yp";

    /*
     * RA_NISPWDDIR
     * 
     * <b>NIS Password Source Directory</b><br>Enter the directory name where
     * the NIS password source files are located.
     */
    private String nisPwdDir;
    
    /*
     * RA_NISSHADOW
     * 
     * <b>NIS Shadow Password Support</b><br>Specify TRUE if the NIS database
     * used shadow passwords, FALSE otherwise.
     */
    private boolean nisShadow = false;
    
    /*
     * RA_NISSRCDIR
     * 
     * <b>NIS Source Directory</b><br>Enter the directory name where the NIS source files are located.
     */
    //TODO NISSRCDIR
    
    
    //===================================================================================
    //===================================================================================
    //===================================================================================
    



    /* ********** CONSTRUCTOR ************ */
    public SolarisConfiguration() {
        // default constructor
    }
    /** 
     * cloning constructor, deep copy 
     */
    public SolarisConfiguration(Configuration config) {
        if (config == null) {
            throw new AssertionError("Configuration cannot be null");
        }

        if (config instanceof SolarisConfiguration) {
            final SolarisConfiguration cfg = (SolarisConfiguration) config;
            this.userName = cfg.getUserName();
            this.password = cfg.getPassword();
            this.hostNameOrIpAddr = cfg.getHostNameOrIpAddr();
            this.port = cfg.getPort();
            this.connectionType = cfg.getConnectionType();
        } else {
            throw new AssertionError("cannot clone other types than SolarisConfiguration");
        }
    }
    
    /* ********** GET / SET ************ */
    @ConfigurationProperty(required=true)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        userName = name;
    }

    @ConfigurationProperty(required=true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString _password) {
        this.password = _password;
    }

    @ConfigurationProperty(required=true)
    public String getHostNameOrIpAddr() {
        return hostNameOrIpAddr;
    }

    public void setHostNameOrIpAddr(String nameOrIpAddr) {
        hostNameOrIpAddr = nameOrIpAddr;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @ConfigurationProperty(required=true)
    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = ConnectionType.toConnectionType(connectionType).toString();
    }
    
    @ConfigurationProperty(required=true)
    public String getRootShellPrompt() {
        return rootShellPrompt;
    }
    public void setRootShellPrompt(String rootShellPrompt) {
        this.rootShellPrompt = rootShellPrompt;
    }
    
    public boolean isSudoAuth() {
        return sudoAuth;
    }
    
    public void setSudoAuth(boolean sudoAuth) {
        this.sudoAuth = sudoAuth;
    }
    
    public long getMutexAcquireTimeout() {
        return mutexAcquireTimeout;
    }
    public void setMutexAcquireTimeout(long mutexAcquireTimeout) {
        this.mutexAcquireTimeout = mutexAcquireTimeout;
    }
    
    public String getSkelDir() {
        return skelDir;
    }
    public void setSkelDir(String skelDir) {
        this.skelDir = skelDir;
    }
    
    public Boolean getMakeDir() {
        return makeDir;
    }
    public void setMakeDir(Boolean makeDir) {
        this.makeDir = makeDir;
    }
    
    public String getHomeBaseDir() {
        return homeBaseDir;
    }
    public void setHomeBaseDir(String homeBaseDir) {
        this.homeBaseDir = homeBaseDir;
    }
    
    public String getDefaultPrimaryGroup() {
        return defaultPrimaryGroup;
    }
    public void setDefaultPrimaryGroup(String defaultPrimaryGroup) {
        this.defaultPrimaryGroup = defaultPrimaryGroup;
    }
    
    public String getLoginShell() {
        return loginShell;
    }
    public void setLoginShell(String loginShell) {
        this.loginShell = loginShell;
    }
    
    public boolean isDelHomeDir() {
        return delHomeDir;
    }
    public void setDelHomeDir(boolean delHomeDir) {
        this.delHomeDir = delHomeDir;
    }
    
    public String getSysDbType() {
        return sysDbType;
    }
    public void setSysDbType(String sysDbType) {
        this.sysDbType = sysDbType;
    }
    
    public String getNisPwdDir() {
        return nisPwdDir;
    }
    public void setNisPwdDir(String nisPwdDir) {
        this.nisPwdDir = nisPwdDir;
    }
    
    public String getNisDir() {
        return nisDir;
    }
    public void setNisDir(String nisDir) {
        this.nisDir = nisDir;
    }
    
    public boolean isNisShadow() {
        return nisShadow;
    }
    public void setNisShadow(boolean nisShadow) {
        this.nisShadow = nisShadow;
    }

    
    /* *********** AUXILIARY METHODS ***************** */
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
        if (StringUtil.isBlank(getUserName())) {
            throw new ConfigurationException(String.format(msg, "UserName"));
        }
        
        if (getPassword() == null) {
            throw new ConfigurationException(String.format(msg, "Password"));
        }
        
        if (StringUtil.isBlank(getHostNameOrIpAddr())) {
            throw new ConfigurationException(String.format(msg, "Hostname/IP address"));
        }
        
        if (port == null || port < 0) {
            throw new ConfigurationException(String.format(msg, "Port"));
        }
        
        if (StringUtil.isBlank(connectionType)) {
            throw new ConfigurationException(String.format(msg, "Connection type"));
        }
        
        if (StringUtil.isBlank(rootShellPrompt)) {
            throw new ConfigurationException(String.format(msg, "Root shell prompt"));
        }

    }
}
