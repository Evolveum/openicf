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
using System.Collections.Concurrent;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Common.Security;
using System.Collections.ObjectModel;
using System.Diagnostics;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Spi;

namespace Org.ForgeRock.OpenICF.Connectors.MsPowerShell
{
    [ConfigurationClass(true, new[] { "MsPowerShellHost" })]
    public class MsPowerShellConfiguration : AbstractConfiguration, StatefulConfiguration
    {
        private Collection<String> _validScripts;

        [ConfigurationProperty(DisplayMessageKey = "display_AuthenticateScriptFileName", HelpMessageKey = "help_AuthenticateScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 1)]
        public String AuthenticateScriptFileName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_CreateScriptFileName", HelpMessageKey = "help_CreateScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 2)]
        public String CreateScriptFileName
        { get; set; }


        [ConfigurationProperty(DisplayMessageKey = "display_DeleteScriptFileName", HelpMessageKey = "help_DeleteScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 3)]
        public String DeleteScriptFileName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_ResolveUsernameScriptFileName", HelpMessageKey = "help_ResolveUsernameScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 4)]
        public String ResolveUsernameScriptFileName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_SchemaScriptFileName", HelpMessageKey = "help_SchemaScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 5)]
        public String SchemaScriptFileName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_SearchScriptFileName", HelpMessageKey = "help_SearchScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 7)]
        public String SearchScriptFileName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_SyncScriptFileName", HelpMessageKey = "help_SyncScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 8)]
        public String SyncScriptFileName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_TestScriptFileName", HelpMessageKey = "help_TestScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 9)]
        public String TestScriptFileName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_UpdateScriptFileName", HelpMessageKey = "help_UpdateScriptFileName",
            GroupMessageKey = "group_OperationScripts", Order = 10)]
        public String UpdateScriptFileName
        { get; set; }

        [ConfigurationProperty(Required = true, DisplayMessageKey = "display_VariablesPrefix", HelpMessageKey = "help_VariablesPrefix",
            GroupMessageKey = "group_PowerShell", Order = 12)]
        public String VariablesPrefix
        { get; set; }

        [ConfigurationProperty(Required = true, DisplayMessageKey = "display_QueryFilterType", HelpMessageKey = "help_QueryFilterType",
            GroupMessageKey = "group_PowerShell", Order = 16)]
        public String QueryFilterType
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_SubstituteUidAndNameInQueryFilter", HelpMessageKey = "help_SubstituteUidAndNameInQueryFilter",
            GroupMessageKey = "group_PowerShell", Order = 15)]
        public Boolean SubstituteUidAndNameInQueryFilter
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_UidAttributeName", HelpMessageKey = "help_UidAttributeName",
            GroupMessageKey = "group_PowerShell", Order = 13)]
        public String UidAttributeName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_NameAttributeName", HelpMessageKey = "help_NameAttributeName",
            GroupMessageKey = "group_PowerShell", Order = 14)]
        public String NameAttributeName
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_PsModulesToImport", HelpMessageKey = "help_PsModulesToImport",
            GroupMessageKey = "group_PowerShell", Order = 17)]
        public string[] PsModulesToImport
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_Host", HelpMessageKey = "help_Host",
            GroupMessageKey = "group_PowerShell", Order = 18)]
        public String Host
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_Port", HelpMessageKey = "help_Port",
            GroupMessageKey = "group_PowerShell", Order = 19)]
        public String Port
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_Login", HelpMessageKey = "help_Login",
            GroupMessageKey = "group_PowerShell", Order = 20)]
        public String Login
        { get; set; }

        [ConfigurationProperty(DisplayMessageKey = "display_Password", HelpMessageKey = "help_Password",
            GroupMessageKey = "group_PowerShell", Confidential  = true, Order = 14)]
        public GuardedString Password
        { get; set; }


        public MsPowerShellConfiguration()
        {
            AuthenticateScriptFileName = "";
            CreateScriptFileName = "";
            DeleteScriptFileName = "";
            ResolveUsernameScriptFileName = "";
            SchemaScriptFileName = "";
            SearchScriptFileName = "";
            SyncScriptFileName = "";
            TestScriptFileName = "";
            UpdateScriptFileName = "";
            VariablesPrefix = "Connector";
            QueryFilterType = MsPowerShellConnector.Visitors.Map.ToString();
            SubstituteUidAndNameInQueryFilter = false;
            UidAttributeName = Uid.NAME;
            NameAttributeName = Name.NAME;
            PsModulesToImport = new string[]{};
            Host = "";
            Port = null;
            Login = "";
            Password = null;
        }

        public override void Validate()
        {
            var scriptsList = new String[] 
            {
                AuthenticateScriptFileName,
                CreateScriptFileName, 
                DeleteScriptFileName,                
                ResolveUsernameScriptFileName, 
                SchemaScriptFileName,
                SearchScriptFileName, 
                SyncScriptFileName,
                TestScriptFileName,
                UpdateScriptFileName
            };

            _validScripts = new Collection<string>();

            Trace.TraceInformation("Entering Validate() configuration");
            foreach (var file in scriptsList)
            {
                if (StringUtil.IsNotBlank(file))
                {
                    System.IO.FileStream fs = null;
                    try
                    {
                        fs = System.IO.File.OpenRead(file);
                        _validScripts.Add(file);
                    }
                    catch (Exception ex)
                    {
                        throw new ConfigurationException(ex);
                    }
                    finally
                    {
                        if (fs != null) fs.Dispose();
                    }
                }
            }

            if (StringUtil.IsBlank(VariablesPrefix))
            {
                throw new ConfigurationException("VariablesPrefix can not be empty or null");
            }

            if (!("Map".Equals(QueryFilterType, StringComparison.InvariantCultureIgnoreCase)
                || "Ldap".Equals(QueryFilterType, StringComparison.InvariantCultureIgnoreCase)
                || "Native".Equals(QueryFilterType, StringComparison.InvariantCultureIgnoreCase)
                || "AdPsModule".Equals(QueryFilterType, StringComparison.InvariantCultureIgnoreCase)))
            {
                throw new ConfigurationException("QueryFilterType must be Native|Map|Ldap|AdPsModule");
            }
        }

        public void Release()
        {
            if (null != _host)
            {
                lock (this)
                {
                    if (null != _host)
                    {
                        _host.Dispose();
                        _host = null;
                    }
                }
            }
        }

        public Collection<String> GetValidScripts()
        {
            return _validScripts;
        }


        private readonly ConcurrentDictionary<string, object> _propertyBag = new ConcurrentDictionary<string, object>();


        /// <summary>
        /// Returns the Dictionary shared between the Connector instances.
        /// 
        /// Shared map to store initialised resources which should be shared between
        /// the scripts.
        /// </summary>
        /// <returns> single instance of shared ConcurrentMap. </returns>
        public virtual ConcurrentDictionary<string, object> PropertyBag
        {
            get
            {
                return _propertyBag;
            }
        }

        private MsPowerShellHost _host = null;

        private MsPowerShellHost MsPowerShellHost
        {
            get
            {
                if (null == _host)
                {
                    lock (this)
                    {
                        if (null == _host)
                        {
                            _host = new MsPowerShellHost();
                        }
                    }
                }
                return _host;
            }
        }

    }
}
