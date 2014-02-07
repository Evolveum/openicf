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
    using Org.IdentityConnectors.Framework.Spi.Operations;

    /// <summary>
    /// MS Exchange connector - build to have the same functionality as Exchange resource adapter
    /// </summary>
    [ConnectorClass("connector_displayName",
        typeof(ExchangeConfiguration),
        MessageCatalogPaths = new[] { "Org.IdentityConnectors.Exchange.Messages",
                "Org.IdentityConnectors.ActiveDirectory.Messages" })]
    public class ExchangeConnector : CreateOp, Connector, SchemaOp, DeleteOp,
        SearchOp<String>, TestOp, ScriptOnResourceOp, UpdateOp, SyncOp,
        AuthenticateOp, PoolableConnector, AttributeNormalizer
    {
        #region Fields Definition

        /// <summary>
        /// ClassName - used for debugging purposes
        /// </summary>
        private static readonly string ClassName = typeof(ExchangeConnector).ToString();

        /// <summary>
        /// A delegate to carry out AD-related tasks.
        /// </summary>
        private ActiveDirectoryConnector _activeDirectoryConnector;
        
        /// <summary>
        /// Cached schema.
        /// </summary>
        private static Schema _schema = null;

        /// <summary>
        /// Configuration instance
        /// </summary>
        private ExchangeConfiguration _configuration;

        /// <summary>
        /// Runspace instance
        /// </summary>
        private RunSpaceInstance _runspace;

        #endregion

        #region Constructors
        static ExchangeConnector()
        {
            PSExchangeConnector.CommandInfo.InitializeIfNeeded();
        }

        public ExchangeConnector()
        {
            _activeDirectoryConnector = new ActiveDirectoryConnector();
        }
        #endregion

        #region CreateOp Members

        /// <summary>
        /// Implementation of CreateOp.Create
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="attributes">Object attributes</param>
        /// <param name="options">Operation options</param>
        /// <returns>Uid of the created object</returns>
        public Uid Create(
                ObjectClass oclass, ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            ExchangeUtility.NullCheck(oclass, "oclass", this._configuration);
            ExchangeUtility.NullCheck(attributes, "attributes", this._configuration);

            Trace.TraceInformation("Exchange.Create method; attributes:\n{0}", CommonUtils.DumpConnectorAttributes(attributes));
            Stopwatch stopWatch = new Stopwatch();
            stopWatch.Start();

            // we handle accounts only
            if (!oclass.Is(ObjectClass.ACCOUNT_NAME))
            {
                return _activeDirectoryConnector.Create(oclass, attributes, options);
            }

            const string METHOD = "Create";
            Debug.WriteLine(METHOD + ":entry", ClassName);

            // get recipient type
            string rcptType = ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttRecipientType, attributes) as string;

            PSExchangeConnector.CommandInfo cmdInfoEnable = null;
            PSExchangeConnector.CommandInfo cmdInfoSet = null;
            switch (rcptType)
            {
                case ExchangeConnectorAttributes.RcptTypeMailBox:
                    cmdInfoEnable = PSExchangeConnector.CommandInfo.EnableMailbox;
                    cmdInfoSet = PSExchangeConnector.CommandInfo.SetMailbox;
                    break;
                case ExchangeConnectorAttributes.RcptTypeMailUser:
                    cmdInfoEnable = PSExchangeConnector.CommandInfo.EnableMailUser;
                    cmdInfoSet = PSExchangeConnector.CommandInfo.SetMailUser;
                    break;
                case ExchangeConnectorAttributes.RcptTypeUser:
                    break;
                default:
                    throw new ArgumentException(
                              this._configuration.ConnectorMessages.Format(
                              "ex_bad_rcpt", "Recipient type [{0}] is not supported", rcptType));
            }

            // first create the object in AD
            ICollection<ConnectorAttribute> adAttributes = FilterOut(attributes, cmdInfoEnable, cmdInfoSet);
            Uid uid = _activeDirectoryConnector.Create(oclass, adAttributes, options);

            if (rcptType == ExchangeConnectorAttributes.RcptTypeUser)
            {
                // AD account only, we do nothing
                return uid;
            }

            // prepare the command            
            Command cmdEnable = ExchangeUtility.GetCommand(cmdInfoEnable, attributes, this._configuration);
            Command cmdSet = ExchangeUtility.GetCommand(cmdInfoSet, attributes, this._configuration);

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
            Trace.TraceInformation("Exchange.Create method exiting, took {0} ms", stopWatch.ElapsedMilliseconds);
            return uid;
        }
        #endregion

        #region Connector Members
        /// <summary>
        /// Inits the connector with configuration injected
        /// </summary>
        /// <param name="configuration">Connector configuration</param>
        public void Init(Configuration configuration)
        {
            Trace.TraceInformation("ExchangeConnector.Init: entry");

            this._configuration = (ExchangeConfiguration)configuration;
            _activeDirectoryConnector.Init(configuration);
            Schema();
            this._runspace = new RunSpaceInstance(RunSpaceInstance.SnapIn.Exchange, this._configuration.ExchangeUri, configuration.ConnectorMessages);

            Trace.TraceInformation("ExchangeConnector.Init: exit");
        }
        #endregion

        #region IDisposable Members
        /// <summary>
        /// Dispose resources, <see cref="IDisposable"/>
        /// </summary>
        public void Dispose()
        {
            this.Dispose(true);
            GC.SuppressFinalize(this);
        }

        /// <summary>
        /// Dispose the resources we use
        /// </summary>
        /// <param name="disposing">true if called from <see cref="PSExchangeConnector.Dispose()"/> (?????)</param>
        protected virtual void Dispose(bool disposing)
        {
            if (disposing)
            {
                // free managed resources
                if (this._runspace != null)
                {
                    this._runspace.Dispose();
                    this._runspace = null;
                }
            }
        }
        #endregion


        #region TestOp Members

        /// <summary>
        /// Tests if the connector is properly configured and ready
        /// </summary>
        public void Test()
        {
            // validate the configuration first, this will check AD configuration too
            _configuration.Validate();

            // AD validation (includes configuration validation too)
            _activeDirectoryConnector.Test();

            // runspace check
            _runspace.Test();
        }

        #endregion

        #region UpdateOp implementation
        /// <summary>
        /// Implementation of UpdateOp.Update
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="uid">Object UID</param>
        /// <param name="attributes">Object attributes</param>
        /// <param name="options">Operation options</param>
        /// <returns>Uid of the updated object</returns>
        public Uid Update(ObjectClass oclass, Uid uid, ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            const string METHOD = "Update";
            Debug.WriteLine(METHOD + ":entry", ClassName);

            ExchangeUtility.NullCheck(oclass, "oclass", this._configuration);
            ExchangeUtility.NullCheck(uid, "uid", this._configuration);
            ExchangeUtility.NullCheck(attributes, "attributes", this._configuration);

            Trace.TraceInformation("Exchange.Update method; oclass = {0}, uid = {1}, attributes:\n{2}", oclass, uid, CommonUtils.DumpConnectorAttributes(attributes));
            Stopwatch stopWatch = new Stopwatch();
            stopWatch.Start();

            // we handle accounts only
            if (!oclass.Is(ObjectClass.ACCOUNT_NAME))
            {
                return _activeDirectoryConnector.Update(oclass, uid, attributes, options);
            }

            // get recipient type and database
            string rcptType = ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttRecipientType, attributes) as string;
            string database = ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttDatabase, attributes) as string;

            // update in AD first
            var filtered = FilterOut(
                attributes,
                PSExchangeConnector.CommandInfo.EnableMailbox,
                PSExchangeConnector.CommandInfo.EnableMailUser,
                PSExchangeConnector.CommandInfo.SetMailbox,
                PSExchangeConnector.CommandInfo.SetMailUser);
            _activeDirectoryConnector.Update(oclass, uid, filtered, options);

            // retrieve Exchange-related information about the user
            ConnectorObject aduser = this.ADSearchByUid(uid, oclass, ExchangeUtility.AddAttributeToOptions(options, ExchangeConnectorAttributes.AttDatabaseADName));

            // to get the user information, we have to provide user's name
            attributes = new HashSet<ConnectorAttribute>(attributes);           // to make collection modifiable
            attributes.Add(aduser.Name);                    

            // now create and execute PS command to get the information
            PSExchangeConnector.CommandInfo cmdInfo;
            if (aduser.GetAttributeByName(ExchangeConnectorAttributes.AttDatabaseADName) != null)
            {
                cmdInfo = PSExchangeConnector.CommandInfo.GetMailbox;       // we can be sure it is user mailbox type
            }
            else
            {
                cmdInfo = PSExchangeConnector.CommandInfo.GetUser;
            }
            PSObject psuser = this.GetUser(cmdInfo, attributes);

            // do we change recipient type?
            string origRcptType = psuser.Members[ExchangeConnectorAttributes.AttRecipientType].Value.ToString();
            if (String.IsNullOrEmpty(rcptType))
            {
                rcptType = origRcptType;
            }

            if (rcptType == ExchangeConnectorAttributes.RcptTypeMailUser)
            {
                // disabling Mailbox if needed
                if (origRcptType == ExchangeConnectorAttributes.RcptTypeMailBox)
                {
                    Command cmdDisable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.DisableMailbox, attributes, this._configuration);
                    cmdDisable.Parameters.Add("Confirm", false);
                    this.InvokePipeline(cmdDisable);
                }

                // enabling MailUser if needed
                if (origRcptType != rcptType)
                {
                    // Enable-MailUser needs the value of ExternalEmailAddress, so we have to get it
                    string externalEmailAddress = ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttExternalEmailAddress, attributes) as string;
                    if (String.IsNullOrEmpty(externalEmailAddress))
                    {
                        PSMemberInfo o = psuser.Members[ExchangeConnectorAttributes.AttExternalEmailAddress];
                        if (o == null || o.Value == null || String.IsNullOrEmpty(o.Value.ToString()))
                        {
                            throw new InvalidOperationException("Missing ExternalEmailAddress value, which is required for a MailUser");
                        }
                        externalEmailAddress = o.Value.ToString();
                        ExchangeUtility.SetAttValue(ExchangeConnectorAttributes.AttExternalEmailAddress, externalEmailAddress, attributes);
                    }

                    // now execute the Enable-MailUser command
                    Command cmdEnable = ExchangeUtility.GetCommand(
                            PSExchangeConnector.CommandInfo.EnableMailUser, attributes, this._configuration);
                    this.InvokePipeline(cmdEnable);
                }

                Command cmdSet = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.SetMailUser, attributes, this._configuration);
                this.InvokePipeline(cmdSet);
            }
            else if (rcptType == ExchangeConnectorAttributes.RcptTypeMailBox)
            {
                // we should execute something like this here:
                // get-user -identity id|?{$_.RecipientType -eq "User"}|enable-mailbox -database "db"
                // unfortunately I was not able to get it working with the pipeline... that's why there are two commands
                // executed :-(
                // alternatively there can be something like:
                // get-user -identity id -RecipientTypeDetails User|enable-mailbox -database "db", but we have then trouble
                // with detecting attempt to change the database attribute               
                string origDatabase = psuser.Members[ExchangeConnectorAttributes.AttDatabase] != null ? psuser.Members[ExchangeConnectorAttributes.AttDatabase].Value.ToString() : null;
                if (origRcptType != rcptType)
                {
                    Command cmdEnable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.EnableMailbox, attributes, this._configuration);
                    this.InvokePipeline(cmdEnable);
                }
                else
                {
                    // trying to update the database?
                    if (database != null && database != origDatabase)
                    {
                        throw new ArgumentException(
                            this._configuration.ConnectorMessages.Format(
                            "ex_not_updatable", "Update of [{0}] attribute is not supported", ExchangeConnectorAttributes.AttDatabase));
                    }
                }

                Command cmdSet = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.SetMailbox, attributes, this._configuration);
                this.InvokePipeline(cmdSet);
            }
            else if (rcptType == ExchangeConnectorAttributes.RcptTypeUser)
            {
                if (origRcptType == ExchangeConnectorAttributes.RcptTypeMailBox)
                {
                    Command cmdDisable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.DisableMailbox, attributes, this._configuration);
                    cmdDisable.Parameters.Add("Confirm", false);
                    this.InvokePipeline(cmdDisable);
                }
                else if (origRcptType == ExchangeConnectorAttributes.RcptTypeMailUser)
                {
                    Command cmdDisable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.DisableMailUser, attributes, this._configuration);
                    cmdDisable.Parameters.Add("Confirm", false);
                    this.InvokePipeline(cmdDisable);
                }
                else if (origRcptType == ExchangeConnectorAttributes.RcptTypeUser)
                {
                    // if orig is User, there is no need to disable anything
                }
                else
                {
                    throw new InvalidOperationException("Invalid original recipient type: " + origRcptType);
                }

                Command cmdSet = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.SetUser, attributes, this._configuration);
                this.InvokePipeline(cmdSet);
            }
            else
            {
                // unsupported rcpt type
                throw new ArgumentException(
                            this._configuration.ConnectorMessages.Format(
                            "ex_bad_rcpt", "Recipient type [{0}] is not supported", rcptType));
            }

            Debug.WriteLine(METHOD + ":exit", ClassName);
            Trace.TraceInformation("Exchange.Update method exiting, took {0} ms", stopWatch.ElapsedMilliseconds);
            return uid;
        }
        #endregion

        #region DeleteOp Members

        public void Delete(ObjectClass objClass, Uid uid, OperationOptions options)
        {
            _activeDirectoryConnector.Delete(objClass, uid, options);
        }

        #endregion

        #region ScriptOnResourceOp Members

        public object RunScriptOnResource(ScriptContext request, OperationOptions options)
        {
            return _activeDirectoryConnector.RunScriptOnResource(request, options);
        }

        #endregion

        public SyncToken GetLatestSyncToken(ObjectClass objectClass)
        {
            return _activeDirectoryConnector.GetLatestSyncToken(objectClass);
        }

        /// <summary>
        /// Implementation of SynOp.Sync
        /// </summary>
        /// <param name="objClass">Object class</param>
        /// <param name="token">Sync token</param>
        /// <param name="handler">Sync results handler</param>
        /// <param name="options">Operation options</param>
        public void Sync(
                ObjectClass objClass, SyncToken token, SyncResultsHandler handler, OperationOptions options)
        {
            ExchangeUtility.NullCheck(objClass, "oclass", this._configuration);         

            // we handle accounts only
            if (!objClass.Is(ObjectClass.ACCOUNT_NAME))
            {
                _activeDirectoryConnector.Sync(objClass, token, handler, options);
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

                // replace the ad attributes with exchange ones and add recipient type and database (if requested)
                ConnectorObject updated = ExchangeUtility.ConvertAdAttributesToExchange(delta.Object, attsToGet);
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
            _activeDirectoryConnector.SyncInternal(objClass, token, xchangeHandler, options, GetAdAttributesToReturn(objClass, options));
        }

        #region AuthenticateOp Members

        public Uid Authenticate(ObjectClass objectClass, string username,
            Org.IdentityConnectors.Common.Security.GuardedString password,
            OperationOptions options)
        {
            return _activeDirectoryConnector.Authenticate(objectClass, username, password, options);
        }

        #endregion

        #region PoolableConnector Members

        public void CheckAlive()
        {
            // TODO check runspace
            return;
        }

        #endregion

        /// <summary>
        /// Implementation of SearchOp.ExecuteQuery
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="query">Query to execute</param>
        /// <param name="handler">Results handler</param>
        /// <param name="options">Operation options</param>
        public void ExecuteQuery(
                ObjectClass oclass, string query, ResultsHandler handler, OperationOptions options)
        {
            ExchangeUtility.NullCheck(oclass, "oclass", this._configuration);

            Trace.TraceInformation("Exchange.ExecuteQuery starting");
            Stopwatch stopWatch = new Stopwatch();
            stopWatch.Start();

            // we handle accounts only
            if (!oclass.Is(ObjectClass.ACCOUNT_NAME))
            {
                _activeDirectoryConnector.ExecuteQuery(oclass, query, handler, options);
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
                                        Trace.TraceInformation("Object returned from AD connector: {0}", CommonUtils.DumpConnectorAttributes(cobject.GetAttributes()));
                                        ConnectorObject filtered = ExchangeUtility.ConvertAdAttributesToExchange(cobject, attsToGet);
                                        filtered = this.AddExchangeAttributes(oclass, filtered, attsToGet);
                                        Trace.TraceInformation("Object as passed from Exchange connector: {0}", CommonUtils.DumpConnectorAttributes(filtered.GetAttributes()));
                                        return handler(filtered);
                                    };

            ResultsHandler handler2use = filter;
            OperationOptions options2use = options;

            // mapping AttributesToGet from Exchange to AD "language"
            // actually, we don't need this code any more, because the only attribute that
            // is not retrieved by default is Database, and it is NOT retrieved via AD.
            // Uncomment this code if necessary in the future.
            if (options != null && options.AttributesToGet != null)
            {
                /*
                ISet<string> mappedExchangeAttributesToGet = new HashSet<string>(AttMap2AD.Keys);
                mappedExchangeAttributesToGet.IntersectWith(options.AttributesToGet);
                if (mappedExchangeAttributesToGet.Count > 0 || attsToGet.Contains(AttRecipientType))
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
                } */

                if (attsToGet.Contains(ExchangeConnectorAttributes.AttDatabase))
                {
                    attsToGet.Remove(ExchangeConnectorAttributes.AttDatabase);

                    // build new op options
                    var builder = new OperationOptionsBuilder(options);
                    string[] attributesToGet = new string[attsToGet.Count];
                    attsToGet.CopyTo(attributesToGet, 0);
                    builder.AttributesToGet = attributesToGet;
                    options2use = builder.Build();
                }
            }

            _activeDirectoryConnector.ExecuteQueryInternal(oclass, query, handler2use, options2use, GetAdAttributesToReturn(oclass, options));
            Trace.TraceInformation("Exchange.ExecuteQuery method exiting, took {0} ms", stopWatch.ElapsedMilliseconds);
        }

        /// <summary>
        /// Implementation of SearchOp.CreateFilterTranslator
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="options">Operation options</param>
        /// <returns>Filter translator</returns>
        public FilterTranslator<string> CreateFilterTranslator(ObjectClass oclass, OperationOptions options)
        {
            return new LegacyExchangeConnectorFilterTranslator();
        }


        /// <summary>
        /// Attribute normalizer
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="attribute">Attribute to be normalized</param>
        /// <returns>Normalized attribute</returns>
        public ConnectorAttribute NormalizeAttribute(ObjectClass oclass, ConnectorAttribute attribute)
        {
            // normalize the attribute using AD connector first
            // attribute = base.NormalizeAttribute(oclass, attribute);

            // normalize mail-related attributes
            //Trace.TraceInformation("NormalizeAttribute called for {0}", attribute.GetDetails());
            if (attribute.Is(ExchangeConnectorAttributes.AttExternalEmailAddress) || attribute.Is(ExchangeConnectorAttributes.AttForwardingSmtpAddress))
            {
                return NormalizeSmtpAddressAttribute(attribute);
            }
            else
            {
                return attribute;
            }

            // TODO: what with EmailAddresses? (we should not remove SMTP/smpt prefix, because it carries information on primary/secondary address type)
            // TODO: and other attributes?
        }

        private ConnectorAttribute NormalizeSmtpAddressAttribute(ConnectorAttribute attribute)
        {
            if (attribute.Value == null)
            {
                return attribute;
            }

            IList<object> normValues = new List<object>();
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
                        normValues.Add(split[1]);
                        normalized = true;
                    }
                    else
                    {
                        // put the original value
                        normValues.Add(val);
                    }
                }
            }

            if (normalized)
            {
                // build the attribute again
                return ConnectorAttributeBuilder.Build(attribute.Name, normValues);
            }
            else
            {
                return attribute;
            }
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
            IList<string> attsToRemove = new List<string> { ExchangeConnectorAttributes.AttRecipientType };
            CollectionUtil.AddAll(attsToRemove, ExchangeConnectorAttributes.AttMap2AD.Keys);
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
        /// for our needs
        /// </summary>
        /// <param name="oc">object class, currently the method works for <see cref="ObjectClass.ACCOUNT"/> only</param>
        /// <param name="cobject">connector object to get the recipient type/database for</param>
        /// <param name="attToGet">attributes to get</param>
        /// <returns>Connector Object with recipient type added</returns>
        /// <exception cref="ConnectorException">In case of some troubles in powershell (if the 
        /// user is not found we get this exception too)</exception>
        private ConnectorObject AddExchangeAttributes(ObjectClass oc, ConnectorObject cobject, IEnumerable<string> attToGet)
        {            
            ExchangeUtility.NullCheck(oc, "name", this._configuration);
            ExchangeUtility.NullCheck(oc, "cobject", this._configuration);

            // we support ACCOUNT only or there is nothing to add
            if (!oc.Is(ObjectClass.ACCOUNT_NAME) || attToGet == null)
            {
                return cobject;
            }

            // check it is not deleted object
            bool? deleted = ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttIsDeleted, cobject.GetAttributes()) as bool?;
            if (deleted != null && deleted == true)
            {
                // do nothing, it is deleted object
                return cobject;
            }

            ICollection<string> lattToGet = CollectionUtil.NewCaseInsensitiveSet();
            CollectionUtil.AddAll(lattToGet, attToGet);
            foreach (string att in attToGet)
            {
                if (cobject.GetAttributeByName(att) != null && att != ExchangeConnectorAttributes.AttDatabase)
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
            Command cmd = ExchangeUtility.GetCommand(cmdInfo, attributes, this._configuration);
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

            string rcptType = user.Members[ExchangeConnectorAttributes.AttRecipientType].Value.ToString();
            foundObjects = null;

            // get detailed information            
            if (rcptType == ExchangeConnectorAttributes.RcptTypeMailBox)
            {
                foundObjects = this.InvokePipeline(ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.GetMailbox, attributes, this._configuration));
            }
            else if (rcptType == ExchangeConnectorAttributes.RcptTypeMailUser)
            {
                foundObjects = this.InvokePipeline(ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.GetMailUser, attributes, this._configuration));
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
                Trace.TraceInformation("PowerShell Command: " + cmd);
                foreach (CommandParameter parameter in cmd.Parameters)
                {
                    Trace.TraceInformation("parameter: " + parameter.Name + " value:" + parameter.Value);
                }

                return this._runspace.InvokePipeline(cmd);
            }
            catch (Exception e)
            {
                throw new ConnectorException(this._configuration.ConnectorMessages.Format(
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
            ExchangeUtility.NullCheck(uid, "uid", this._configuration);
            ExchangeUtility.NullCheck(oclass, "oclass", this._configuration);
            if (options == null)
            {
                options = new OperationOptionsBuilder().Build();
            }

            ConnectorObject ret = null;
            Filter filter = FilterBuilder.EqualTo(uid);
            var translator = _activeDirectoryConnector.CreateFilterTranslator(oclass, options);
            IList<string> queries = translator.Translate(filter);

            if (queries.Count == 1)
            {
                ResultsHandler handler = delegate(ConnectorObject cobject)
                                         {
                                             ret = cobject;
                                             return false;
                                         };
                _activeDirectoryConnector.ExecuteQuery(oclass, queries[0], handler, options);
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
            ExchangeUtility.NullCheck(name, "User name", this._configuration);

            Command cmdUser = ExchangeUtility.GetCommand(cmdInfo, attributes, this._configuration);
            ICollection<PSObject> users = this.InvokePipeline(cmdUser);
            if (users.Count == 1)
            {
                foreach (PSObject obj in users)
                {
                    return obj;
                }
            }

            throw new ArgumentException(
                this._configuration.ConnectorMessages.Format(
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
                // Exchange attributes with known mappings to AD
                foreach (string attrNameInExchange in ExchangeConnectorAttributes.AttMap2AD.Keys)
                {
                    if (attr.Is(attrNameInExchange))
                    {
                        return new[] { ExchangeConnectorAttributes.AttMap2AD[attrNameInExchange] };
                    }
                }

                // Other Exchange attributes have no mapping to AD ones.
                // This means that some attributes with more complicated mappings,
                // like RecipientType or EmailAddressPolicyEnabled, cannot be
                // used in search queries.
                if (ExchangeConnectorAttributes.IsExchangeAttribute(attr))
                {
                    return null;
                }
                else
                {
                    return base.GetLdapNamesForAttribute(attr);
                }
            }
        }
        #region SchemaOp Implementation

        /// <summary>
        /// Implementation of SchemaSpiOp
        /// </summary>
        /// <returns></returns>
        public Schema Schema()
        {
            Trace.TraceInformation("Exchange.Schema method");
            if (_schema != null)
            {
                Trace.TraceInformation("Returning cached schema");
            }
            else
            {
                _schema = SchemaUtils.BuildSchema(this,
                                _activeDirectoryConnector.GetSupportedObjectClasses,
                                GetObjectClassInfo,
                                _activeDirectoryConnector.GetSupportedOperations,
                                _activeDirectoryConnector.GetUnSupportedOperations);
            }
            return _schema;
        }

        /// <summary>
        /// Gets the object class info for specified object class, used for schema building
        /// </summary>
        /// <param name="oc">ObjectClass to get info for</param>
        /// <returns>ObjectClass' ObjectClassInfo</returns>
        private ObjectClassInfo GetObjectClassInfo(ObjectClass oc)
        {
            // get the object class from base
            ObjectClassInfo oinfo = _activeDirectoryConnector.GetObjectClassInfo(oc);
            Trace.TraceInformation("ExchangeConnector.GetObjectClassInfo: oinfo for {0} from AD has {1} entries", oc, oinfo.ConnectorAttributeInfos.Count);

            // add additional attributes for ACCOUNT
            if (oc.Is(ObjectClass.ACCOUNT_NAME))
            {
                var classInfoBuilder = new ObjectClassInfoBuilder { IsContainer = oinfo.IsContainer, ObjectType = oinfo.ObjectType };
                classInfoBuilder.AddAllAttributeInfo(oinfo.ConnectorAttributeInfos);
                classInfoBuilder.AddAllAttributeInfo(ExchangeConnectorAttributes.ManualExchangeAttInfosForSchema);
                classInfoBuilder.AddAllAttributeInfo(ExchangeConnectorAttributes.AttInfoCustomAttributesForSchema);
                classInfoBuilder.AddAllAttributeInfo(ExchangeConnectorAttributes.ExchangeRelatedADAttInfosForSchema);
                oinfo = classInfoBuilder.Build();
                Trace.TraceInformation("ExchangeConnector.GetObjectClassInfo: newly created oinfo has {0} entries", oinfo.ConnectorAttributeInfos.Count);
            }

            // return
            return oinfo;
        }

        private ICollection<string> GetAdAttributesToReturn(ObjectClass oclass, OperationOptions options)
        {
            ICollection<string> attNames = _activeDirectoryConnector.GetAdAttributesToReturn(oclass, options);

            // In attNames there is a mix of attributes - some AD-only ones (from ObjectClasses.xml),
            // and some Exchange ones (from the schema) and some AD-only-for-Exchange ones (from the schema).
            // We should convert Exchange ones to their AD counterparts and add "hidden useful" AD attributes.

            if (oclass.Is(ObjectClass.ACCOUNT_NAME))
            {
                ICollection<string> newAttNames = new HashSet<string>(attNames);

                // IMPORTANT: we assume that "options" do not imply any additional AD attributes
                CollectionUtil.AddAll(newAttNames, ExchangeConnectorAttributes.AttMapFromAD.Keys);
                CollectionUtil.AddAll(newAttNames, ExchangeConnectorAttributes.HiddenAdAttributesToRetrieve);
                CollectionUtil.AddAll(newAttNames, ExchangeConnectorAttributes.VisibleAdAttributesToRetrieve);
                attNames = newAttNames;
            }
            return attNames;
        }

        #endregion

    }
}
