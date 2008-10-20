/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
using System;
using System.Reflection;
using System.Globalization;
using System.Collections.Generic;
using System.Net.Security;
using System.Security;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Common.Pooling;
using Org.IdentityConnectors.Common.Security;
using Org.IdentityConnectors.Framework.Api.Operations;
using Org.IdentityConnectors.Framework.Common;
using Org.IdentityConnectors.Framework.Common.Objects;

namespace Org.IdentityConnectors.Framework.Api
{
    public static class APIConstants {
        public const int NO_TIMEOUT = -1;
    }
    
    public interface APIConfiguration {
        ConfigurationProperties ConfigurationProperties { get; }
        bool IsConnectorPoolingSupported { get; }
        ObjectPoolConfiguration ConnectorPoolConfiguration { get; }
        ICollection<SafeType<APIOperation>> SupportedOperations { get; }
        
        int GetTimeout(SafeType<APIOperation> operation);
        void SetTimeout(SafeType<APIOperation> operation, int timeout);
        
        int ProducerBufferSize { get; set; }
    }
    
    /**
     * Configuration properties encapsulates the {@link Configuration} and uses
     * {@link Reflection} to determine the properties available for manipulation.
     */
    public interface ConfigurationProperties {
    
        /**
         * Get the list of properties names for this {@link Configuration}.
         * 
         * @return get the list of properties names.
         */
        IList<string> PropertyNames { get; }
    
        /**
         * Get a particular {@link ConfigurationProperty} by name.
         * 
         * @param name
         *            the unique name of the property.
         * @return a {@link ConfigurationProperty} if it exists otherwise null.
         */
        ConfigurationProperty GetProperty(string name);
    
        /**
         * Set the value of the {@link Configuration} property by name.
         * 
         * @param name
         *            Name of the property to set the value against.
         * @param value
         *            Value to set on the configuration property.
         * @throws IllegalArgumentException
         *             iff the property name does not exist.
         */
        void SetPropertyValue(string name, Object value);
    
    }

    /**
     * Translation from {@link Configuration} at the SPI layer to the API.
     */
    public interface ConfigurationProperty {
    
        int Order { get; }
        
        /**
         * Get the unique name of the configuration property.
         */
        string Name { get; }
    
        /**
         * Get the help message from the message catalog.
         */
        string GetHelpMessage(string def);
    
        /**
         * Get the display name for the is configuration
         */
        string GetDisplayName(string def);
    
        /**
         * Get the value from the property. This should be the default value.
         */
        object Value { get; set; }
    
        /**
         * Get the type of the property.
         */
        Type ValueType { get; }
        
        /**
         * Is this a confidential property whose value should be encrypted by
         * the application when persisted?
         */
        bool IsConfidential { get; }
        
        /**
         * Set of operations for which this property must be specified.
         * This is used for the case where a connector may or may not
         * implement certain operations depending in the configuration.
         * The default value of "empty array" is special in that
         * it means that this property is applicable to all operations.
         */        
        ICollection<SafeType<APIOperation>> Operations { get; }
    }
    
    /**
     * Main interface for which consumers call the Connector API logic.
     */
    public interface ConnectorFacade : CreateApiOp, DeleteApiOp,
            SearchApiOp, UpdateApiOp, SchemaApiOp, AuthenticationApiOp, GetApiOp,
            ValidateApiOp, TestApiOp, ScriptOnConnectorApiOp, ScriptOnResourceApiOp,
            SyncApiOp {

        /**
         * Get the set of operations that this {@link ConnectorFacade} will support.
         */
        ICollection<SafeType<APIOperation>> SupportedOperations { get; }

        /**
         * Get an instance of an operation that this facade supports.
         */
        APIOperation GetOperation(SafeType<APIOperation> type);
 
    }
    
    /**
     * Manages a pool of connectors for use by a provisioner.
     * 
     */
    public abstract class ConnectorFacadeFactory {
    
        // At some point we might make this pluggable, but for now, hard-code
        private const string IMPL_NAME = 
            "Org.IdentityConnectors.Framework.Impl.Api.ConnectorFacadeFactoryImpl";
        private static ConnectorFacadeFactory _instance;
        private static object LOCK = new Object();
    
        /**
         * Get the singleton instance of the {@link ConnectorFacadeFactory}.
         */
        public static ConnectorFacadeFactory GetInstance() {
            lock(LOCK) {
                if (_instance == null) {
                    SafeType<ConnectorFacadeFactory> t = FrameworkInternalBridge.LoadType<ConnectorFacadeFactory>(IMPL_NAME);
                    _instance = t.CreateInstance();
                }
            }
            return _instance;
        }
    
        /**
         * Get a new instance of {@link ConnectorFacade}.
         * 
         * @param config
         *            all the configuration that the framework, connector, and
         *            pooling needs.
         * @return {@link ConnectorFacade} to call API operations against.
         * @throws ClassNotFoundException
         */
        public abstract ConnectorFacade NewInstance(APIConfiguration config);
        
        
        /**
         * Dispose of all connection pools, resources, etc.
         */
        public abstract void Dispose();
    }

    /**
     * The connector meta-data for a given connector.
     */
    public interface ConnectorInfo {   
        /**
         * Returns a friendly name suitable for display in the UI.
         * 
         * @return The friendly name
         */
        string GetConnectorDisplayName(); 
        
        ConnectorKey ConnectorKey { get; }

        /**
         * Loads the {@link Connector} and {@link Configuration} class in order to
         * determine the proper default configuration parameters.
         */
        APIConfiguration CreateDefaultAPIConfiguration();
    }
    /**
     * Class responsible for maintaing a list of <code>ConnectorInfo</code>
     * associated with a set of connector bundles.
     */
    public interface ConnectorInfoManager {
        /**
         * Returns the list of <code>ConnectorInfo</code>
         * @return the list of <code>ConnectorInfo</code>
         */
        IList<ConnectorInfo> ConnectorInfos { get; }
    
        /**
         * Given a connectorName and connectorVersion, returns the
         * associated <code>ConnectorInfo</code>.
         * @param key The connector key.
         * @return The <code>ConnectorInfo</code> or null if it couldn't
         *         be found.
         */
        ConnectorInfo FindConnectorInfo(ConnectorKey key);
    }
    /**
     * The main entry point into connectors. This allows you
     * to load the connector classes from a set of bundles.
     */
    public abstract class ConnectorInfoManagerFactory {
        //At some point we might make this pluggable, but for now, hard-code
        private const string IMPL_NAME =  
            "Org.IdentityConnectors.Framework.Impl.Api.ConnectorInfoManagerFactoryImpl";
        private static ConnectorInfoManagerFactory _instance;
        private static object LOCK = new Object();
        /// <summary>
        /// Singleton pattern for getting an instance of the 
        /// ConnectorInfoManagerFactory.
        /// </summary>
        /// <returns></returns>
        public static ConnectorInfoManagerFactory GetInstance() {
            lock(LOCK) {
                if (_instance == null) {
                    SafeType<ConnectorInfoManagerFactory> t = 
                        FrameworkInternalBridge.LoadType<ConnectorInfoManagerFactory>(IMPL_NAME);
                    _instance = t.CreateInstance();
                }
            }
            return _instance;
        }
        public abstract ConnectorInfoManager GetLocalManager();
        public abstract ConnectorInfoManager GetRemoteManager(RemoteFrameworkConnectionInfo info);
        
        /**
         * Clears the bundle manager cache. Generally intended for unit testing
         */
        public abstract void ClearRemoteCache();
    }
    
    /**
     * Uniquely identifies a connector within an installation.
     * Consists of the triple (bundleName, bundleVersion, connectorName)
     */
    public sealed class ConnectorKey {
        private readonly string _bundleName;
        private readonly string _bundleVersion;
        private readonly string _connectorName;
        
        public ConnectorKey(String bundleName,
                String bundleVersion,
                String connectorName) {
            if (bundleName == null) {
                throw new ArgumentException("bundleName may not be null");
            }
            if (bundleVersion == null) {
                throw new ArgumentException("bundleVersion may not be null");            
            }
            if (connectorName == null) {
                throw new ArgumentException("connectorName may not be null");            
            }
            _bundleName    = bundleName;
            _bundleVersion = bundleVersion;
            _connectorName = connectorName;
        }
        
        public string BundleName {
            get {
                return _bundleName;
            }
        }
        
        public string BundleVersion {
            get {
                return _bundleVersion;
            }
        }
        
        public string ConnectorName {
            get {
                return _connectorName;
            }
        }
        
        public override bool Equals(object o) {
            if ( o is ConnectorKey ) {
                ConnectorKey other = (ConnectorKey)o;
                if (!_bundleName.Equals(other._bundleName)) {
                    return false;
                }
                if (!_bundleVersion.Equals(other._bundleVersion)) {
                    return false;
                }
                if (!_connectorName.Equals(other._connectorName)) {
                    return false;
                }
                return true;
            }
            return false;
        }
        
        public override int GetHashCode() {
            int rv = 0;
            rv ^= _connectorName.GetHashCode();
            return rv;
        }
        
        public override string ToString() {
            StringBuilder builder = new StringBuilder();
            builder.Append("ConnectorKey(");
            builder.Append(" bundleName=").Append(_bundleName);
            builder.Append(" bundleVersion=").Append(_bundleVersion);
            builder.Append(" connectorName=").Append(_connectorName);
            builder.Append(" )");
            return builder.ToString();
        }
    }


    
    public sealed class RemoteFrameworkConnectionInfo {
        private readonly String _host;
        private readonly int _port;
        private readonly GuardedString _key;
        private readonly bool _useSSL;
        private readonly RemoteCertificateValidationCallback _certificateValidationCallback;
        private readonly int _timeout;
    
        /**
         * Creates a new instance of RemoteFrameworkConnectionInfo, using
         * a clear (non-ssl) connection and a 60-second timeout.
         * @param host The host to connect to
         * @param port The port to connect to
         */
        public RemoteFrameworkConnectionInfo(String host,
                int port,
                GuardedString key)
            :this(host,port,key,false,null,60*1000) {
        }
        
        /**
         * Creates a new instance of RemoteFrameworkConnectionInfo.
         * @param host The host to connect to
         * @param port The port to connect to
         * @param useSSL Set to true if we are to connect via SSL.
         * @param certificateValidationCallback to use
         * for establising the SSL connection. May be null or empty,
         * in which case the default installed providers for the JVM will
         * be used. Ignored if 'useSSL' is false. 
         * @param timeout The timeout to use (in milliseconds). A value of 0
         * means infinite timeout;
         */
        public RemoteFrameworkConnectionInfo(String host,
                int port,
                GuardedString key,
                bool useSSL,
                RemoteCertificateValidationCallback certificateValidationCallback,
                int timeout) {
            
            if ( host == null ) {
                throw new ArgumentException("Parameter 'host' is null.");
            }
            if ( key == null ) {
                throw new ArgumentException("Parameter 'key' is null.");
            }
                    
            _host = host;
            _port = port;
            _key  = key;
            _useSSL = useSSL;
            _certificateValidationCallback = certificateValidationCallback;
            _timeout = timeout;
        }
        
        /**
         * Returns the host to connect to.
         * @return The host to connect to.
         */
        public String Host {
            get {
                return _host;
            }
        }
        
        /**
         * Returns the port to connect to
         * @return The port to connect to
         */
        public int Port {
            get {
                return _port;
            }
        }
        
        public GuardedString Key {
            get {
                return _key;
            }
        }
        
        /**
         * Returns true iff we are to use SSL to connect.
         * @return true iff we are to use SSL to connect.
         */
        public bool UseSSL {
            get {
                return _useSSL;
            }
        }
            
        /**
         * Returns the list of {@link TrustManager}'s. to use when establishing
         * the connection.
         * @return The list of {@link TrustManager}'s.
         */
        public RemoteCertificateValidationCallback CertificateValidationCallback {
            get {
                return _certificateValidationCallback;
            }
        }
        
        /**
         * Returns the timeout (in milliseconds) to use for the connection.
         * A value of zero means infinite timeout.
         * @return the timeout (in milliseconds) to use for the connection.
         */
        public int Timeout {
            get {
                return _timeout;
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public override bool Equals(Object o) {
            if ( o is RemoteFrameworkConnectionInfo ) {
                RemoteFrameworkConnectionInfo other = 
                    (RemoteFrameworkConnectionInfo)o;
                if (!Object.Equals(Host,other.Host)) {
                    return false;
                }
                if (Port != other.Port) {
                    return false;
                }
                if (UseSSL != other.UseSSL) {
                    return false;
                }
                if (CertificateValidationCallback == null ||
                    other.CertificateValidationCallback == null) {
                    if (CertificateValidationCallback != null ||
                        other.CertificateValidationCallback != null) {
                        return false;
                    }
                }
                else {
                    if (!CertificateValidationCallback.Equals
                        (other.CertificateValidationCallback)) {
                        return false;
                    }
                }
                
                if (!Key.Equals(other.Key)) {
                    return false;
                }
                
                if (Timeout != other.Timeout) {
                    return false;
                }
                
                return true;
            }
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        public override int GetHashCode() {
            return _host.GetHashCode() ^ _port;
        }
        
        /**
         * {@inheritDoc}
         */
        public override String ToString() {
            return "{host="+_host+", port="+_port+"}";
        }
        

    }
}
