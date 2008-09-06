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
using System.IO;
using System.Xml;
using System.Collections.Generic;

using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;

using Org.IdentityConnectors.Framework.Test;

namespace FrameworkTests
{
    /// <summary>
    /// Description of TestHelperTests.
    /// </summary>
    [TestFixture]
    public class TestHelperTests
    {
        [Test]
        public void testLoadProperties()
        {
            string tmpFn = Path.GetTempFileName();
            try {
                // create some xml text
                TextWriter stringWriter = new StringWriter();
                XmlTextWriter w = new XmlTextWriter(stringWriter);
                w.WriteStartElement("configuration");
                w.WriteStartElement("property");
                w.WriteAttributeString("name", "bob");
                w.WriteAttributeString("value", "bobsValue");
                w.WriteEndElement();
                w.WriteStartElement("property");
                w.WriteAttributeString("name", "bob2");
                w.WriteAttributeString("value", "bob2sValue");
                w.WriteEndElement();
                w.Close();
                File.WriteAllText(tmpFn, stringWriter.ToString());
                // load the properties files
                IDictionary<string, string> dict =TestHelpers.LoadPropertiesFile(tmpFn);
                Assert.AreEqual(dict["bob"], "bobsValue");
            } finally {
                File.Delete(tmpFn);
            }
        }
        [Test]
        public void testGetProperties() {
            Assert.IsTrue(TestHelpers.GetProperty("Help", null).Equals("Me"));
        }
    }
}
