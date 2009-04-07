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
using System.Text;

using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Api;
using Org.IdentityConnectors.Framework.Impl.Api;
using Org.IdentityConnectors.Framework.Impl.Api.Local;
using Org.IdentityConnectors.Framework.Impl.Api.Local.Operations;
using Org.IdentityConnectors.Framework.Common;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;
using Org.IdentityConnectors.Test.Common.Spi;

namespace Org.IdentityConnectors.Framework.Impl.Test
{    
    public class TestHelpersImpl : TestHelpersSpi {
        
        /**
         * Method for convenient testing of local connectors. 
         */
        public APIConfiguration CreateTestConfiguration(SafeType<Connector> clazz,
                Configuration config) {
            LocalConnectorInfoImpl info = new LocalConnectorInfoImpl();
            info.ConnectorConfigurationClass=SafeType<Configuration>.Get(config);
            info.ConnectorClass=(clazz);
            info.ConnectorDisplayNameKey=("DUMMY_DISPLAY_NAME");
            info.ConnectorKey=(
                   new ConnectorKey(clazz.RawType.Name+".bundle",
                    "1.0",
                    clazz.RawType.Name));
            info.Messages=(this.CreateDummyMessages());
            APIConfigurationImpl rv = new APIConfigurationImpl();
            rv.IsConnectorPoolingSupported=(
                    IsConnectorPoolingSupported(clazz));
            ConfigurationPropertiesImpl properties =
                CSharpClassProperties.CreateConfigurationProperties(config);
            rv.ConfigurationProperties=(properties);
            rv.ConnectorInfo=(info);
            rv.SupportedOperations=(
                    FrameworkUtil.GetDefaultSupportedOperations(clazz));
            info.DefaultAPIConfiguration=(
                    rv);
            return rv;
        }
                
        private static bool IsConnectorPoolingSupported(SafeType<Connector> clazz) {
            return ReflectionUtil.IsParentTypeOf(typeof(PoolableConnector),clazz.RawType);
        }

        /**
         * Performs a raw, unfiltered search at the SPI level,
         * eliminating duplicates from the result set.
         * @param search The search SPI
         * @param oclass The object class - passed through to
         * connector so it may be null if the connecor
         * allowing it to be null. (This is convenient for
         * unit tests, but will not be the case in general)
         * @param filter The filter to search on
         * @param handler The result handler
         * @param options The options - may be null - will
         *  be cast to an empty OperationOptions
         */
        public void Search<T>(SearchOp<T> search,
                ObjectClass oclass, 
                Filter filter, 
                ResultsHandler handler,
                OperationOptions options) where T : class
        {
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            RawSearcherImpl<T>.RawSearch(
                 search, oclass, filter, handler, options);
        }
        
        public ConnectorMessages CreateDummyMessages() {
            return new DummyConnectorMessages();
        }
        
        private class DummyConnectorMessages : ConnectorMessages {
            public String Format(String key, String dflt, params Object [] args) {
                StringBuilder builder = new StringBuilder();
                builder.Append(key);
                builder.Append(": ");
                String sep = "";
                foreach (Object arg in args ) {
                    builder.Append(sep);
                    builder.Append(arg);
                    sep=", ";
                }
                return builder.ToString();
            }
        }    
    }
}
