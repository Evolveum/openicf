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
using System.Collections.Generic;
using System.Globalization;
using Org.IdentityConnectors.Common.Pooling;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;
namespace org.identityconnectors.testconnector
{
    public class TstConnectorConfig : AbstractConfiguration
    {    
        public override void Validate() {
        }
    }
    
    [ConnectorClass("TestConnector",
                      typeof(TstConnectorConfig),
                      MessageCatalogPath="TestBundleV2.Messages"
                        )]
    public class TstConnector : CreateOp, Connector, SchemaOp
    {
        public Uid Create(ObjectClass oclass, ICollection<ConnectorAttribute> attrs, OperationOptions options) {
            String version = "2.0";
            return new Uid(version);
        }
        public void Init(Configuration cfg) {
        }

        public void Dispose() {
            
        }
            
        public Schema Schema() {
            return null;
        }

        
    }
}
