// <copyright file="ExchangeConnector.cs" company="Sun Microsystems, Inc.">
// ====================
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// 
// Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
// 
// The contents of this file are subject to the terms of the Common Development 
// and Distribution License("CDDL") (the "License").  You may not use this file 
// except in compliance with the License.
// 
// You can obtain a copy of the License at 
// http://IdentityConnectors.dev.java.net/legal/license.txt
// See the License for the specific language governing permissions and limitations 
// under the License. 
// 
// When distributing the Covered Code, include this CDDL Header Notice in each file
// and include the License file at identityconnectors/legal/license.txt.
// If applicable, add the following below this CDDL Header, with the fields 
// enclosed by brackets [] replaced by your own identifying information: 
// "Portions Copyrighted [year] [name of copyright owner]"
// ====================
// </copyright>
// <author>Tomas Knappek</author>

namespace Org.IdentityConnectors.Exchange
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.Management.Automation.Runspaces;

    using Data;

    using Org.IdentityConnectors.ActiveDirectory;
    using Org.IdentityConnectors.Common;
    using Org.IdentityConnectors.Framework.Common.Objects;
    using Org.IdentityConnectors.Framework.Spi;

    /// <summary>
    /// MS Exchange extension of Active Directory connector.
    /// Full featured connector, <see cref="LegacyExchangeConnector"/> for limited functionality connector.
    /// LegacyExchangeConnector will be extension of this class, once ready.
    /// </summary>    
    public class PSExchangeConnector : ActiveDirectoryConnector
    {        
        /// <summary>
        /// MailBox object class name
        /// </summary>
        public const string MailboxName = "mailbox";

        /// <summary>
        /// MailUser object class name
        /// </summary>
        public const string MailUserName = "mailuser";
        
        /// <summary>
        /// MailBox object class, based on <see cref="MailboxName"/>
        /// </summary>
        public static readonly ObjectClass Mailbox = new ObjectClass(MailboxName);

        /// <summary>
        /// MailUser object class, based on <see cref="MailUserName"/>
        /// </summary>
        public static readonly ObjectClass MailUser = new ObjectClass(MailUserName);

        /// <summary>
        /// This Class name - used for logging purposes
        /// </summary>
        private static readonly string ClassName = typeof(PSExchangeConnector).ToString();
        
        /// <summary>
        /// Configuration instance variable, <see cref="Init"/> method for assignment
        /// </summary>
        private ExchangeConfiguration configuration;

        /// <summary>
        /// Runspace instance variable, it is managed resource - has to be released
        /// </summary>
        private RunSpaceInstance runspace;

        /// <summary>
        /// Map of object class infos, used for <see cref="Schema"/> generating
        /// </summary>
        private IDictionary<ObjectClass, ObjectClassInfo> mapOcInfo;
        
        /// <summary>
        /// Implementation of CreateOp.Create
        /// </summary>
        /// <param name="oclass">Object class</param>(oc
        /// <param name="attributes">Object attributes</param>
        /// <param name="options">Operation options</param>
        /// <returns><see cref="Uid"/> of the created object</returns>
        public override Uid Create(
            ObjectClass oclass,
            ICollection<ConnectorAttribute> attributes,
            OperationOptions options)
        {
            const string METHOD = "Create";
            Debug.WriteLine(METHOD + ":entry", ClassName);
            
            // first create the object in AD
            Uid uid = base.Create(oclass, attributes, options);
            
            try
            {                
                if (oclass.Is(MailboxName))
                {
                    // enable mailbox for person
                    Command cmd = ExchangeUtility.GetCommand(CommandInfo.EnableMailbox, attributes);
                    this.runspace.InvokePipeline(cmd);
                }
                else if (oclass.Is(MailUserName))
                {
                    // enable mailuser
                    Command cmd = ExchangeUtility.GetCommand(CommandInfo.EnableMailUser, attributes);
                    this.runspace.InvokePipeline(cmd);                    
                }

                Debug.WriteLine(METHOD + ":exit", ClassName);
            }
            catch
            {
                // do the rollback - delete the object by uid
                // no need to check the uid is null, ensured by the create contract
                this.Delete(oclass, uid, options);
                throw;
            }
            
            return uid;
        }

        /// <summary>
        /// Implementation of UpdateOp.Update
        /// </summary>
        /// <param name="type">Update type</param>
        /// <param name="oclass">Object class</param>
        /// <param name="attributes">Object attributes</param>
        /// <param name="options">Operation options</param>
        /// <returns><see cref="Uid"/> of the updated object</returns>
        public override Uid Update(
            UpdateType type,
            ObjectClass oclass,
            ICollection<ConnectorAttribute> attributes,
            OperationOptions options)
        {
            // TODO: Implement Update
            return base.Update(type, oclass, attributes, options);
        }

        /// <summary>
        /// Tests if the connector is properly configured and ready
        /// </summary>
        public override void Test()
        {
            // validate the configuration first, this will check AD configuration too
            this.configuration.Validate();

            // AD validation (includes configuration validation too)
            base.Test();

            // runspace check
            this.runspace.Test();
        }
        
        /// <summary>
        /// Implementation of SynOp.Sync
        /// </summary>
        /// <param name="objClass">Object class</param>
        /// <param name="token">Syncronization token</param>
        /// <param name="handler">Handler for syncronization results</param>
        /// <param name="options">Operation options, can be null</param>
        public override void Sync(
            ObjectClass objClass,
            SyncToken token,
            SyncResultsHandler handler,
            OperationOptions options)
        {
            // TODO: implement Sync
            base.Sync(objClass, token, handler, options);
        }
        
        /// <summary>
        /// Implementation of SynOp.GetLatestSyncToken
        /// </summary>
        /// <param name="objectClass">Object class</param>
        /// <returns><see cref="SyncToken" /> of the last sync</returns>
        public override SyncToken GetLatestSyncToken(ObjectClass objectClass)
        {
            // TODO: Implement GetLatestSyncToken
            return base.GetLatestSyncToken(objectClass);
        }
        
        /// <summary>
        /// Implementation of SearchOp.ExecuteQuery
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="query">Query to execute</param>
        /// <param name="handler">Result handler</param>
        /// <param name="options">Operation options</param>
        public override void ExecuteQuery(
            ObjectClass oclass,
            string query,
            ResultsHandler handler,
            OperationOptions options)
        {
            // TODO: Implement ExecuteQuery
            base.ExecuteQuery(oclass, query, handler, options);
        }
        
        /// <summary>
        /// Implementation of SearchOp.CreateFilterTranslator
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="options">Operation options</param>
        /// <returns>Exchange specific Filter translator</returns>
        public override Org.IdentityConnectors.Framework.Common.Objects.Filters.FilterTranslator<string> CreateFilterTranslator(
            ObjectClass oclass,
            OperationOptions options)
        {
            // TODO: Implement CreateFilterTranslator
            return base.CreateFilterTranslator(oclass, options);
        }

        /// <summary>
        /// Inits the connector with configuration injected
        /// </summary>
        /// <param name="configuration">Initialized Exchange configuration</param>
        public override void Init(Configuration configuration)
        {
            base.Init(configuration);
            this.configuration = (ExchangeConfiguration)configuration;

            // create runspace instance, will be alive as long as the connector instance is alive
            this.runspace = new RunSpaceInstance(RunSpaceInstance.SnapIn.Exchange);

            // read the object class info definitions
            this.mapOcInfo = ExchangeUtility.GetOCInfo();
        }
                
        /// <summary>
        /// Dispose resources, <see cref="IDisposable"/>
        /// </summary>
        public sealed override void Dispose()
        {
            this.Dispose(true);
            GC.SuppressFinalize(this);                        
        }

        /// <summary>
        /// Dispose the resources we use
        /// </summary>
        /// <param name="disposing">true if called from <see cref="ExchangeConnector.Dispose()"/></param>
        protected virtual void Dispose(bool disposing)
        {
            if (disposing)
            {
                // free managed resources
                if (this.runspace != null)
                {
                    this.runspace.Dispose();
                    this.runspace = null;
                }
            }            
        }

        /// <summary>
        /// Defines the supported object classes by the connector, used for schema building
        /// </summary>
        /// <returns>List of supported object classes</returns>
        protected override ICollection<ObjectClass> GetSupportedObjectClasses()
        {
            ICollection<ObjectClass> objectClasses = base.GetSupportedObjectClasses();
            Assertions.NullCheck(objectClasses, "ocList");

            ICollection<ObjectClass> ourObjectClass = new List<ObjectClass>(objectClasses);

            // add our object classes
            ourObjectClass.Add(Mailbox);
            ourObjectClass.Add(MailUser);
            return ourObjectClass;
        }

        /// <summary>
        /// Gets the object class info for specified object class, used for schema building
        /// </summary>
        /// <param name="oc">ObjectClass to get info for</param>
        /// <returns>ObjectClass' ObjectClassInfo</returns>
        protected override ObjectClassInfo GetObjectClassInfo(ObjectClass oc)
        {
            ObjectClassInfo ret = CollectionUtil.GetValue(this.mapOcInfo, oc, null) ?? base.GetObjectClassInfo(oc);
            Assertions.NullCheck(ret, "ret");
            return ret;  
        }

        /// <summary>
        /// Command definition object, uses internally <see cref="SerializableCommandInfo"/>,
        /// intended to be one place for all the PowerShell commands definition
        /// </summary>
        internal sealed class CommandInfo
        {
            /// <summary>
            /// Enable-Mailbox command meta info
            /// </summary>
            internal static readonly CommandInfo EnableMailbox = new CommandInfo("Enable-Mailbox");

            /// <summary>
            /// Set-Mailbox command meta info
            /// </summary>
            internal static readonly CommandInfo SetMailbox = new CommandInfo("Set-Mailbox");

            /// <summary>
            /// Enable-MailUser command meta info
            /// </summary>
            internal static readonly CommandInfo EnableMailUser = new CommandInfo("Enable-MailUser");

            /// <summary>
            /// Set-MailUser command meta info
            /// </summary>
            internal static readonly CommandInfo SetMailUser = new CommandInfo("Set-MailUser");

            /// <summary>
            /// Get-MailUser command meta info
            /// </summary>
            internal static readonly CommandInfo GetMailUser = new CommandInfo("Get-MailUser");

            /// <summary>
            /// Get-User command meta info
            /// </summary>
            internal static readonly CommandInfo GetUser = new CommandInfo("Get-User");

            /// <summary>
            /// Get-Mailbox command meta info
            /// </summary>
            internal static readonly CommandInfo GetMailbox = new CommandInfo("Get-Mailbox");

            /// <summary>
            /// List of SerializableCommandInfo object - will be read from persistence
            /// </summary>
            private static IList<SerializableCommandInfo> serCmdInfos;

            /// <summary>
            /// Private placeholder for concrete <see cref="SerializableCommandInfo"/>
            /// </summary>
            private SerializableCommandInfo serCmdInfo;

            /// <summary>
            /// Initializes a new instance of the <see cref="CommandInfo" /> class. 
            /// , made private to be immutable
            /// </summary>
            /// <param name="name">Command name</param>
            private CommandInfo(string name)
            {
                this.Name = name;                
            }

            /// <summary>
            /// Gets Comamnd Name
            /// </summary>
            internal string Name { get; private set; }

            /// <summary>
            /// Gets Comand Parameters
            /// </summary>
            internal IList<string> Parameters
            {
                get
                {
                    return this.SerCmdInfo.Parameters;
                }
            }

            /// <summary>
            /// Gets Name parameter
            /// </summary>
            internal string NameParameter
            {
                get
                {
                    return this.SerCmdInfo.NameParameter;
                }
            }

            /// <summary>
            /// Gets SerCmdInfo.
            /// </summary>
            private SerializableCommandInfo SerCmdInfo
            {
                get
                {
                    // lazy init
                    if (serCmdInfos == null)
                    {
                        serCmdInfos = PersistenceUtility.ReadCommandInfo();
                    }

                    if (this.serCmdInfo == null)
                    {
                        foreach (SerializableCommandInfo info in serCmdInfos)
                        {
                            if (info.Name.Equals(this.Name))
                            {
                                this.serCmdInfo = info;
                                break;
                            }
                        }
                    }

                    return this.serCmdInfo;
                }
            }            
        }
    } 
}
 
