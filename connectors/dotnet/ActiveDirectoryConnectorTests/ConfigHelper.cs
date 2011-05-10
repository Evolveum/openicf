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
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Test.Common;

namespace Org.IdentityConnectors.ActiveDirectory
{
    internal static class ConfigHelper
    {
        #region Configuration property name constants
        public static readonly string CONFIG_PROPERTY_USER = "config_user";
        public static readonly string CONFIG_PROPERTY_PASSWORD = "config_password";
        public static readonly string CONFIG_PROPERTY_CONTAINER = "config_container";
        public static readonly string CONFIG_PROPERTY_SCRIPT_USER_LOCAL = "config_script_user_local";
        public static readonly string CONFIG_PROPERTY_SCRIPT_PASSWORD_LOCAL = "config_script_password_local";
        public static readonly string CONFIG_PROPERTY_SCRIPT_USER_DOMAIN = "config_script_user_domain";
        public static readonly string CONFIG_PROPERTY_SCRIPT_PASSWORD_DOMAIN = "config_script_password_domain";
        public static readonly string CONFIG_PROPERTY_LDAPHOSTNAME = "config_ldap_hostname";
        public static readonly string CONFIG_PROPERTY_SEARCH_CONTEXT = "config_search_context";
        public static readonly string config_PROPERTY_SYNC_CONTAINER_ROOT = "config_sync_container_root";
        public static readonly string config_PROPERTY_SYNC_CONTAINER_CHILD = "config_sync_container_child";
        public static readonly string CONFIG_PROPERTY_DOMAIN_NAME = "config_domain_name";
        public static readonly string CONFIG_PROPERTY_SYNC_DOMAIN_CONTROLLER = "config_sync_domain_controller";
        public static readonly string CONFIG_PROPERTY_GC_DOMAIN_CONTROLLER = "config_sync_gc_domain_controller";
        public static readonly string TEST_PARAM_SHARED_HOME_FOLDER = "test_param_shared_home_folder";
        #endregion

        #region Methods
        /// <summary>
        /// Gets the configuration used by the unit tests to acces an AD resource.
        /// </summary>
        /// <returns>A new instance of <see cref="ActiveDirectoryConfiguration"/>.</returns>
        public static Configuration GetConfiguration()
        {
            var config = new ActiveDirectoryConfiguration
                             {
                                 ConnectorMessages = TestHelpers.CreateDummyMessages(),

                                 Container = TestHelpers.GetProperties(
                                     typeof( ActiveDirectoryConnector ) ).GetProperty<string>(
                                     ConfigHelper.CONFIG_PROPERTY_CONTAINER ),
                                 DirectoryAdminName = TestHelpers.GetProperties(
                                     typeof( ActiveDirectoryConnector ) ).GetProperty<string>(
                                     ConfigHelper.CONFIG_PROPERTY_USER ),
                                 DirectoryAdminPassword = TestHelpers.GetProperties(
                                     typeof( ActiveDirectoryConnector ) ).GetProperty<string>(
                                     ConfigHelper.CONFIG_PROPERTY_PASSWORD ),
                                 DomainName = TestHelpers.GetProperties(
                                     typeof (ActiveDirectoryConnector)).GetProperty<string>(
                                     ConfigHelper.CONFIG_PROPERTY_DOMAIN_NAME),
                                 LDAPHostName = TestHelpers.GetProperties(
                                     typeof (ActiveDirectoryConnector)).GetProperty<string>(
                                     ConfigHelper.CONFIG_PROPERTY_LDAPHOSTNAME),
                                 SearchContext = TestHelpers.GetProperties(
                                     typeof (ActiveDirectoryConnector)).GetProperty<string>(
                                     ConfigHelper.CONFIG_PROPERTY_SEARCH_CONTEXT),
                                 SyncDomainController = TestHelpers.GetProperties(
                                     typeof (ActiveDirectoryConnector)).GetProperty<string>(
                                     ConfigHelper.CONFIG_PROPERTY_SYNC_DOMAIN_CONTROLLER),
                                 SyncGlobalCatalogServer = TestHelpers.GetProperties(
                                     typeof (ActiveDirectoryConnector)).GetProperty<string>(
                                     ConfigHelper.CONFIG_PROPERTY_GC_DOMAIN_CONTROLLER)
                             };
            return config;
        }
        #endregion
    }
}
