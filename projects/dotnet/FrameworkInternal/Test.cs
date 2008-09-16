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
using Org.IdentityConnectors.Framework.Test;

namespace Org.IdentityConnectors.Framework.Impl.Test
{
    public class TestHelpersImpl : TestHelpers {
        
        /**
         * Method for convenient testing of local connectors. 
         */
        protected override APIConfiguration CreateTestConfigurationImpl(SafeType<Connector> clazz,
                Configuration config) {
            LocalConnectorInfoImpl info = new LocalConnectorInfoImpl();
            info.ConnectorConfigurationClass=SafeType<Configuration>.Get(config);
            info.ConnectorClass=(clazz);
            info.ConnectorDisplayNameKey=("DUMMY_DISPLAY_NAME");
            info.ConnectorKey=(
                   new ConnectorKey(clazz.RawType.Name+".bundle",
                    "1.0",
                    clazz.RawType.Name));
            info.Messages=(new ConnectorMessagesImpl());
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
        protected override void SearchImpl<T>(SearchOp<T> search,
                ObjectClass oclass, 
                Filter filter, 
                ResultsHandler handler,
                OperationOptions options) {
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            RawSearcherImpl<T>.RawSearch(
                 search, oclass, filter, handler, options);
        }
    
    }

}
