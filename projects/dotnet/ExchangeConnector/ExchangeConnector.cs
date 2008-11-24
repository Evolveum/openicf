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
using System.Collections.Generic;
using System.Diagnostics;
using System.Management.Automation.Runspaces;
using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;

namespace Org.IdentityConnectors.Exchange
{
    /// <summary>
    /// MS Exchange extension of Active Directory connector
    /// </summary>
    //TODO:  what about MessageCatalogPath inheritance?
    [ConnectorClass("connector_displayName",
                    typeof(ExchangeConfiguration),
                    MessageCatalogPath = "Org.IdentityConnectors.Exchange.Messages"
                   )]    
    public class ExchangeConnector : ActiveDirectoryConnector
    {
        private static readonly string CLASS = typeof(ExchangeConnector).ToString();

        //object class names
        public const string MAILBOX_NAME = "mailbox";
        public const string MAILUSER_NAME = "mailuser";
        
        //object classes
        public static readonly ObjectClass MAILBOX = new ObjectClass(MAILBOX_NAME);
        public static readonly ObjectClass MAILUSER = new ObjectClass(MAILUSER_NAME);

        //local vars
        private ExchangeConfiguration _configuration = null;
        private RunSpaceInstance _runspace = null;
        private bool _disposed = false;
        private IDictionary<ObjectClass, ObjectClassInfo> _mapOcInfo = null;

        
        /// <summary>
        /// Implementation of CreateOp.Create
        /// </summary>
        /// <param name="oclass"></param>(oc
        /// <param name="attributes"></param>
        /// <param name="options"></param>
        /// <returns></returns>
        public override Uid Create(ObjectClass oclass,
                                   ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            const string METHOD = "Create";

            Debug.WriteLine(METHOD + ":entry", CLASS);

            
            //first create the object in AD
            Uid uid = base.Create(oclass, attributes, options);
            
            try
            {
                if (oclass.Equals(MAILBOX))
                {
                    //enable mailbox for person
                    Command cmd = ExchangeUtils.GetCommand(CommandInfo.ENABLE_MAILBOX, attributes);
                    _runspace.InvokePipeline(cmd);
                } else if (oclass.Equals(MAILUSER))
                {
                    //enable mailuser
                    Command cmd = ExchangeUtils.GetCommand(CommandInfo.ENABLE_MAILUSER, attributes);
                    _runspace.InvokePipeline(cmd);
                    
                }

                Debug.WriteLine(METHOD + ":exit", CLASS);

            }
            catch (Exception)
            {
                //do the rollback - delete the uid
                base.Delete(oclass, uid, options);
                throw;
            }
            
            return uid;
        }

        /// <summary>
        /// Implementation of UpdateOp.Update
        /// </summary>
        /// <param name="type"></param>
        /// <param name="oclass"></param>
        /// <param name="attributes"></param>
        /// <param name="options"></param>
        /// <returns></returns>
        public override Uid Update(UpdateType type, ObjectClass oclass,
                                   ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            //TODO: Implement Update
            return base.Update(type, oclass, attributes, options);
        }

        /// <summary>
        /// Tests if the connector is properly configured and ready
        /// </summary>
        public override void Test()
        {
            //validate the configuration first, this will check AD configuration too
            _configuration.Validate();
            //AD validation (includes configuration validation too)
            base.Test();
            //runspace check
            _runspace.Test();
        }
        
        /// <summary>
        /// Implementation of SynOp.Sync
        /// </summary>
        /// <param name="objClass"></param>
        /// <param name="token"></param>
        /// <param name="handler"></param>
        /// <param name="options"></param>
        public override void Sync(ObjectClass objClass, SyncToken token,
                                  SyncResultsHandler handler, OperationOptions options)
        {
            //TODO: implement Sync
            base.Sync(objClass, token, handler, options);
        }
        
        /// <summary>
        /// Implementation of SynOp.GetLatestSyncToken
        /// </summary>
        /// <returns></returns>
        public override SyncToken GetLatestSyncToken()
        {
            //TODO: Implement GetLatestSyncToken
            return base.GetLatestSyncToken();
        }
        
        /// <summary>
        /// Implementation of SearchOp.ExecuteQuery
        /// </summary>
        /// <param name="oclass"></param>
        /// <param name="query"></param>
        /// <param name="handler"></param>
        /// <param name="options"></param>
        public override void ExecuteQuery(ObjectClass oclass, string query,
                                          ResultsHandler handler, OperationOptions options)
        {
            //TODO: Implement ExecuteQuery
            base.ExecuteQuery(oclass, query, handler, options);
        }
        
        
        /// <summary>
        /// Implementation of SearchOp.CreateFilterTranslator
        /// </summary>
        /// <param name="oclass"></param>
        /// <param name="options"></param>
        /// <returns></returns>
        public override Org.IdentityConnectors.Framework.Common.Objects.Filters.FilterTranslator<string> CreateFilterTranslator(ObjectClass oclass, OperationOptions options)
        {
            //TODO: Implement CreateFilterTranslator
            return base.CreateFilterTranslator(oclass, options);
        }

        /// <summary>
        /// Inits the connector with configuration injected
        /// </summary>
        /// <param name="configuration"></param>
        public override void Init(Configuration configuration)
        {
            base.Init(configuration);
            _configuration = (ExchangeConfiguration)configuration;
            _runspace = new RunSpaceInstance(RunSpaceInstance.SnapIn.Exchange);
            _mapOcInfo = ExchangeUtils.GetOCInfo();
        }
        
        
        /// <summary>
        /// Dispose resources
        /// </summary>
        public override void Dispose()
        {
            //lock is probably not necessary
            lock (this)
            {
                if (_disposed)
                {
                    return;
                }
                if (_runspace != null)
                {
                    _runspace.Dispose();
                }
                base.Dispose();
                _disposed = true;
            }
            
        }

        /// <summary>
        /// Defines the supported object classes by the connector, used for schema building
        /// </summary>
        /// <returns>List of supported object classes</returns>
        protected override IList<ObjectClass> GetSupportedObjectClasses()
        {
            IList<ObjectClass> ocList =  base.GetSupportedObjectClasses();
            Assertions.NullCheck(ocList, "ocList");
            ocList.Add(MAILBOX);
            ocList.Add(MAILUSER);
            return ocList;
        }

        /// <summary>
        /// Gets the object class info for specified object class, used for schema building
        /// </summary>
        /// <param name="oc">ObjectClass to get info for</param>
        /// <returns>ObjectClass' ObjectClassInfo</returns>
        protected override ObjectClassInfo GetObjectClassInfo(ObjectClass oc)
        {
            ObjectClassInfo ret = CollectionUtil.GetValue(_mapOcInfo, oc, null) ?? base.GetObjectClassInfo(oc);
            Assertions.NullCheck(ret, "ret");
            return ret;  
        }

    }


    /// <summary>
    /// Command definition object
    /// </summary>
    internal sealed class CommandInfo
    {
        private static IDictionary<string, XCommandInfo> cinfos = null;

        internal static readonly CommandInfo ENABLE_MAILBOX = new CommandInfo("Enable-Mailbox");
        internal static readonly CommandInfo ENABLE_MAILUSER = new CommandInfo("Enable-MailUser");
        internal static readonly CommandInfo SET_MAILUSER = new CommandInfo("Set-MailUser");


        private CommandInfo(string name)
        {
            Name = name;
            if (cinfos == null)
            {
                cinfos = ExchangeUtils.GetCommandInfo();
            }

        }

        /// <summary>
        /// Comamnd Name
        /// </summary>
        internal string Name { get; private set; }

        /// <summary>
        /// Comand Parameters
        /// </summary>
        internal string[] Parameters
        {
            get
            {
                var ret = CollectionUtil.GetValue(cinfos, Name, null);
                Assertions.NullCheck(ret, "ret");

                return ret.Parameter;  
            }
        }

        /// <summary>
        /// Name parameter
        /// </summary>
        internal string NameParameter
        {
            get
            {
                var ret = CollectionUtil.GetValue(cinfos, Name, null);
                Assertions.NullCheck(ret, "ret");

                return ret.NameParameter;
            }
        }
    }
}
 