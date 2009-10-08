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
using System.IO;
using System.Xml;
using System.Collections.Generic;

using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;

using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Test.Common;

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
            try
            {
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
                IDictionary<string, string> dict = TestHelpers.LoadPropertiesFile(tmpFn);
                Assert.AreEqual(dict["bob"], "bobsValue");
            }
            finally
            {
                File.Delete(tmpFn);
            }
        }
        [Test]
        public void testGetProperties()
        {
            Assert.IsTrue(TestHelpers.GetProperty("Help", null).Equals("Me"));
        }

        [Test]
        public void testFillConfiguration()
        {
            TestConfiguration testConfig = new TestConfiguration();
            // There is no "Foo" property in the config bean. We want to ensure
            // that TestHelpers.FillConfiguration() does not fail for unknown properties.
            IDictionary<string, object> configData = new Dictionary<string, object>();
            configData["Host"] = "example.com";
            configData["Port"] = 1234;
            configData["Foo"] = "bar";
            TestHelpers.FillConfiguration(testConfig, configData);

            Assert.AreEqual("example.com", testConfig.Host);
            Assert.AreEqual(1234, testConfig.Port);
        }

        public class TestConfiguration : Configuration
        {

            public ConnectorMessages ConnectorMessages
            {
                get
                {
                    return null;
                }
                set
                {
                    Assert.Fail("Should not set ConnectorMessages");
                }
            }

            public void Validate()
            {
                Assert.Fail("Should not call Validate()");
            }

            public String Host { get; set; }

            public int Port { get; set; }

            public String Unused { get; set; }
        }
    }
}
