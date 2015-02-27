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
 * Portions Copyrighted 2012-2014 ForgeRock AS.
 */
using System;
using System.Reflection;
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;
using System.Diagnostics;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using System.DirectoryServices;
using DS = System.DirectoryServices;
using System.DirectoryServices.ActiveDirectory;
using System.Text;
using Org.IdentityConnectors.Common.Script;
using System.Globalization;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;

namespace Org.IdentityConnectors.Sample
{
    public enum UpdateType
    {
        ADD,
        DELETE,
        REPLACE
    }

    /// <summary>
    /// The Active Directory Connector
    /// </summary>
    [ConnectorClass("connector_displayName",
                      typeof(SampleConfiguration),
                      MessageCatalogPaths = new String[] { "Org.IdentityConnectors.Sample.Messages" }
                      )]
    public class SampleConnector : CreateOp, Connector, SchemaOp, DeleteOp,
        SearchOp<String>, TestOp, UpdateOp, AuthenticateOp, PoolableConnector
	{
        public SampleConnector()
        {
        }

        public virtual Uid Create(ObjectClass oclass,
            ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            Console.WriteLine("Create called.");
            return new Uid("uid1");
        }

        // implementation of Connector
        public virtual void Init(Configuration configuration)
        {
            Console.WriteLine("Init called.");
            configuration.Validate();
        }

        public virtual void Dispose()
        {
        }

        public virtual Schema Schema()
        {
            Console.WriteLine("Schema called.");
            return null;
        }

        public FilterTranslator<String> CreateFilterTranslator(ObjectClass oclass, OperationOptions options)
        {
            return null;
        }

        public void ExecuteQuery(ObjectClass oclass, string query, ResultsHandler handler, OperationOptions options)
        {
            Console.WriteLine("ExecuteQuery called");
        }

        public virtual void Test()
        {
            Console.WriteLine("Test called");
        }

        public Uid Update(ObjectClass objclass, Uid uid, ICollection<ConnectorAttribute> attrs, OperationOptions options)
        {
            Console.WriteLine("Update called.");
            return uid;
        }

        public void Delete(ObjectClass objClass, Uid uid, OperationOptions options)
        {
            Console.WriteLine("Delete called.");
        }

        public Uid Authenticate(ObjectClass objectClass, string username,
            Org.IdentityConnectors.Common.Security.GuardedString password,
            OperationOptions options)
        {
            Console.WriteLine("Authenticate called.");
            return new Uid("uid1");
        }

        public void CheckAlive()
        {
            Console.WriteLine("CheckAlive called");
        }
    }
}
