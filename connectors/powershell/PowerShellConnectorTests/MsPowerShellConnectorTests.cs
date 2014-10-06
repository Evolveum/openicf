/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Management.Automation;
using System.Reflection;
using System.Security;
using MsPowerShellTestModule;
using NUnit.Framework;
using Org.ForgeRock.OpenICF.Connectors.MsPowerShell;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Common.Security;
using Org.IdentityConnectors.Framework.Api;
using Org.IdentityConnectors.Framework.Api.Operations;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Common.Serializer;
using Org.IdentityConnectors.Framework.Impl.Api.Local;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Test.Common;

namespace MSPowerShellConnectorTests
{
    [TestFixture]
    public class MsPowerShellConnectorTests
    {
        private ConnectorFacade _facade = null;
        private static readonly ObjectClass Test = new ObjectClass("__TEST__");
        private static readonly ObjectClass Sample = new ObjectClass("__SAMPLE__");
        private static readonly ObjectClass Unknown = new ObjectClass("__UNKNOWN__");
        private const String Password = "Passw0rd";

        [TestFixtureSetUp]
        public void Init()
        {
            GetFacade();
        }

        [TestFixtureTearDown]
        public void Cleanup()
        {
            if (null != _facade)
            {
                ((LocalConnectorFacadeImpl)_facade).Dispose();
                _facade = null;
            }
        }

        // =====================================================
        // Authenticate Operation Test
        // =====================================================

        [Test]
        [Category("Authenticate")]
        [ExpectedException(typeof(ConnectorSecurityException))]
        public void TestAuthenticate1()
        {
            GetFacade().Authenticate(Test, "TEST1", new GuardedString(GetSecure(Password)), null);
        }

        [Test]
        [Category("Authenticate")]
        [ExpectedException(typeof(InvalidCredentialException))]
        public void TestAuthenticate2()
        {
            GetFacade().Authenticate(Test, "TEST2", new GuardedString(GetSecure(Password)), null);
        }

        [Test]
        [Category("Authenticate")]
        [ExpectedException(typeof(InvalidPasswordException))]
        public void TestAuthenticate3()
        {
            GetFacade().Authenticate(Test, "TEST3", new GuardedString(GetSecure(Password)), null);
        }

        [Test]
        [Category("Authenticate")]
        [ExpectedException(typeof(PermissionDeniedException))]
        public void TestAuthenticate4()
        {
            GetFacade().Authenticate(Test, "TEST4", new GuardedString(GetSecure(Password)), null);
        }

        [Test]
        [Category("Authenticate")]
        [ExpectedException(typeof(PasswordExpiredException))]
        public void TestAuthenticate5()
        {
            GetFacade().Authenticate(Test, "TEST5", new GuardedString(GetSecure(Password)), null);
        }

        [Test]
        [Category("Authenticate")]
        [ExpectedException(typeof(UnknownUidException))]
        public void TestAuthenticate7()
        {
            GetFacade().Authenticate(Test, "TEST7", new GuardedString(GetSecure(Password)), null);
        }

        [Test]
        [Category("Authenticate")]
        [ExpectedException(typeof(NotSupportedException))]
        public void TestAuthenticateUnsupportedObjectClass()
        {
            GetFacade().Authenticate(Unknown, "TESTOK1", new GuardedString(GetSecure(Password)), null);
        }

        [Test]
        [Category("Authenticate")]
        public void TestAuthenticateOk()
        {
            Assert.AreEqual("TESTOK1", GetFacade().Authenticate(Test, "TESTOK1", new GuardedString(GetSecure(Password)), null).GetUidValue());
        }

        [Test]
        [Category("Authenticate")]
        public void TestAuthenticateEmpty()
        {
            Assert.AreEqual("TESTOK2", GetFacade().Authenticate(Test, "TESTOK2", new GuardedString(GetSecure("")), null).GetUidValue());
        }

        [Test]
        [Category("Authenticate")]
        [ExpectedException(typeof(ConnectorException))]
        public void TestAuthenticateNotEmpty()
        {
            GetFacade().Authenticate(Test, "TESTOK2", new GuardedString(GetSecure("NOT_EMPTY")), null);
        }

         //=====================================================
         //Create Operation Test
         //=====================================================

        [Test]
        [Category("Create")]
        public void TestCreate()
        {
            Assert.NotNull(CreateTestUser("Foo"));
        }

        [Test]
        [Category("Create")]
        [ExpectedException(typeof(AlreadyExistsException))]
        public void TestCreate1()
        {
            GetFacade().Create(Test, GetTestCreateConnectorObject("TEST1"), null);
        }

        [Test]
        [Category("Create")]
        [ExpectedException(typeof(InvalidAttributeValueException))]
        public void TestCreate2()
        {
            GetFacade().Create(Test, GetTestCreateConnectorObject("TEST2"), null);
        }

        [Test]
        [Category("Create")]
        [ExpectedException(typeof(ArgumentException))]
        public void TestCreate3()
        {
            GetFacade().Create(Test, GetTestCreateConnectorObject("TEST3"), null);
        }

        [Test]
        [Category("Create")]
        [ExpectedException(typeof(RetryableException))]
        public void TestCreate4()
        {
            GetFacade().Create(Test, GetTestCreateConnectorObject("TEST4"), null);
        }

        [Test]
        [Category("Create")]
        public void TestCreate5()
        {
            Assert.AreEqual("TEST5", GetFacade().Create(Test, GetTestCreateConnectorObject("TEST5"), null).GetUidValue());
        }

        [Test]
        [Category("Create")]
        public void TestCreateTestRunAs()
        {
            ICollection<ConnectorAttribute> createAttributes = GetTestCreateConnectorObject("TEST5");
            var builder = new OperationOptionsBuilder();
            builder.RunAsUser = "admin";
            builder.RunWithPassword = new GuardedString(GetSecure(Password));
            Uid uid = GetFacade().Create(Test, createAttributes, builder.Build());
            Assert.AreEqual(uid.GetUidValue(), "TEST5");
        }

        [Test]
        [Category("Create")]
        [ExpectedException(typeof(ConnectorSecurityException))]
        public void TestCreateTestRunAsFailed()
        {
            ICollection<ConnectorAttribute> createAttributes = GetTestCreateConnectorObject("TEST5");
            var builder = new OperationOptionsBuilder();
            builder.RunAsUser = "admin";
            var secret = new SecureString();
            "__FAKE__".ToCharArray().ToList().ForEach(secret.AppendChar);
            builder.RunWithPassword = new GuardedString(secret);
            Uid uid = GetFacade().Create(Test, createAttributes, builder.Build());
            Assert.AreEqual(uid.GetUidValue(), "TEST5");
        }

        [Test]
        [Category("Create")]
        [ExpectedException(typeof(OperationTimeoutException))]
        public void TestCreateTimeOut()
        {
            ICollection<ConnectorAttribute> createAttributes = GetTestCreateConnectorObject("TIMEOUT");
            GetFacade().Create(Test, createAttributes, null);
            Assert.Fail();
        }

        [Test]
        [Category("Create")]
        [ExpectedException(typeof(NotSupportedException))]
        public void TestCreateUnsupportedObjectClass()
        {
            ICollection<ConnectorAttribute> createAttributes = GetTestCreateConnectorObject("TEST5");
            GetFacade().Create(Unknown, createAttributes, null);
        }

        // =====================================================
        // Delete Operation Test
        // =====================================================

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(UnknownUidException))]
        public void TestDelete1()
        {
            GetFacade().Delete(Test, new Uid("TEST1"), null);
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(PreconditionFailedException))]
        public void TestDelete4()
        {
            GetFacade().Delete(Test, new Uid("TEST4"), null);
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(PreconditionRequiredException))]
        public void TestDelete5()
        {
            GetFacade().Delete(Test, new Uid("TEST5"), null);
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(OperationTimeoutException))]
        public void TestDeleteTimeOut()
        {
            GetFacade().Delete(Test, new Uid("TIMEOUT"), null);
            Assert.Fail();
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(ConfigurationException))]
        public void TestDeleteCEException()
        {
            GetFacade().Delete(Test, new Uid("TESTEX_CE"), null);
            Assert.Fail();
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(ConnectionBrokenException))]
        public void TestDeleteCBException()
        {
            GetFacade().Delete(Test, new Uid("TESTEX_CB"), null);
            Assert.Fail();
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(ConnectionFailedException))]
        public void TestDeleteCFException()
        {
            GetFacade().Delete(Test, new Uid("TESTEX_CF"), null);
            Assert.Fail();
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(ConnectorException))]
        public void TestDeleteCException()
        {
            GetFacade().Delete(Test, new Uid("TESTEX_C"), null);
            Assert.Fail();
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(ConnectorIOException))]
        public void TestDeleteCIOException()
        {
            GetFacade().Delete(Test, new Uid("TESTEX_CIO"), null);
            Assert.Fail();
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(OperationTimeoutException))]
        public void TestDeleteOTException()
        {
            GetFacade().Delete(Test, new Uid("TESTEX_OT"), null);
            Assert.Fail();
        }

        [Test]
        [Category("Delete")]
        [ExpectedException(typeof(NotSupportedException))]
        public void TestDeleteUnsupportedObjectClass()
        {
            GetFacade().Delete(Unknown, new Uid("001"), null);
        }

        // End of Groovy from

        //[Test]
        //[Category("Delete")]
        //public void TestDeleteOK()
        //{
        //    // Will succeed
        //    GetFacade().Delete(ObjectClass.ACCOUNT, new Uid("smith"), null);
        //}


        //[Test]
        //[Category("Delete")]
        //[ExpectedException(typeof(UnknownUidException))]
        //public void TestDeleteNOK()
        //{
        //    // Will fail
        //    GetFacade().Delete(ObjectClass.ACCOUNT, new Uid("doe"), null);
        //}

        // =====================================================
        // ResolveUsername Operation Test
        // =====================================================

        [Test]
        [Category("ResolveUsername")]
        public void TestResolveUsername1()
        {
            Assert.AreEqual("123", GetFacade().ResolveUsername(ObjectClass.ACCOUNT, "TESTOK1", null).GetUidValue());
        }

        [Test]
        [Category("ResolveUsername")]
        [ExpectedException(typeof(UnknownUidException))]
        public void TestResolveUsername2()
        {
            GetFacade().ResolveUsername(ObjectClass.ACCOUNT, "NON_EXIST", null);
        }

        [Test]
        [Category("ResolveUsername")]
        [ExpectedException(typeof(NotSupportedException))]
        public void TestResolveUsername3()
        {
            GetFacade().ResolveUsername(Unknown, "NON_EXIST", null);
        }

        // =====================================================
        // Schema Operation Test
        // =====================================================

        [Test]
        [Category("Schema")]
        public void TestSchema1()
        {
            Schema schema = GetFacade().Schema();
            Assert.NotNull(schema.FindObjectClassInfo("__TEST__"));
            Assert.NotNull(schema.FindObjectClassInfo("__ACCOUNT__"));
            Assert.NotNull(schema.FindObjectClassInfo("__GROUP__"));
            Console.WriteLine(SerializerUtil.SerializeXmlObject(schema, true));
        }


        // =======================================================================
        // ScriptOnConnector Operation Test
        // =======================================================================

        [Test]
        [Category("ScriptOnConnector")]
        public void TestScriptOnConnector()
        {
            var builder = new ScriptContextBuilder();
            builder.ScriptLanguage = "POWERShell";
            builder.ScriptText = "Write-Warning \"Test\"; return $Connector.Arguments.uid.GetUidValue()";
            var uid = new Uid("foo", "12345");
            builder.AddScriptArgument("uid", uid);
            var res = GetFacade().RunScriptOnConnector(builder.Build(), null) as Collection<object>;
            if (res != null)
            {
                Assert.AreEqual(res[0], uid.GetUidValue());    
            }
            
        }

        // =======================================================================
        // ScriptOnResource Operation Test
        // =======================================================================

        //[Test]
        //[Category("ScriptOnResource")]
        //public void TestScriptOnResource()
        //{
        //    ScriptContextBuilder builder = new ScriptContextBuilder();
        //    builder.ScriptLanguage = "PowerShell";
        //    builder.ScriptText = "return $arg";
        //    builder.AddScriptArgument("arg01", true);
        //    builder.AddScriptArgument("arg02", "String");
        //    Dictionary<object, object> result = (Dictionary<object, object>)GetFacade().RunScriptOnResource(builder.Build(), null);
        //    Assert.AreEqual(true, result["arg01"]);
        //    Assert.AreEqual("String", result["arg02"]);
        //}

        //[Test]
        //[Category("ScriptOnResource")]
        //[ExpectedException(typeof(InvalidAttributeValueException))]
        //public void TestScriptOnResourceFail()
        //{
        //    ScriptContextBuilder builder = new ScriptContextBuilder();
        //    builder.ScriptLanguage = "BASH";
        //    builder.ScriptText = "test";
        //    GetFacade().RunScriptOnResource(builder.Build(), null);
        //    Assert.Fail();
        //}

        // =====================================================
        // Get Operation Test
        // =====================================================

        [Test]
        [Category("Get")]
        public void TestGet()
        {
            var co = GetFacade().GetObject(Test, new Uid("1"), null);
            Assert.IsNotNull(co);
        }

        // =====================================================
        // Search Operation Test
        // =====================================================

        [Test]
        [Category("Search")]
        public void TestSearch()
        {
            var co = GetFacade().GetObject(Test, new Uid("UID01"), null);
            Assert.IsNotNull(co);
        }

        [Test]
        [Category("Search")]
        public void TestSearchByteAttributes()
        {
            OperationOptionsBuilder builder = new OperationOptionsBuilder();
            builder.AttributesToGet = new[] { "attributeByte", "attributeByteMultivalue", "attributeByteArray", "attributeByteArrayMultivalue" };
            var co = GetFacade().GetObject(Test, new Uid("UID01"), builder.Build());
            Assert.IsNotNull(co);
            
            IList<Object> value = co.GetAttributeByName("attributeByte").Value;
            Assert.AreEqual(1, value.Count);
            Assert.IsInstanceOf(typeof(byte), value[0]);

            value = co.GetAttributeByName("attributeByteMultivalue").Value;
            Assert.AreEqual(2, value.Count);
            Assert.IsInstanceOf(typeof(byte), value[0]);
            Assert.IsInstanceOf(typeof(byte), value[1]);

            value = co.GetAttributeByName("attributeByteArray").Value;
            Assert.AreEqual(1, value.Count);
            Assert.IsInstanceOf(typeof(byte[]), value[0]);

            value = co.GetAttributeByName("attributeByteArrayMultivalue").Value;
            Assert.AreEqual(2, value.Count);
            Assert.IsInstanceOf(typeof(byte[]), value[0]);
            Assert.IsInstanceOf(typeof(byte[]), value[1]);
        }

        [Test]
        [Category("Search")]
        public void TestSearchAttributes()
        {
            OperationOptionsBuilder builder = new OperationOptionsBuilder();
            builder.AttributesToGet = new[] { "attributeString", "attributeMap"};
            GetFacade().Search(Test, null, new ResultsHandler
            {
                Handle = connectorObject =>
                {
                    Assert.AreEqual(4, connectorObject.GetAttributes().Count);
                    return true;
                }
            }, builder.Build());
        }

        [Test]
        [Category("Search")]
        public void TestSearch1()
        {
            IList<ConnectorObject> result = TestHelpers.SearchToList(GetFacade(), new ObjectClass("__EMPTY__"), null);
            Assert.IsEmpty(result);
        }

        [Test]
        [Category("Search")]
        public virtual void TestSearch2()
        {
            ConnectorFacade search = GetFacade();
            for (int i = 0; i < 100; i++)
            {
                ICollection<ConnectorAttribute> co = GetTestCreateConnectorObject(string.Format("TEST{0:D5}", i));
                co.Add(ConnectorAttributeBuilder.Build("sortKey", i));
                search.Create(ObjectClass.ACCOUNT, co, null);
            }

            OperationOptionsBuilder builder = new OperationOptionsBuilder { PageSize = 10, SortKeys = new[] { new SortKey("sortKey", false) } };
            SearchResult result = null;

            ICollection<ConnectorObject> resultSet = new HashSet<ConnectorObject>();
            int pageIndex = 0;
            int index = 101;

            while ((result = search.Search(ObjectClass.ACCOUNT, FilterBuilder.StartsWith(ConnectorAttributeBuilder.Build(Name.NAME, "TEST")), new ResultsHandler()
            {
                Handle = connectorObject =>
                {
                    int? idx = ConnectorAttributeUtil.GetIntegerValue(connectorObject.GetAttributeByName("sortKey"));
                    Assert.IsTrue(idx < index);
                    if (idx != null) { index = (int)idx; }
                    resultSet.Add(connectorObject);
                    return true;
                }
            }, builder.Build())).PagedResultsCookie != null)
            {
                
                builder = new OperationOptionsBuilder(builder.Build()) { PagedResultsCookie = result.PagedResultsCookie };
                Assert.AreEqual(10 * ++pageIndex, resultSet.Count);
            }
            Assert.AreEqual(9, pageIndex);
            Assert.AreEqual(100, resultSet.Count);
        }

        [Test]
        [Category("Search")]
        [ExpectedException(typeof(NotSupportedException))]
        public virtual void TestSearchUnsupportedObjectClass()
        {
            GetFacade().GetObject(Unknown, new Uid("1"), null);
        }

        // End of Groovy from

        /*FilterBuilder.EqualTo(ConnectorAttributeBuilder.Build(Name.NAME, "Foo"))*/
        [Test]
        [Category("Search")]
        public void TestNullQuery()
        {
            var result = new List<ConnectorObject>();
            GetFacade().Search(Test, null, new ResultsHandler()
            {
                Handle = connectorObject => { result.Add(connectorObject); return true; }
            }, null);
            Assert.AreEqual(10, result.Count);
        }

        //[Test]
        //[Category("Search")]
        //public void TestEqualsQuery()
        //{
        //    var result = new List<ConnectorObject>();
        //    GetFacade().Search(ObjectClass.ACCOUNT, FilterBuilder.EqualTo(ConnectorAttributeBuilder.Build(Uid.NAME, "001")), new ResultsHandler()
        //    {
        //        Handle = connectorObject => { result.Add(connectorObject); return true; }
        //    }, null);

        //    Assert.AreEqual(1, result.Count);
        //    var co = result[0];
        //    Assert.IsTrue("User 1".Equals(co.Name.GetNameValue()));
        //}

        //[Test]
        //[Category("Search")]
        //public void TestStartsWithQuery()
        //{
        //    var result = new List<ConnectorObject>();
        //    GetFacade().Search(ObjectClass.ACCOUNT, FilterBuilder.StartsWith(ConnectorAttributeBuilder.Build("sn", "SMITH")), new ResultsHandler()
        //    {
        //        Handle = connectorObject => { result.Add(connectorObject); return true; }
        //    }, null);

        //    Assert.AreEqual(1, result.Count);
        //    var co = result[0];
        //    Assert.IsTrue("Smith".Equals(ConnectorAttributeUtil.GetAsStringValue(co.GetAttributeByName("sn"))));
        //}

        // =====================================================
        // Sync Operation Test
        // =====================================================

        static readonly object[] SyncObjectClassProvider =
        {
            new object[] { ObjectClass.ACCOUNT },
            new object[] { Test } 
        };

        [Test, TestCaseSource("SyncObjectClassProvider")]
        [Category("Sync")]
        public void TestSyncNull(ObjectClass objectClass)
        {
            var result = new List<SyncDelta>();
            SyncToken lastToken = GetFacade().Sync(objectClass, new SyncToken(5), new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(10, lastToken.Value);
            Assert.AreEqual(0, result.Count);
        }


        [Test, TestCaseSource("SyncObjectClassProvider")]
        [Category("Sync")]
        public void TestSyncAccount(ObjectClass objectClass)
        {
            var result = new List<SyncDelta>();
            SyncToken lastToken = GetFacade().Sync(objectClass, new SyncToken(0), new SyncResultsHandler()
                {
                    Handle = delta =>
                    {
                        result.Add(delta);
                        return true;
                    }
                }, null);
            Assert.AreEqual(1, lastToken.Value);
            Assert.AreEqual(1, result.Count);
            SyncDelta sdelta = result[0];
            result.RemoveAt(0);
            Assert.AreEqual(SyncDeltaType.CREATE, sdelta.DeltaType);
            Assert.AreEqual(44, sdelta.Object.GetAttributes().Count);

            lastToken = GetFacade().Sync(objectClass, lastToken, new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(2, lastToken.Value);
            Assert.AreEqual(1, result.Count);
            sdelta = result[0];
            result.RemoveAt(0);
            Assert.AreEqual(SyncDeltaType.UPDATE, sdelta.DeltaType);
            Assert.AreEqual(44, sdelta.Object.GetAttributes().Count);

            lastToken = GetFacade().Sync(objectClass, lastToken, new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(3, lastToken.Value);
            Assert.AreEqual(1, result.Count);
            sdelta = result[0];
            result.RemoveAt(0);
            Assert.AreEqual(SyncDeltaType.CREATE_OR_UPDATE, sdelta.DeltaType);
            Assert.AreEqual(44, sdelta.Object.GetAttributes().Count);

            lastToken = GetFacade().Sync(objectClass, lastToken, new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(4, lastToken.Value);
            Assert.AreEqual(1, result.Count);
            sdelta = result[0];
            result.RemoveAt(0);
            Assert.AreEqual(SyncDeltaType.UPDATE, sdelta.DeltaType);
            Assert.AreEqual(44, sdelta.Object.GetAttributes().Count);
            Assert.AreEqual("001", sdelta.PreviousUid.GetUidValue());

            lastToken = GetFacade().Sync(objectClass, lastToken, new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(5, lastToken.Value);
            Assert.AreEqual(1, result.Count);
            sdelta = result[0];
            result.RemoveAt(0);
            Assert.AreEqual(SyncDeltaType.DELETE, sdelta.DeltaType);

            lastToken = GetFacade().Sync(objectClass, lastToken, new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(10, lastToken.Value);
            Assert.IsEmpty(result);

            lastToken = GetFacade().Sync(objectClass, lastToken, new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(17, lastToken.Value);
            Assert.AreEqual(4, result.Count);
            result.Clear();

            lastToken = GetFacade().Sync(ObjectClass.GROUP, lastToken, new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(16, lastToken.Value);
            Assert.AreEqual(3, result.Count);

        }

        [Test]
        [Category("Sync")]
        public void TestSyncAll()
        {
            var result = new List<SyncDelta>();
            SyncToken lastToken =
                    GetFacade().Sync(ObjectClass.ALL, new SyncToken(0), new SyncResultsHandler()
                    {
                        Handle = delta =>
                        {
                            result.Add(delta);
                            return true;
                        }
                    }, null);
            Assert.AreEqual(17, lastToken.Value);
            Assert.AreEqual(7, result.Count);
            int index = 10;

            foreach (var delta in result)
            {
                Assert.AreEqual(index++, delta.Token.Value);
                Assert.AreEqual(((int)delta.Token.Value) % 2 == 0 ? ObjectClass.ACCOUNT : ObjectClass.GROUP,
                    delta.Object.ObjectClass);
            }
        }

        [Test]
        [Category("Sync")]
        public void TestSyncSample()
        {
            var result = new List<SyncDelta>();
            SyncToken lastToken = GetFacade().Sync(Sample, new SyncToken(5), new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual("SAMPLE", lastToken.Value);
            //Assert.AreEqual(4, result.Count);
        }

        [Test]
        [Category("Sync")]
        public void TestSyncToken()
        {
            Assert.AreEqual(17, GetFacade().GetLatestSyncToken(ObjectClass.ACCOUNT).Value);
            Assert.AreEqual(16, GetFacade().GetLatestSyncToken(ObjectClass.GROUP).Value);
            Assert.AreEqual(17, GetFacade().GetLatestSyncToken(ObjectClass.ALL).Value);
            Assert.AreEqual(0, GetFacade().GetLatestSyncToken(Test).Value);
            Assert.IsInstanceOf(typeof(string), GetFacade().GetLatestSyncToken(Sample).Value);
        }

        [Test]
        [Category("Sync")]
        [ExpectedException(typeof(NotSupportedException))]
        public void TestSyncUnsupportedObjectClass()
        {
            GetFacade().Sync(Unknown, new SyncToken(0), new SyncResultsHandler()
            {
                Handle = o => true
            }, null);
        }
        [Test]
        [Category("Sync")]
        [ExpectedException(typeof(NotSupportedException))]
        public void TestSyncTokenUnsupportedObjectClass()
        {
            GetFacade().GetLatestSyncToken(Unknown);
        }

        //End of Groovy from

        [Test]
        [Category("Sync")]
        public void TestSyncGroup()
        {
            var result = new List<SyncDelta>();

            GetFacade().Sync(ObjectClass.GROUP, new SyncToken(0), new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    result.Add(delta);
                    return true;
                }
            }, null);
            Assert.AreEqual(3, result.Count);
            SyncDelta sdelta = result[0];
            result.RemoveAt(0);
            Assert.AreEqual(SyncDeltaType.CREATE_OR_UPDATE, sdelta.DeltaType);
            Assert.AreEqual(4, sdelta.Object.GetAttributes().Count);
        }

        //[Test]
        //[Category("Sync")]
        //[ExpectedException(typeof(ArgumentException))]
        //public void TestSyncTest0()
        //{
        //    var result = new List<SyncDelta>();

        //    GetFacade().Sync(Test, new SyncToken(0), new SyncResultsHandler()
        //    {
        //        Handle = delta =>
        //        {
        //            result.Add(delta);
        //            return true;
        //        }
        //    }, null);
        //}

        //[Test]
        //[Category("Sync")]
        //[ExpectedException(typeof(ArgumentException))]
        //public void TestSyncTest1()
        //{
        //    var result = new List<SyncDelta>();

        //    GetFacade().Sync(Test, new SyncToken(1), new SyncResultsHandler()
        //    {
        //        Handle = delta =>
        //        {
        //            result.Add(delta);
        //            return true;
        //        }
        //    }, null);
        //}

        //[Test]
        //[Category("Sync")]
        //[ExpectedException(typeof(ArgumentException))]
        //public void TestSyncTest2()
        //{
        //    var result = new List<SyncDelta>();

        //    GetFacade().Sync(Test, new SyncToken(2), new SyncResultsHandler()
        //    {
        //        Handle = delta =>
        //        {
        //            result.Add(delta);
        //            return true;
        //        }
        //    }, null);
        //}

        //[Test]
        //[Category("Sync")]
        //[ExpectedException(typeof(ArgumentException))]
        //public void TestSyncTest3()
        //{
        //    var result = new List<SyncDelta>();

        //    GetFacade().Sync(Test, new SyncToken(3), new SyncResultsHandler()
        //    {
        //        Handle = delta =>
        //        {
        //            result.Add(delta);
        //            return true;
        //        }
        //    }, null);
        //}

        // =======================================================================
        // Test Operation Test
        // =======================================================================

        [Test]
        [Category("test")]
        [ExpectedException(typeof(MissingFieldException))]
        public void TestTest()
        {
            GetFacade().Test();
        }

        // =====================================================
        // Update Operation Test
        // =====================================================

        [Test]
        [Category("Update")]
        public void TestUpdate()
        {
            Uid uid = CreateTestUser("TESTOK01");
            var updateAttributes = new List<ConnectorAttribute>(1);
            updateAttributes.Add(ConnectorAttributeBuilder.Build("email", "foo@example.com"));

            uid = GetFacade().Update(ObjectClass.ACCOUNT, uid, updateAttributes, null);
        }

        [Test]
        [Category("Update")]
        [ExpectedException(typeof(InvalidAttributeValueException), ExpectedMessage = "Expecting non null value")]

        public void TestUpdateFailEmpty()
        {
            Uid uid = CreateTestUser("FAIL01");
            var updateAttributes = new List<ConnectorAttribute>(1) {ConnectorAttributeBuilder.Build("email")};

            uid = GetFacade().Update(ObjectClass.ACCOUNT, uid, updateAttributes, null);
            Assert.Fail("Connector operation should fail");
        }

        [Test]
        [Category("Update")]
        [ExpectedException(typeof(InvalidAttributeValueException), ExpectedMessage = "Expecting Boolean value")]
        public void TestUpdateFailType()
        {
            Uid uid = CreateTestUser("FAIL02");
            var updateAttributes = new List<ConnectorAttribute>(1);
            updateAttributes.Add(ConnectorAttributeBuilder.Build("active", "true"));

            uid = GetFacade().Update(ObjectClass.ACCOUNT, uid, updateAttributes, null);
            Assert.Fail("Connector operation should fail");
        }

        [Test]
        [Category("Update")]
        [ExpectedException(typeof(InvalidAttributeValueException), ExpectedMessage = "Expecting single value")]

        public void TestUpdateFailMulti()
        {
            Uid uid = CreateTestUser("FAIL03");
            var updateAttributes = new List<ConnectorAttribute>(1);
            updateAttributes.Add(ConnectorAttributeBuilder.Build("userName", "name1", "name2"));

            uid = GetFacade().Update(ObjectClass.ACCOUNT, uid, updateAttributes, null);
            Assert.Fail("Connector operation should fail");
        }

        [Test]
        [Category("Update")]
        [ExpectedException(typeof(InvalidAttributeValueException), ExpectedMessage = "Try update non modifiable attribute")]
        public void TestUpdateFailReadOnly()
        {
            Uid uid = CreateTestUser("FAIL04");
            var updateAttributes = new List<ConnectorAttribute>(1);
            updateAttributes.Add(ConnectorAttributeBuilder.Build("lastModified", "newValue"));

            uid = GetFacade().Update(ObjectClass.ACCOUNT, uid, updateAttributes, null);
            Assert.Fail("Connector operation should fail");
        }

        [Test]
        [Category("Update")]
        [ExpectedException(typeof(OperationTimeoutException))]
        public void TestUpdateTimeOut()
        {
            GetFacade().Update(Test, new Uid("TIMEOUT"),
                    CollectionUtil.NewSet(ConnectorAttributeBuilder.Build("null")), null);
            Assert.Fail();
        }

        [Test]
        [Category("Update")]
        [ExpectedException(typeof(NotSupportedException))]
        public void TestUpdateUnsupportedObjectClass()
        {
            var updateAttributes = new List<ConnectorAttribute>(1);
            updateAttributes.Add(ConnectorAttributeBuilder.Build("email", "foo@example.com"));
            GetFacade().Update(Unknown, new Uid("TESTOK1"), updateAttributes, null);
        }

        ///// End of From Groovy 

        //[Test]
        //[Category("Update")]
        //public void TestUpdate0()
        //{
        //    Uid uid = GetFacade().Update(ObjectClass.ACCOUNT, new Uid("Foo"), GetTestCreateConnectorObject("Foo"), null);
        //    Assert.NotNull(uid, "The Uid is null");
        //}

        //[Test]
        //[Category("Update")]
        //[ExpectedException(typeof(UnknownUidException))]
        //public void TestUpdate1()
        //{
        //    GetFacade().Update(Test, new Uid("TEST1"), GetTestUpdateConnectorObject("TEST1"), null);
        //}

        //[Test]
        //[Category("Update")]
        //[ExpectedException(typeof(InvalidAttributeValueException))]
        //public void TestUpdate2()
        //{
        //    GetFacade().Update(Test, new Uid("TEST2"), GetTestUpdateConnectorObject("TEST2"), null);
        //}

        //[Test]
        //[Category("Update")]
        //[ExpectedException(typeof(ArgumentException))]
        //public void TestUpdate3()
        //{
        //    GetFacade().Update(Test, new Uid("TEST3"), GetTestUpdateConnectorObject("TEST3"), null);
        //}

        //[Test]
        //[Category("Update")]
        //[ExpectedException(typeof(PreconditionFailedException))]
        //public void TestUpdate4()
        //{
        //    GetFacade().Update(Test, new Uid("TEST4"), GetTestUpdateConnectorObject("TEST4"), null);
        //}

        //[Test]
        //[Category("Update")]
        //[ExpectedException(typeof(PreconditionRequiredException))]
        //public void TestUpdate5()
        //{
        //    GetFacade().Update(Test, new Uid("TEST5"), GetTestUpdateConnectorObject("TEST5"), null);
        //}

        //[Test]
        //[Category("Update")]
        //[ExpectedException(typeof(RuntimeException))]
        //public void TestUpdate7()
        //{
        //    GetFacade().Update(ObjectClass.GROUP, new Uid("Group1"), GetTestUpdateConnectorObject("Group1"), null);
        //}

        protected ConnectorFacade GetFacade()
        {
            var f = CreateConnectorFacade(SafeType<Connector>.ForRawType(typeof(MsPowerShellConnector)));
            Assert.NotNull(f, "The Facade Creation fails");
            return f;
        }

        public ConnectorFacade CreateConnectorFacade(SafeType<Connector> clazz)
        {
            if (null == _facade)
            {
                PropertyBag propertyBag = TestHelpers.GetProperties(clazz.RawType);

                string assemblyFolder = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
                string testModulePath = Path.GetFullPath(Path.Combine(assemblyFolder, "..\\..\\..\\Samples\\Tests\\TestModule.psm1"));
                string objectChacheModulePath = typeof(ObjectCacheLibrary).Assembly.Location;

                var importModules = new string[] { testModulePath, objectChacheModulePath };
                

                APIConfiguration impl = TestHelpers.CreateTestConfiguration(clazz, propertyBag, "configuration");
                impl.ConfigurationProperties.SetPropertyValue("PsModulesToImport", importModules);
                // 
                impl.ProducerBufferSize = 0;
                impl.ResultsHandlerConfiguration.EnableAttributesToGetSearchResultsHandler = false;
                impl.ResultsHandlerConfiguration.EnableCaseInsensitiveFilter = false;
                impl.ResultsHandlerConfiguration.EnableFilteredResultsHandler = false;
                impl.ResultsHandlerConfiguration.EnableNormalizingResultsHandler = false;

                //We timeout after 10s
                impl.SetTimeout(SafeType<APIOperation>.ForRawType(typeof(CreateApiOp)), 10000);
                impl.SetTimeout(SafeType<APIOperation>.ForRawType(typeof(UpdateApiOp)), 10000);
                impl.SetTimeout(SafeType<APIOperation>.ForRawType(typeof(DeleteApiOp)), 10000);
                _facade = ConnectorFacadeFactory.GetInstance().NewInstance(impl);
            }
            return _facade;
        }

        protected virtual Uid CreateTestUser(string username)
        {
            ICollection<ConnectorAttribute> createAttributes = GetTestCreateConnectorObject(username);
            ConnectorFacade facade = GetFacade();
            Uid uid = facade.Create(ObjectClass.ACCOUNT, createAttributes, null);
            Assert.IsNotNull(uid);
            ConnectorObject co = facade.GetObject(ObjectClass.ACCOUNT, uid, null);
            Assert.AreEqual(co.Uid, uid);
            return uid;
        }

        private List<ConnectorAttribute> GetTestCreateConnectorObject(String name)
        {
            var attrs = new List<ConnectorAttribute>
            {
                ConnectorAttributeBuilder.Build(Name.NAME, name),
                ConnectorAttributeBuilder.Build("userName", name),
                ConnectorAttributeBuilder.Build("email", name + "@example.com"),
                ConnectorAttributeBuilder.BuildEnabled(true),    
                ConnectorAttributeBuilder.Build("firstName", "John"),
                ConnectorAttributeBuilder.Build("surName", name.ToUpper()),
                ConnectorAttributeBuilder.BuildPassword(new GuardedString(GetSecure(Password))),
                ConnectorAttributeBuilder.Build(PredefinedAttributes.DESCRIPTION, "Description"),
                ConnectorAttributeBuilder.Build("groups", "group1", "group2"),                
            };
            return attrs;
        }

        private List<ConnectorAttribute> GetTestUpdateConnectorObject(String name)
        {
            var attrs = new List<ConnectorAttribute>
            {
                ConnectorAttributeBuilder.Build("mail", name + "@example2.com"),
                ConnectorAttributeBuilder.BuildPassword(new GuardedString(GetSecure(Password+"2"))),
                ConnectorAttributeBuilder.BuildEnabled(false)
            };
            return attrs;
        }
        private SecureString GetSecure(String password)
        {
            var secure = new SecureString();
            foreach (char c in password)
            {
                secure.AppendChar(c);
            }
            return secure;
        }
    }
}
