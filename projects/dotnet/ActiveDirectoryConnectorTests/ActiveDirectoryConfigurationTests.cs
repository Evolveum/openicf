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
using System.Collections.Generic;
using System.Linq;
using System.Text;
using NUnit.Framework;
using Org.IdentityConnectors.Test.Common;
using Org.IdentityConnectors.Framework.Common.Exceptions;

namespace Org.IdentityConnectors.ActiveDirectory
{
    [TestFixture]
    public class ActiveDirectoryConfigurationTests
    {
        [Test]
        public void TestProperties()
        {
            var sut = new ActiveDirectoryConfiguration
                          {
                              ConnectorMessages = TestHelpers.CreateDummyMessages()
                          };

            var container = TestHelpers.GetProperties(
                typeof( ActiveDirectoryConnector ) ).GetProperty<string>( ConfigHelper.CONFIG_PROPERTY_CONTAINER );
            sut.Container = container;
            Assert.AreEqual( container, sut.Container, "Container" );

            //test with the negate of the default value
            var createHomeDirectory = !sut.CreateHomeDirectory;
            sut.CreateHomeDirectory = createHomeDirectory;
            Assert.AreEqual( createHomeDirectory, sut.CreateHomeDirectory, "CreateHomeDirectory" );

            var directoryAdminName = TestHelpers.GetProperties(
                typeof( ActiveDirectoryConnector ) ).GetProperty<string>( ConfigHelper.CONFIG_PROPERTY_USER );
            sut.DirectoryAdminName = directoryAdminName;
            Assert.AreEqual( directoryAdminName, sut.DirectoryAdminName, "DirectoryAdminName" );

            var directoryAdminPassword = TestHelpers.GetProperties(
                typeof( ActiveDirectoryConnector ) ).GetProperty<string>( ConfigHelper.CONFIG_PROPERTY_PASSWORD );
            sut.DirectoryAdminPassword = directoryAdminPassword;
            Assert.AreEqual( directoryAdminPassword, sut.DirectoryAdminPassword, "DirectoryAdminPassword" );

            var domainName = TestHelpers.GetProperties(
                typeof( ActiveDirectoryConnector ) ).GetProperty<string>( ConfigHelper.CONFIG_PROPERTY_DOMAIN_NAME );
            sut.DomainName = domainName;
            Assert.AreEqual( domainName, sut.DomainName, "DomainName" );

            var ldapHostName = TestHelpers.GetProperties(
                typeof( ActiveDirectoryConnector ) ).GetProperty<string>( ConfigHelper.CONFIG_PROPERTY_LDAPHOSTNAME );
            sut.LDAPHostName = ldapHostName;
            Assert.AreEqual( ldapHostName, sut.LDAPHostName, "LDAPHostName" );

            const string objectClassName = "DOES NOT MATTER";
            sut.ObjectClass = objectClassName;
            Assert.AreEqual( objectClassName, sut.ObjectClass, "ObjectClass" );

            //test with the negate of the default value
            var searchChildDomains = !sut.SearchChildDomains;
            sut.SearchChildDomains = searchChildDomains;
            Assert.AreEqual( searchChildDomains, sut.SearchChildDomains, "SearchChildDomains" );

            var searchContext = TestHelpers.GetProperties(
                typeof( ActiveDirectoryConnector ) ).GetProperty<string>( ConfigHelper.CONFIG_PROPERTY_SEARCH_CONTEXT );
            sut.SearchContext = searchContext;
            Assert.AreEqual( searchContext, sut.SearchContext, "SearchContext" );

            var syncDomainController = TestHelpers.GetProperties(
                typeof( ActiveDirectoryConnector ) ).GetProperty<string>( ConfigHelper.CONFIG_PROPERTY_SYNC_DOMAIN_CONTROLLER );
            sut.SyncDomainController = syncDomainController;
            Assert.AreEqual( syncDomainController, sut.SyncDomainController, "SyncDomainController" );

            var syncGlobalCatalogServer = TestHelpers.GetProperties(
                typeof( ActiveDirectoryConnector ) ).GetProperty<string>( ConfigHelper.CONFIG_PROPERTY_GC_DOMAIN_CONTROLLER );
            sut.SyncGlobalCatalogServer = syncGlobalCatalogServer;
            Assert.AreEqual( syncGlobalCatalogServer, sut.SyncGlobalCatalogServer, "SyncGlobalCatalogServer" );
        }

        [Test]
        public void TestValidate()
        {
            var sut = (ActiveDirectoryConfiguration)ConfigHelper.GetConfiguration();
            //test if the default configuration is valid, hence the following tests will fail only if the 
            //changed property is incorrect
            sut.Validate();

            var domainName = sut.DomainName;
            try
            {
                sut.DomainName = string.Empty;
                sut.Validate();
                Assert.Fail( "Exception was not thrown for empty DomainName" );
            }
            catch(ConfigurationException)
            {
                sut.DomainName = domainName;
            }

            var directoryAdminName = sut.DirectoryAdminName;
            try
            {
                sut.DirectoryAdminName = string.Empty;
                sut.Validate();
                Assert.Fail( "Exception was not thrown for empty DirectoryAdminName" );
            }
            catch (ConfigurationException)
            {
                sut.DirectoryAdminName = directoryAdminName;
            }

            var directoryAdminPassword = sut.DirectoryAdminPassword;
            try
            {
                sut.DirectoryAdminPassword = string.Empty;
                sut.Validate();
                Assert.Fail( "Exception was not thrown for empty DirectoryAdminPassword" );
            }
            catch (ConfigurationException)
            {
                sut.DirectoryAdminPassword = directoryAdminPassword;
            }

            var objectClass = sut.ObjectClass;
            try
            {
                sut.ObjectClass = string.Empty;
                sut.Validate();
                Assert.Fail( "Exception was not thrown for empty ObjectClass" );
            }
            catch (ConfigurationException)
            {
                sut.ObjectClass = objectClass;
            }


            var container = sut.Container;
            try
            {
                sut.Container = string.Empty;
                sut.Validate();
                Assert.Fail( "Exception was not thrown for empty Container" );

                sut.Container = "CN=ClaytonFarlow.DC=NotMyCompany.DC=com";
                sut.Validate();
                Assert.Fail( "Exception was not thrown for Container containing periods" );
            }
            catch (ConfigurationException)
            {
                sut.Container = container;
            }
        }
    }
}
