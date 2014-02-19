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

using NUnit.Framework;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Api.Operations;
namespace FrameworkTests
{
    [TestFixture]
    public class SafeTypeTest
    {
        [Test]
        public void TestSafeType()
        {
            //compile-time type safe
            SafeType<APIOperation> op =
                SafeType<APIOperation>.Get<ScriptOnResourceApiOp>();
            Assert.AreEqual(typeof(ScriptOnResourceApiOp), op.RawType);
            //runtime type safe. needed for marshalling code, etc
            op =
                SafeType<APIOperation>.ForRawType(typeof(SchemaApiOp));
            Assert.AreEqual(typeof(SchemaApiOp), op.RawType);
        }
    }
}
