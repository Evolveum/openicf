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
using System.Text;
using System.Diagnostics;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Spi.Operations;

namespace Org.IdentityConnectors.ActiveDirectory
{
    public class ActiveDirectoryConfiguration : Org.IdentityConnectors.Framework.Spi.AbstractConfiguration
    {
        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_DirectoryAdminName",
            Required = true, HelpMessageKey = "help_DirectoryAdminName", Order = 1)]
        public String DirectoryAdminName
        { get; set; }

        [ConfigurationProperty(Confidential = true, DisplayMessageKey = "display_DirectoryAdminPassword",
            Required = true, HelpMessageKey = "help_DirectoryAdminPassword", Order = 2)]
        public String DirectoryAdminPassword
        { get; set; }

        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_ObjectClass", HelpMessageKey = "help_ObjectClass", Order = 3)]
        public String ObjectClass
        { get; set; }

        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_Container", 
            Required=true, HelpMessageKey = "help_Container", Order = 4)]
        public String Container
        { get; set; }

        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_CreateHomeDirectory", HelpMessageKey = "help_CreateHomeDirectory", Order = 5)]
        public bool CreateHomeDirectory { get; set; }

        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_LDAPHostName", HelpMessageKey = "help_LDAPHostName", Order = 6)]
        public String LDAPHostName
        { get; set; }

        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_SearchChildDomains", HelpMessageKey = "help_SearchChildDomains", Order = 7)]
        public bool SearchChildDomains { get; set; }

        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_domainName",
            Required = true, HelpMessageKey = "help_domainName", Order = 8)]
        public String DomainName
        { get; set; }

        [ConfigurationProperty(OperationTypes = new Type[] { typeof(SyncOp) }, Confidential = false, DisplayMessageKey = "display_SyncGlobalCatalogServer", HelpMessageKey = "help_SyncGlobalCatalogServer", Order = 9)]
        public String SyncGlobalCatalogServer
        { get; set; }

        [ConfigurationProperty(OperationTypes = new Type[] { typeof(SyncOp) }, Confidential = false, DisplayMessageKey = "display_SyncDomainController", HelpMessageKey = "help_SyncDomainController", Order=10)]
        public String SyncDomainController
        { get; set; }

        private string _searchContext = string.Empty;    
        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_SearchContext", HelpMessageKey = "help_SearchContext", Order=11)]
        public String SearchContext
        {
            get
            {
                if((_searchContext == null) || (_searchContext.Length == 0))
                {
                    return Container;
                }
                return _searchContext;
            } 
            
            set
            {
                _searchContext = value;
            }
        }

        public ActiveDirectoryConfiguration()
        {
            DomainName = "";
            Container = "";
            DirectoryAdminName = "administrator";
            ObjectClass = "User";
            CreateHomeDirectory = true;
            SearchChildDomains = false;
            LDAPHostName = "";
        }

        public override void Validate()
        {
            String message = "Configuration errors:  ";
            Boolean foundError = false;

            // can't lookup the schema without the domain name
            if ((DomainName == null) || (DomainName.Length == 0))
            {
                message += ConnectorMessages.Format(
                            "confReqParam_domainName", "Domain name not supplied  ");
                foundError = true;
            }

            if ((DirectoryAdminName == null) || (DirectoryAdminName.Length == 0))
            {
                message += ConnectorMessages.Format(
                            "confReqParam_adminName", "Directory administrator name not supplied  ");
                foundError = true;
            }

            if ((DirectoryAdminPassword == null) || (DirectoryAdminPassword.Length == 0))
            {
                message += ConnectorMessages.Format(
                            "confReqParam_adminPass", "Directory administrator password not supplied  ");
                foundError = true;
            }

            if ((ObjectClass == null) || (ObjectClass.Length == 0))
            {
                message += ConnectorMessages.Format(
                            "confReqParam_objClass", "ObjectClass was not supplied  ");
                foundError = true;
            }

            if ((Container == null) || (Container.Length == 0))
            {
                message += ConnectorMessages.Format(
                            "confReqParam_Container", "Container was not supplied  ");
                foundError = true;
            }

            if (foundError)
            {
                throw new ConfigurationException(message);
            }
        }
    }
}
