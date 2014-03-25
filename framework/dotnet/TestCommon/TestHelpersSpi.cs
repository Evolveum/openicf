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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 * Portions Copyrighted 2014 ForgeRock AS.
 */
using System.Collections.Generic;

using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Api;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;

namespace Org.IdentityConnectors.Test.Common.Spi
{
    /// <summary>
    /// Private use only, do not implement! Use the methods in 
    /// <see cref="Org.IdentityConnectors.Test.Common.TestHelpers"/> instead.
    /// </summary>
    public interface TestHelpersSpi
    {
        APIConfiguration CreateTestConfiguration(SafeType<Connector> clazz,
                Configuration config);

        APIConfiguration CreateTestConfiguration(SafeType<Connector> clazz,
            PropertyBag configData, string prefix);

        void FillConfiguration(Configuration config,
                IDictionary<string, object> configData);

        SearchResult Search<T>(SearchOp<T> search,
                ObjectClass oclass,
                Filter filter,
                ResultsHandler handler,
                OperationOptions options) where T : class;

        ConnectorMessages CreateDummyMessages();
    }
}
