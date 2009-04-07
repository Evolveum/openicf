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
using System;
using System.Globalization;
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Common.Pooling;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Spi.Operations;
namespace Org.IdentityConnectors.Framework.Spi
{
    #region AttributeNormalizer
    /**
     * Interface to be implemented by connectors that need
     * to normalize certain attributes. This might, for
     * example, be used to normalize whitespace within 
     * DN's to ensure consistent filtering whether that
     * filtering is natively on the resource or by the
     * connector framework. For connectors implementing
     * this interface, the method {@link #normalizeAttribute(ObjectClass, Attribute)}
     * will be applied to each of the following:
     * <ol>
     *    <li>The filter passed to {@link SearchOp}.</li>
     *    <li>The results returned from {@link SearchOp}.</li>
     *    <li>The results returned from {@link SyncOp}.</li>
     *    <li>The attributes passed to {@link AdvancedUpdateOp}.</li>
     *    <li>The <code>Uid</code> returned from {@link AdvancedUpdateOp}.</li>
     *    <li>The attributes passed to {@link UpdateOp}.</li>
     *    <li>The <code>Uid</code> returned from {@link UpdateOp}.</li>
     *    <li>The attributes passed to {@link CreateOp}.</li>
     *    <li>The <code>Uid</code> returned from {@link CreateOp}.</li>
     *    <li>The <code>Uid</code> passed to {@link DeleteOp}.</li>
     * </ol>
     */
    public interface AttributeNormalizer 
    {
        ConnectorAttribute NormalizeAttribute(ObjectClass oclass, ConnectorAttribute attribute);
    }
    #endregion
    
    #region Configuration
    ///<summary>
    /// Configuration information for the Connector.
    ///</summary>
    public interface Configuration {

        ConnectorMessages ConnectorMessages{get;set;}
        
        /**
         * Determine if the configuration is valid based on the values set.
         */
        void Validate();
    }
    #endregion
    
    #region AbstractConfiguration
    public abstract class AbstractConfiguration : Configuration {

        public ConnectorMessages ConnectorMessages{get;set;}
        
        public abstract void Validate();
    }
    #endregion

    #region ConnectorClassAttribute
    [AttributeUsage(AttributeTargets.Class,AllowMultiple=false)]
    public class ConnectorClassAttribute : System.Attribute {
        
        private readonly String _connectorDisplayNameKey;
        private readonly SafeType<Configuration> _connectorConfigurationClass;
        
        public ConnectorClassAttribute(String connectorDisplayNameKey,
                                      Type connectorConfigurationClass) {
            _connectorDisplayNameKey = connectorDisplayNameKey;
            _connectorConfigurationClass = SafeType<Configuration>.ForRawType(connectorConfigurationClass);
        }
                
        public string ConnectorDisplayNameKey {
            get {
                return _connectorDisplayNameKey;
            }
        }
                
        public SafeType<Configuration> ConnectorConfigurationType {
            get {
                return _connectorConfigurationClass;
            }
        }
                
        public string [] MessageCatalogPaths {get;set;}
        
    }
    #endregion
    
    #region ConfigurationPropertyAttribute
    /// <summary>
    /// The <see cref="IConfiguration"/> interface is traversed through reflection. This
    /// annotation provides a way to override the default configuration operation for
    /// each property.
    /// </summary>
    /// <example>
    /// <code>
    ///     public class MyClass : IConfiguration {
    ///         [ConfigurationPropertionOptions(Confidential=true)]
    ///         public string MyProperty {get ; set;}
    ///     }
    /// </code>
    /// </example>
    [AttributeUsage(AttributeTargets.Property)]    
    public class ConfigurationPropertyAttribute : System.Attribute {
        
        /// <summary>
        /// Order in which this property is displayed.
        /// </summary>
        public int Order {get;set;}
        /// <summary>
        /// Is this a confidential property whose value should be 
        /// encrypted by the application when persisted?
        /// </summary>
        public bool Confidential {get;set;}
        /// <summary>
        /// Is this a required property?
        /// </summary>
        public bool Required {get;set;}
        /// <summary>
        /// Change the default help message key.
        /// </summary>
        public string HelpMessageKey {get;set;}
        /// <summary>
        /// Change the default display message key.
        /// </summary>
        public string DisplayMessageKey {get;set;}
        
        /**
         * List of operations for which this property must be specified.
         * This is used for the case where a connector may or may not
         * implement certain operations depending in the configuration.
         * The default value of "empty array" is special in that
         * it means that this property is applicable to all operations.
         * MUST be SPI operations
         */
        public Type [] OperationTypes {get;set;}
        
        /**
         * List of operations for which this property must be specified.
         * This is used for the case where a connector may or may not
         * implement certain operations depending in the configuration.
         * The default value of "empty array" is special in that
         * it means that this property is applicable to all operations.
         */
        public SafeType<SPIOperation> [] Operations {
            get {
                Type [] types = OperationTypes;
                SafeType<SPIOperation> [] rv = new SafeType<SPIOperation>[types.Length];
                for ( int i = 0; i < types.Length; i++ ) {
                    rv[i] =
                        SafeType<SPIOperation>.ForRawType(types[i]);
                }
                return rv;
            }
        }

        /// <summary>
        /// Default constructor 
        /// </summary>
        public ConfigurationPropertyAttribute() {
            Order = 1;
            Confidential = false;
            Required = false;
            HelpMessageKey = null;
            DisplayMessageKey = null;
            OperationTypes = new Type[0];
        }
    } 
    #endregion
    
    #region Connector
    /// <summary>
    /// This is the main interface to declare a connector. Developers must implement
    /// this interface. The life-cycle for a <see cref="IConnector"/> is as follows
    /// <see cref="IConnector.Init(IConfiguration)"/> is called then any of the operations implemented
    /// in the Connector and finally dispose. The <see cref="IConnector.Init(IConfiguration)"/> and
    /// <see cref="IDisposable.Dispose()"/> allow for block operations. For instance bulk creates or
    /// deletes and the use of before and after actions. Once <see cref="IDisposable.Dispose()"/> is
    /// called the <see cref="IConnector"/> object is discarded.
    /// </summary>
    public interface Connector : IDisposable {
        
        /// <summary>
        /// Initialize the connector with its configuration. For instance in a JDBC
        /// Connector this would include the database URL, password, and user.
        /// </summary>
        /// <param name="configuration">instance of the <see cref="IConfiguration"/> object implemented by
        /// the <see cref="IConnector"/> developer and populated with information
        /// in order to initialize the <see cref="IConnector"/>.</param>
        void Init(Configuration configuration);
    }
    #endregion
    
    #region PoolableConnector
    /**
     * To be implemented by Connectors that wish to be pooled.
     */
    public interface PoolableConnector : Connector {
        /**
         * Checks to see if the connector is still alive.
         * @throws RuntimeException If no longer alive.
         */
        void CheckAlive();
    }
    #endregion

}
