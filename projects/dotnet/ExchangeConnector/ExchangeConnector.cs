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
    using System.Collections;
    using System.Collections.Generic;
    using System.Collections.ObjectModel;
    using System.Diagnostics;
    using System.Management.Automation;
    using System.Management.Automation.Runspaces;

    using Org.IdentityConnectors.ActiveDirectory;
    using Org.IdentityConnectors.Common;
    using Org.IdentityConnectors.Framework.Common.Exceptions;
    using Org.IdentityConnectors.Framework.Common.Objects;
    using Org.IdentityConnectors.Framework.Common.Objects.Filters;
    using Org.IdentityConnectors.Framework.Spi;

    /// <summary>
    /// MS Exchange connector - build to have the same functionality as Exchange resource adapter
    /// </summary>
    [ConnectorClass("connector_displayName",
        typeof(ExchangeConfiguration),
        MessageCatalogPaths = new[] { "Org.IdentityConnectors.Exchange.Messages",
                "Org.IdentityConnectors.ActiveDirectory.Messages" })]
    public class ExchangeConnector : ActiveDirectoryConnector
    {
        #region Fields Definition

        /// <summary>
        /// Recipient Type attribute name
        /// </summary>
        internal const string AttRecipientType = "RecipientType";

        /// <summary>
        /// External Mail Address attribute name
        /// </summary>
        internal const string AttExternalMail = "ExternalEmailAddress";

        /// <summary>
        /// Database attribute name
        /// </summary>
        internal const string AttDatabase = "Database";

        /// <summary>
        /// Deleted atrribute name
        /// </summary>
        internal const string AttIsDeleted = "isDeleted";

        /// <summary>
        /// External Mail attribute name as in AD
        /// </summary>
        internal const string AttExternalMailADName = "targetAddress";

        /// <summary>
        /// Database attribute name as in AD
        /// </summary>
        internal const string AttDatabaseADName = "homeMDB";
        
        /// <summary>
        /// Attribute mapping constant
        /// </summary>
        internal static readonly IDictionary<string, string> AttMap2AD = new Dictionary<string, string> 
        {
        { AttDatabase, AttDatabaseADName },
        { AttExternalMail, AttExternalMailADName }
        };

        /// <summary>
        /// Attribute mapping constant
        /// </summary>
        internal static readonly IDictionary<string, string> AttMapFromAD = new Dictionary<string, string> 
        {
        { AttDatabaseADName, AttDatabase },
        { AttExternalMailADName, AttExternalMail }
        };

        /// <summary>
        /// ClassName - used for debugging purposes
        /// </summary>
        private static readonly string ClassName = typeof(ExchangeConnector).ToString();

        /// <summary>
        /// Recipient type attribute info
        /// </summary>
        private static readonly ConnectorAttributeInfo AttInfoRecipientType =
                ConnectorAttributeInfoBuilder.Build(
                        AttRecipientType,
                        typeof(string),
                        ConnectorAttributeInfo.Flags.REQUIRED | ConnectorAttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT);

        /// <summary>
        /// External Mail attribute info
        /// </summary>
        private static readonly ConnectorAttributeInfo AttInfoExternalMail =
                ConnectorAttributeInfoBuilder.Build(
                        AttExternalMail,
                        typeof(string),
                        ConnectorAttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT | ConnectorAttributeInfo.Flags.MULTIVALUED);

        /// <summary>
        /// Database attribute info
        /// </summary>
        private static readonly ConnectorAttributeInfo AttInfoDatabase =
                ConnectorAttributeInfoBuilder.Build(
                        AttDatabase,
                        typeof(string),
                        ConnectorAttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT);

        /// <summary>
        /// Recipient type attribute for Mailbox
        /// </summary>
        private const string RcptTypeMailBox = "UserMailbox";

        /// <summary>
        /// Recipient type attribute for MailUser
        /// </summary>
        private const string RcptTypeMailUser = "MailUser";

        /// <summary>
        /// Recipient type attribute for AD only User
        /// </summary>
        private const string RcptTypeUser = "User";

        /// <summary>
        /// Configuration instance
        /// </summary>
        private ExchangeConfiguration configuration;

        /// <summary>
        /// Runspace instance
        /// </summary>
        private RunSpaceInstance runspace;

        #endregion

        #region CreateOp.Create implementation

        /// <summary>
        /// Implementation of CreateOp.Create
        /// </summary>
        /// <param name="oclass">Object class</param>(oc
        /// <param name="attributes">Object attributes</param>
        /// <param name="options">Operation options</param>
        /// <returns>Uid of the created object</returns>
        public override Uid Create(
                ObjectClass oclass, ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            ExchangeUtility.NullCheck(oclass, "oclass", this.configuration);
            ExchangeUtility.NullCheck(attributes, "attributes", this.configuration);

            // we handle accounts only
            if (!oclass.Is(ObjectClass.ACCOUNT_NAME))
            {
                return base.Create(oclass, attributes, options);
            }

            const string METHOD = "Create";
            Debug.WriteLine(METHOD + ":entry", ClassName);

            // get recipient type
            string rcptType = ExchangeUtility.GetAttValue(AttRecipientType, attributes) as string;

            PSExchangeConnector.CommandInfo cmdInfoEnable = null;
            PSExchangeConnector.CommandInfo cmdInfoSet = null;
            switch (rcptType)
            {
                case RcptTypeMailBox:
                    cmdInfoEnable = PSExchangeConnector.CommandInfo.EnableMailbox;
                    cmdInfoSet = PSExchangeConnector.CommandInfo.SetMailbox;
                    break;
                case RcptTypeMailUser:
                    cmdInfoEnable = PSExchangeConnector.CommandInfo.EnableMailUser;
                    cmdInfoSet = PSExchangeConnector.CommandInfo.SetMailUser;
                    break;
                case RcptTypeUser:
                    break;
                default:
                    throw new ArgumentException(
                              this.configuration.ConnectorMessages.Format(
                              "ex_bad_rcpt", "Recipient type [{0}] is not supported", rcptType));
            }

            // first create the object in AD
            Uid uid = base.Create(oclass, FilterOut(attributes, cmdInfoEnable, cmdInfoSet), options);

            if (rcptType == RcptTypeUser)
            {
                // AD account only, we do nothing
                return uid;
            }

            // prepare the command            
            Command cmdEnable = ExchangeUtility.GetCommand(cmdInfoEnable, attributes);
            Command cmdSet = ExchangeUtility.GetCommand(cmdInfoSet, attributes);

            try
            {
                this.InvokePipeline(cmdEnable);
                this.InvokePipeline(cmdSet);
            }
            catch
            {
                Trace.TraceWarning("Rolling back AD create for UID: " + uid.GetUidValue());

                // rollback AD create
                try
                {
                    Delete(oclass, uid, options);
                }
                catch
                {
                    Trace.TraceWarning("Not able to rollback AD create for UID: " + uid.GetUidValue());

                    // note: this is not perfect, we hide the original exception
                    throw;
                }

                // rethrow original exception
                throw;
            }

            Debug.WriteLine(METHOD + ":exit", ClassName);
            return uid;
        }

        #endregion

        /// <summary>
        /// Implementation of UpdateOp.Update
        /// </summary>
        /// <param name="type">Update type</param>
        /// <param name="oclass">Object class</param>
        /// <param name="attributes">Object attributes</param>
        /// <param name="options">Operation options</param>
        /// <returns>Uid of the updated object</returns>
        public override Uid Update(
                UpdateType type,
                ObjectClass oclass,
                ICollection<ConnectorAttribute> attributes,
                OperationOptions options)
        {
            const string METHOD = "Update";
            Debug.WriteLine(METHOD + ":entry", ClassName);

            ExchangeUtility.NullCheck(type, "updatetype", this.configuration);
            ExchangeUtility.NullCheck(oclass, "oclass", this.configuration);
            ExchangeUtility.NullCheck(attributes, "attributes", this.configuration);

            // we handle accounts only
            if (!oclass.Is(ObjectClass.ACCOUNT_NAME))
            {
                return base.Update(type, oclass, attributes, options);
            }

            // get recipient type and database
            string rcptType = ExchangeUtility.GetAttValue(AttRecipientType, attributes) as string;
            string database = ExchangeUtility.GetAttValue(AttDatabase, attributes) as string;

            // update in AD first
            var filtered = FilterOut(
                attributes,
                PSExchangeConnector.CommandInfo.EnableMailbox,
                PSExchangeConnector.CommandInfo.EnableMailUser,
                PSExchangeConnector.CommandInfo.SetMailbox,
                PSExchangeConnector.CommandInfo.SetMailUser);
            Uid uid = base.Update(type, oclass, filtered, options);

            ConnectorObject aduser = this.ADSearchByUid(uid, oclass, ExchangeUtility.AddAttributeToOptions(options, AttDatabaseADName));
            attributes.Add(aduser.Name);
            PSExchangeConnector.CommandInfo cmdInfo = PSExchangeConnector.CommandInfo.GetUser;
            if (aduser.GetAttributeByName(AttDatabaseADName) != null)
            {
                // we can be sure it is user mailbox type
                cmdInfo = PSExchangeConnector.CommandInfo.GetMailbox;
            }

            PSObject psuser = this.GetUser(cmdInfo, attributes);
            string origRcptType = psuser.Members[AttRecipientType].Value.ToString();
            if (String.IsNullOrEmpty(rcptType))
            {
                rcptType = origRcptType;
            }

            if (rcptType == RcptTypeMailUser)
            {
                if (type == UpdateType.REPLACE)
                {                 
                    if (origRcptType != rcptType)
                    {
                        Command cmdEnable = ExchangeUtility.GetCommand(
                                PSExchangeConnector.CommandInfo.EnableMailUser, attributes);
                        this.InvokePipeline(cmdEnable);
                    }

                    Command cmdSet = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.SetMailUser, attributes);
                    this.InvokePipeline(cmdSet);
                }
                else
                {
                    throw new ConnectorException(this.configuration.ConnectorMessages.Format(
                            "ex_wrong_update_type", "Update type [{0}] not supported", type));
                }
            }
            else if (rcptType == RcptTypeMailBox)
            {
                // we should execute something like this here:
                // get-user -identity id|?{$_.RecipientType -eq "User"}|enable-mailbox -database "db"
                // unfortunately I was not able to get it working with the pipeline... that's why there are two commands
                // executed :-(
                // alternatively there can be something like:
                // get-user -identity id -RecipientTypeDetails User|enable-mailbox -database "db", but we have then trouble
                // with detecting attempt to change the database attribute               
                string origDatabase = psuser.Members[AttDatabase] != null ? psuser.Members[AttDatabase].Value.ToString() : null;
                if (origRcptType != rcptType)
                {
                    Command cmdEnable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.EnableMailbox, attributes);
                    this.InvokePipeline(cmdEnable);
                }
                else
                {
                    // trying to update the database?
                    if (database != null && database != origDatabase)
                    {
                        throw new ArgumentException(
                            this.configuration.ConnectorMessages.Format(
                            "ex_not_updatable", "Update of [{0}] attribute is not supported", AttDatabase));
                    }
                }

                Command cmdSet = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.SetMailbox, attributes);
                this.InvokePipeline(cmdSet);
            }
            else if (rcptType == RcptTypeUser && origRcptType != rcptType)
            {
                throw new ArgumentException(
                        this.configuration.ConnectorMessages.Format(
                        "ex_update_notsupported", "Update of [{0}] to [{1}] is not supported", AttRecipientType, rcptType));
            }
            else if (rcptType != RcptTypeUser)
            {
                // unsupported rcpt type
                throw new ArgumentException(
                            this.configuration.ConnectorMessages.Format(
                            "ex_bad_rcpt", "Recipient type [{0}] is not supported", rcptType));
            }

            Debug.WriteLine(METHOD + ":exit", ClassName);
            return uid;
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
        /// <param name="token">Sync token</param>
        /// <param name="handler">Sync results handler</param>
        /// <param name="options">Operation options</param>
        public override void Sync(
                ObjectClass objClass, SyncToken token, SyncResultsHandler handler, OperationOptions options)
        {
            ExchangeUtility.NullCheck(objClass, "oclass", this.configuration);         

            // we handle accounts only
            if (!objClass.Is(ObjectClass.ACCOUNT_NAME))
            {
                base.Sync(objClass, token, handler, options);
                return;
            }
            
            ICollection<string> attsToGet = null;
            if (options != null && options.AttributesToGet != null)
            {
                attsToGet = CollectionUtil.NewSet(options.AttributesToGet);
            }

            // delegate to get the exchange attributes if requested            
            SyncResultsHandler xchangeHandler = delegate(SyncDelta delta)
            {
                if (delta.DeltaType == SyncDeltaType.DELETE)
                {
                    return handler(delta);
                }

                // replace the ad attributes with exchange one and add recipient type
                ConnectorObject updated = ExchangeUtility.ReplaceAttributes(delta.Object, attsToGet, AttMapFromAD);
                updated = this.AddExchangeAttributes(objClass, updated, attsToGet);
                if (updated != delta.Object)
                {
                    // build new sync delta, cause we changed the object
                    SyncDeltaBuilder deltaBuilder = new SyncDeltaBuilder
                                                        {
                                                                DeltaType = delta.DeltaType,
                                                                Token = delta.Token,
                                                                Uid = delta.Uid,
                                                                Object = updated
                                                        };
                    delta = deltaBuilder.Build();
                }

                return handler(delta);
            };

            // call AD sync, use xchangeHandler
            base.Sync(objClass, token, xchangeHandler, options);
        }

        /// <summary>
        /// Implementation of SearchOp.ExecuteQuery
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="query">Query to execute</param>
        /// <param name="handler">Results handler</param>
        /// <param name="options">Operation options</param>
        public override void ExecuteQuery(
                ObjectClass oclass, string query, ResultsHandler handler, OperationOptions options)
        {
            ExchangeUtility.NullCheck(oclass, "oclass", this.configuration);         

            // we handle accounts only
            if (!oclass.Is(ObjectClass.ACCOUNT_NAME))
            {
                base.ExecuteQuery(oclass, query, handler, options);
                return;
            }

            ICollection<string> attsToGet = null;
            if (options != null && options.AttributesToGet != null)
            {
                attsToGet = CollectionUtil.NewList(options.AttributesToGet);
            }

            // delegate to get the exchange attributes if requested            
            ResultsHandler filter = delegate(ConnectorObject cobject)
                                    {
                                        ConnectorObject filtered = ExchangeUtility.ReplaceAttributes(
                                                cobject, attsToGet, AttMapFromAD);
                                        filtered = this.AddExchangeAttributes(oclass, filtered, attsToGet);
                                        return handler(filtered);
                                    };

            ResultsHandler handler2use = handler;
            OperationOptions options2use = options;
            if (options != null && options.AttributesToGet != null)
            {
                if (attsToGet.Contains(AttDatabase) || attsToGet.Contains(AttExternalMail)
                    || attsToGet.Contains(AttRecipientType))
                {
                    // replace Exchange attributes with AD names
                    var newAttsToGet = ExchangeUtility.FilterReplace(attsToGet, AttMap2AD);

                    // we have to remove recipient type, as it is unknown to AD
                    newAttsToGet.Remove(AttRecipientType);

                    // build new op options
                    var builder = new OperationOptionsBuilder(options);
                    string[] attributesToGet = new string[newAttsToGet.Count];
                    newAttsToGet.CopyTo(attributesToGet, 0);
                    builder.AttributesToGet = attributesToGet;
                    options2use = builder.Build();
                    handler2use = filter;
                }
            }

            base.ExecuteQuery(oclass, query, handler2use, options2use);
        }

        /// <summary>
        /// Implementation of SearchOp.CreateFilterTranslator
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="options">Operation options</param>
        /// <returns>Filter translator</returns>
        public override FilterTranslator<string> CreateFilterTranslator(ObjectClass oclass, OperationOptions options)
        {
            return new LegacyExchangeConnectorFilterTranslator();
        }

        /// <summary>
        /// Inits the connector with configuration injected
        /// </summary>
        /// <param name="configuration">Connector configuration</param>
        public override void Init(Configuration configuration)
        {
            this.configuration = (ExchangeConfiguration)configuration;
            base.Init(configuration);            
            this.runspace = new RunSpaceInstance(RunSpaceInstance.SnapIn.Exchange);
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
        /// Attribute normalizer
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="attribute">Attribute to be normalized</param>
        /// <returns>Normalized attribute</returns>
        public override ConnectorAttribute NormalizeAttribute(ObjectClass oclass, ConnectorAttribute attribute)
        {
            // normalize the attribute using AD connector first
            attribute = base.NormalizeAttribute(oclass, attribute);

            // normalize external mail value
            if (attribute.Name == AttExternalMail && attribute.Value != null)
            {
                IList<object> normAttributes = new List<object>();
                bool normalized = false;
                foreach (object val in attribute.Value)
                {
                    string strVal = val as string;
                    if (strVal != null)
                    {
                        string[] split = strVal.Split(':');
                        if (split.Length == 2)
                        {
                            // it contains delimiter, use the second part
                            normAttributes.Add(split[1]);
                            normalized = true;
                        }
                        else
                        {
                            // put the original value
                            normAttributes.Add(val);
                        }
                    }
                }

                if (normalized)
                {
                    // build the attribute again
                    return ConnectorAttributeBuilder.Build(attribute.Name, normAttributes);
                }
            }

            // return the original attribute
            return attribute;
        }

        /// <summary>
        /// Dispose the resources we use
        /// </summary>
        /// <param name="disposing">true if called from <see cref="PSExchangeConnector.Dispose()"/></param>
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
        /// Gets the object class info for specified object class, used for schema building
        /// </summary>
        /// <param name="oc">ObjectClass to get info for</param>
        /// <returns>ObjectClass' ObjectClassInfo</returns>
        protected override ObjectClassInfo GetObjectClassInfo(ObjectClass oc)
        {
            // get the object class from base
            ObjectClassInfo oinfo = base.GetObjectClassInfo(oc);

            // add additional attributes for ACCOUNT
            if (oc.Is(ObjectClass.ACCOUNT_NAME))
            {
                var classInfoBuilder = new ObjectClassInfoBuilder { IsContainer = oinfo.IsContainer, ObjectType = oinfo.ObjectType };
                classInfoBuilder.AddAllAttributeInfo(oinfo.ConnectorAttributeInfos);
                classInfoBuilder.AddAttributeInfo(AttInfoDatabase);
                classInfoBuilder.AddAttributeInfo(AttInfoRecipientType);
                classInfoBuilder.AddAttributeInfo(AttInfoExternalMail);
                oinfo = classInfoBuilder.Build();
            }

            // return
            return oinfo;
        }

        /// <summary>
        /// helper method to filter out all attributes used in ExchangeConnector only
        /// </summary>
        /// <param name="attributes">Connector attributes</param>
        /// <param name="cmdInfos">CommandInfo whose parameters will be used and filtered out from attributes</param>
        /// <returns>
        /// Filtered connector attributes
        /// </returns>
        private static ICollection<ConnectorAttribute> FilterOut(ICollection<ConnectorAttribute> attributes, params PSExchangeConnector.CommandInfo[] cmdInfos)
        {
            IList<string> attsToRemove = new List<string> { AttRecipientType, AttDatabase, AttExternalMail };
            if (cmdInfos != null)
            {
                foreach (PSExchangeConnector.CommandInfo cmdInfo in cmdInfos)
                {
                    if (cmdInfo != null)
                    {
                        CollectionUtil.AddAll(attsToRemove, cmdInfo.Parameters);
                    }
                }
            }

            return ExchangeUtility.FilterOut(attributes, attsToRemove);
        }

        /// <summary>
        /// This method tries to get name and value from <see cref="PSMemberInfo"/> and
        /// creates <see cref="ConnectorAttribute"/> out of it
        /// </summary>
        /// <param name="info">PSMemberInfo to get the data from</param>
        /// <returns>Created ConnectorAttribute or null if not possible to create it</returns>
        private static ConnectorAttribute GetAsAttribute(PSMemberInfo info)
        {
            Assertions.NullCheck(info, "param");
            if (info.Value != null)
            {
                string value = info.Value.ToString();

                // TODO: add type recognition, currently only string is supported
                if (value != info.Value.GetType().ToString() && !string.IsNullOrEmpty(value))
                {
                    return ConnectorAttributeBuilder.Build(info.Name, value);
                }
            }

            return null;
        }

        /// <summary>
        /// Returns first element of the collection
        /// </summary>
        /// <typeparam name="T">Object Type stored in collection</typeparam>
        /// <param name="collection">Collection to get the first element from</param>
        /// <returns>First element in the collection, null if the collection is empty</returns>
        private static T GetFirstElement<T>(IEnumerable<T> collection) where T : class
        {
            foreach (T o in collection)
            {
                return o;
            }

            return null;
        }

        /// <summary>
        /// Gets Recipient Type/Database from Exchange database, this method can be more general, but it is ok
        /// for out needs
        /// </summary>
        /// <param name="oc">object class, currently the moethod works for <see cref="ObjectClass.ACCOUNT"/> only</param>
        /// <param name="cobject">connector object to get the recipient type/database for</param>
        /// <param name="attToGet">attributes to get</param>
        /// <returns>Connector Object with recipient type added</returns>
        /// <exception cref="ConnectorException">In case of some troubles in powershell (if the 
        /// user is not found we get this exception too)</exception>
        private ConnectorObject AddExchangeAttributes(ObjectClass oc, ConnectorObject cobject, IEnumerable<string> attToGet)
        {            
            ExchangeUtility.NullCheck(oc, "name", this.configuration);
            ExchangeUtility.NullCheck(oc, "cobject", this.configuration);

            // we support ACCOUNT only or there is nothing to add
            if (!oc.Is(ObjectClass.ACCOUNT_NAME) || attToGet == null)
            {
                return cobject;
            }

            // check it is not deleted object
            bool? deleted = ExchangeUtility.GetAttValue(AttIsDeleted, cobject.GetAttributes()) as bool?;
            if (deleted != null && deleted == true)
            {
                // do nothing, it is deleted object
                return cobject;
            }

            ICollection<string> lattToGet = CollectionUtil.NewCaseInsensitiveSet();
            CollectionUtil.AddAll(lattToGet, attToGet);
            foreach (string att in attToGet)
            {
                if (cobject.GetAttributeByName(att) != null && att != AttDatabase)
                {
                    lattToGet.Remove(att);
                }
            }

            if (lattToGet.Count == 0)
            {
                return cobject;
            }

            ConnectorObjectBuilder cobjBuilder = new ConnectorObjectBuilder();
            cobjBuilder.AddAttributes(cobject.GetAttributes());

            PSExchangeConnector.CommandInfo cmdInfo = PSExchangeConnector.CommandInfo.GetUser;
            
            // prepare the connector attribute list to get the command
            ICollection<ConnectorAttribute> attributes = new Collection<ConnectorAttribute> { cobject.Name };

            // get the command
            Command cmd = ExchangeUtility.GetCommand(cmdInfo, attributes);
            ICollection<PSObject> foundObjects = this.InvokePipeline(cmd);
            PSObject user = null;
            if (foundObjects != null && foundObjects.Count == 1)
            {
                user = GetFirstElement(foundObjects);
                foreach (var info in user.Properties)
                {
                    ConnectorAttribute att = GetAsAttribute(info);                    
                    if (att != null && lattToGet.Contains(att.Name))
                    {
                        cobjBuilder.AddAttribute(att);
                        lattToGet.Remove(att.Name);
                    }                    
                }

                if (lattToGet.Count == 0)
                {
                    return cobjBuilder.Build();
                }
            } 

            if (user == null)
            {
                // nothing to do
                return cobject;
            }

            string rcptType = user.Members[AttRecipientType].Value.ToString();
            foundObjects = null;

            // get detailed information            
            if (rcptType == RcptTypeMailBox)
            {
                foundObjects = this.InvokePipeline(ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.GetMailbox, attributes));
            }
            else if (rcptType == RcptTypeMailUser)
            {
                foundObjects = this.InvokePipeline(ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.GetMailUser, attributes));
            }

            if (foundObjects != null && foundObjects.Count == 1)
            {
                PSObject userDetails = GetFirstElement(foundObjects);
                foreach (var info in userDetails.Properties)
                {
                    ConnectorAttribute att = GetAsAttribute(info);
                    if (att != null && lattToGet.Contains(att.Name))
                    {
                        cobjBuilder.AddAttribute(att);
                        lattToGet.Remove(att.Name);
                    }
                }
            }            

            return cobjBuilder.Build();
        }     

        /// <summary>
        /// Invokes command in PowerShell runspace, this method is just helper
        /// method to do the exception localization
        /// </summary>
        /// <param name="cmd">Command to execute</param>
        /// <returns>Collection of <see cref="PSObject"/> returned from runspace</returns>
        /// <exception cref="ConnectorException">If some troubles with command execution, 
        /// the exception will be partially localized</exception>
        private ICollection<PSObject> InvokePipeline(Command cmd)
        {
            try
            {
                return this.runspace.InvokePipeline(cmd);
            }
            catch (Exception e)
            {
                throw new ConnectorException(this.configuration.ConnectorMessages.Format(
                            "ex_powershell_problem", "Problem while PowerShell execution {0}", e));
            }            
        }

        /// <summary>
        /// helper method for searching object in AD by UID
        /// </summary>
        /// <param name="uid">Uid of the searched </param>
        /// <param name="oclass">Object class</param>
        /// <param name="options">Operation options</param>
        /// <returns>Connector object found by the Uid</returns>
        private ConnectorObject ADSearchByUid(Uid uid, ObjectClass oclass, OperationOptions options)
        {
            ExchangeUtility.NullCheck(uid, "uid", this.configuration);
            ExchangeUtility.NullCheck(oclass, "oclass", this.configuration);
            if (options == null)
            {
                options = new OperationOptionsBuilder().Build();
            }

            ConnectorObject ret = null;
            Filter filter = FilterBuilder.EqualTo(uid);
            var translator = base.CreateFilterTranslator(oclass, options);
            IList<string> queries = translator.Translate(filter);

            if (queries.Count == 1)
            {
                ResultsHandler handler = delegate(ConnectorObject cobject)
                                         {
                                             ret = cobject;
                                             return false;
                                         };
                base.ExecuteQuery(oclass, queries[0], handler, options);
            }

            return ret;
        }

        /// <summary>
        /// Gets the Exchange user using powershell Get-User command
        /// </summary>
        /// <param name="cmdInfo">command info to get the user</param>
        /// <param name="attributes">attributes containing the Name</param>
        /// <returns><see cref="PSObject"/> with user info</returns>
        private PSObject GetUser(PSExchangeConnector.CommandInfo cmdInfo, ICollection<ConnectorAttribute> attributes)
        {
            // assert we have user name
            string name = ExchangeUtility.GetAttValue(Name.NAME, attributes) as string;
            ExchangeUtility.NullCheck(name, "User name", this.configuration);

            Command cmdUser = ExchangeUtility.GetCommand(cmdInfo, attributes);
            ICollection<PSObject> users = this.InvokePipeline(cmdUser);
            if (users.Count == 1)
            {
                foreach (PSObject obj in users)
                {
                    return obj;
                }
            }

            throw new ArgumentException(
                this.configuration.ConnectorMessages.Format(
                "ex_bad_username", "Provided User name is not unique or not existing"));
        }        

        /// <summary>
        /// Filter translator which does MS Exchange specific translation
        /// </summary>
        private class LegacyExchangeConnectorFilterTranslator : ActiveDirectoryFilterTranslator
        {
            /// <summary>
            /// Translates the connector attribute name to LDAP name            
            /// </summary>
            /// <param name="attr">Connector attribute name</param>
            /// <returns>Translated string array</returns>
            protected override string[] GetLdapNamesForAttribute(ConnectorAttribute attr)
            {
                if (attr.Is(AttDatabase))
                {
                    return new[] { AttDatabaseADName };
                }

                if (attr.Is(AttExternalMail))
                {
                    return new[] { AttExternalMailADName };
                }

                if (attr.Is(AttRecipientType))
                {
                    return null;
                }

                return base.GetLdapNamesForAttribute(attr);
            }
        }
    }
}
