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
// Portions Copyrighted 2014 ForgeRock AS.
// Portions Copyrighted 2014 Evolveum
// </copyright>
// <author>Tomas Knappek</author>
// <author>Pavol Mederly</author>

namespace Org.IdentityConnectors.Exchange
{
    using System;
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
    using System.Reflection;

    /// <summary>
    /// MS Exchange connector - build to have the same functionality as Exchange resource adapter
    /// </summary>
    [ConnectorClass("connector_displayName",
        typeof(ExchangeConfiguration),
        MessageCatalogPaths = new[] { "Org.IdentityConnectors.Exchange.Messages",
                "Org.IdentityConnectors.ActiveDirectory.Messages" })]
    public class ExchangeConnector : CreateOp, Connector, SchemaOp, DeleteOp,
        SearchOp<string>, TestOp, ScriptOnResourceOp, UpdateOp, SyncOp,
        AuthenticateOp, PoolableConnector, AttributeNormalizer
    {
        #region Fields Definition

        /// <summary>
        /// ClassName - used for debugging purposes
        /// </summary>
        private static readonly string ClassName = typeof(ExchangeConnector).ToString();

        private static TraceSource LOGGER = new TraceSource(TraceNames.DEFAULT);
        private static TraceSource LOGGER_API = new TraceSource(TraceNames.API);
        private const int CAT_DEFAULT = 1;      // default tracing event category

        private IDictionary<string, ObjectClassHandler> _handlers;

        /// <summary>
        /// A delegate to carry out AD-related tasks.
        /// </summary>
        private ActiveDirectoryConnector _activeDirectoryConnector;
        
        /// <summary>
        /// Cached schema.
        /// </summary>
        private static Schema _schema = null;

        /// <summary>
        /// Cached object class infos.
        /// </summary>
        private IDictionary<ObjectClass, ObjectClassInfo> _objectClassInfos = null;

        /// <summary>
        /// Configuration instance
        /// </summary>
        private ExchangeConfiguration _configuration;

        // TODO do cleanly
        public ExchangeConfiguration Configuration {
            get {
                return _configuration;
            }
        }

        public ActiveDirectoryConnector ActiveDirectoryConnector {
            get {
                return _activeDirectoryConnector;
            }
        }

        internal ExchangePowerShellSupport PowerShellSupport {
            get {
                return _powershell;
            }
        }
        private ExchangePowerShellSupport _powershell;

        private Scripting _scripting;

        #endregion

        #region Constructors
        static ExchangeConnector() {
            PSExchangeConnector.CommandInfo.InitializeIfNeeded();
        }

        public ExchangeConnector() {
            _activeDirectoryConnector = new ActiveDirectoryConnector();
            _handlers = new Dictionary<string, ObjectClassHandler>() {
                { ObjectClass.ACCOUNT_NAME, new AccountHandler() },
                { ActiveDirectoryConnector.OBJECTCLASS_OU, new DelegateToActiveDirectoryHandler() },
                { ActiveDirectoryConnector.OBJECTCLASS_GROUP, new DelegateToActiveDirectoryHandler() },
                { AcceptedDomainHandler.OBJECTCLASS_NAME, new AcceptedDomainHandler() },
                { GlobalAddressListHandler.OBJECTCLASS_NAME, new GlobalAddressListHandler() },
                { AddressListHandler.OBJECTCLASS_NAME, new AddressListHandler() },
                { OfflineAddressBookHandler.OBJECTCLASS_NAME, new OfflineAddressBookHandler() },
                { AddressBookPolicyHandler.OBJECTCLASS_NAME, new AddressBookPolicyHandler() },
                { DistributionGroupHandler.OBJECTCLASS_NAME, new DistributionGroupHandler() },
                { EmailAddressPolicyHandler.OBJECTCLASS_NAME, new EmailAddressPolicyHandler() }
            };
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
        public Uid Create(ObjectClass oclass, ICollection<ConnectorAttribute> attributes, OperationOptions options) {
            const string operation = "Create";

            ExchangeUtility.NullCheck(oclass, "oclass", this._configuration);
            ExchangeUtility.NullCheck(attributes, "attributes", this._configuration);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, 
                "Exchange.Create method for {0}; attributes:\n{1}", oclass.GetObjectClassValue(), CommonUtils.DumpConnectorAttributes(attributes));

            CreateOpContext context = new CreateOpContext() {
                Attributes = attributes,
                Connector = this,
                ConnectorConfiguration = this._configuration,
                ObjectClass = oclass,
                OperationName = operation,
                Options = options
            };

            _scripting.ExecutePowerShell(context, Scripting.Position.BeforeMain);

            if (!_scripting.ExecutePowerShell(context, Scripting.Position.InsteadOfMain)) {
                CreateMain(context);
            }

            _scripting.ExecutePowerShell(context, Scripting.Position.AfterMain);

            return context.Uid;
        }

        public void CreateMain(CreateOpContext context) {
            Stopwatch stopWatch = new Stopwatch();
            stopWatch.Start();

            GetHandler(context).Create(context);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Exchange.Create method exiting, took {0} ms", stopWatch.ElapsedMilliseconds);
        }

        private ObjectClassHandler GetHandler(Context context) {
            return GetHandler(context.ObjectClass.GetObjectClassValue());
        }

        private ObjectClassHandler GetHandler(string objectClassName) {
            ObjectClassHandler handler;
            if (_handlers.TryGetValue(objectClassName, out handler)) {
                return handler;
            } else {
                throw new ConnectorException("Unsupported object class " + objectClassName + " (there is no handler for it)");
            }
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
        public Uid Update(ObjectClass oclass, Uid uid, ICollection<ConnectorAttribute> attributes, OperationOptions options) {
            const string operation = "Update";

            ExchangeUtility.NullCheck(oclass, "oclass", this._configuration);
            ExchangeUtility.NullCheck(uid, "uid", this._configuration);
            ExchangeUtility.NullCheck(attributes, "attributes", this._configuration);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, 
                "Exchange.Update method; oclass = {0}, uid = {1}, attributes:\n{2}", oclass, uid, CommonUtils.DumpConnectorAttributes(attributes));

            UpdateOpContext context = new UpdateOpContext() {
                Attributes = attributes,
                Connector = this,
                ConnectorConfiguration = this._configuration,
                ObjectClass = oclass,
                OperationName = operation,
                Options = options,
                Uid = uid
            };

            _scripting.ExecutePowerShell(context, Scripting.Position.BeforeMain);

            if (!_scripting.ExecutePowerShell(context, Scripting.Position.InsteadOfMain)) {
                UpdateMain(context);
            }

            _scripting.ExecutePowerShell(context, Scripting.Position.AfterMain);

            return context.Uid;
        }

        public void UpdateMain(UpdateOpContext context) {
            Stopwatch stopWatch = new Stopwatch();
            stopWatch.Start();

            GetHandler(context).Update(context);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, 
                "Exchange.Update method exiting, took {0} ms", stopWatch.ElapsedMilliseconds);
        }
        #endregion

        #region DeleteOp Members

        public void Delete(ObjectClass objClass, Uid uid, OperationOptions options) {
            const string operation = "Delete";

            ExchangeUtility.NullCheck(objClass, "objClass", this._configuration);
            ExchangeUtility.NullCheck(uid, "uid", this._configuration);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, 
                "Exchange.Delete method; uid:\n{0}", uid.GetUidValue());

            DeleteOpContext context = new DeleteOpContext() {
                Connector = this,
                ConnectorConfiguration = _configuration,
                ObjectClass = objClass,
                OperationName = operation,
                Uid = uid,
                Options = options
            };

            _scripting.ExecutePowerShell(context, Scripting.Position.BeforeMain);

            if (!_scripting.ExecutePowerShell(context, Scripting.Position.InsteadOfMain)) {
                DeleteMain(context);
            }

            _scripting.ExecutePowerShell(context, Scripting.Position.AfterMain);
        }

        public void DeleteMain(DeleteOpContext context) {
            Stopwatch stopWatch = new Stopwatch();
            stopWatch.Start();

            GetHandler(context).Delete(context);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, 
                "Exchange.Delete method exiting, took {0} ms", stopWatch.ElapsedMilliseconds);
        }

        #endregion

        #region ExecuteQuery
        /// <summary>
        /// Implementation of SearchOp.ExecuteQuery
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="query">Query to execute</param>
        /// <param name="handler">Results handler</param>
        /// <param name="options">Operation options</param>
        public void ExecuteQuery( ObjectClass oclass, string query, ResultsHandler handler, OperationOptions options) {
            const string operation = "ExecuteQuery";

            ExchangeUtility.NullCheck(oclass, "oclass", this._configuration);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, 
                "Exchange.ExecuteQuery method; oclass = {0}, query = {1}", oclass, query);

            ExecuteQueryContext context = new ExecuteQueryContext() {
                Connector = this,
                ConnectorConfiguration = _configuration,
                ObjectClass = oclass,
                OperationName = operation,
                Options = options,
                Query = query,
                ResultsHandler = handler
            };

            _scripting.ExecutePowerShell(context, Scripting.Position.BeforeMain);
            if (!_scripting.ExecutePowerShell(context, Scripting.Position.InsteadOfMain)) {
                ExecuteQueryMain(context);
            }
            _scripting.ExecutePowerShell(context, Scripting.Position.AfterMain);

            // TODO what about executing a script on each returned item?
        }

        public void ExecuteQueryMain(ExecuteQueryContext context) {
            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Exchange.ExecuteQueryMain starting");
            Stopwatch stopWatch = new Stopwatch();
            stopWatch.Start();

            GetHandler(context).ExecuteQuery(context);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, 
                "Exchange.ExecuteQuery method exiting, took {0} ms", stopWatch.ElapsedMilliseconds);
        }
        #endregion

        #region SyncOp Members

        public SyncToken GetLatestSyncToken(ObjectClass objectClass) {
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
                ObjectClass oclass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {
            const string operation = "Sync";

            ExchangeUtility.NullCheck(oclass, "oclass", this._configuration);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, 
                "Exchange.Sync method; oclass = {0}, token = {1}", oclass, token);

            SyncOpContext context = new SyncOpContext() {
                Connector = this,
                ConnectorConfiguration = _configuration,
                ObjectClass = oclass,
                OperationName = operation,
                Options = options,
                SyncToken = token,
                SyncResultsHandler = handler
            };

            _scripting.ExecutePowerShell(context, Scripting.Position.BeforeMain);
            if (!_scripting.ExecutePowerShell(context, Scripting.Position.InsteadOfMain)) {
                SyncMain(context);
            }
            _scripting.ExecutePowerShell(context, Scripting.Position.AfterMain);

            // TODO what about executing a script on each returned item?
        }

        public void SyncMain(SyncOpContext context) {
            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Exchange.Sync starting");
            Stopwatch stopWatch = new Stopwatch();
            stopWatch.Start();

            GetHandler(context).Sync(context);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Exchange.Sync method exiting, took {0} ms", stopWatch.ElapsedMilliseconds);
        }

        #endregion

        #region SchemaOp Implementation

        /// <summary>
        /// Implementation of SchemaSpiOp
        /// </summary>
        /// <returns></returns>
        public Schema Schema()
        {
            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Exchange.Schema method");
            if (_schema != null) {
                LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Returning cached schema");
            } else {
                _schema = SchemaUtils.BuildSchema(this,
                                GetSupportedObjectClasses,
                                GetObjectClassInfo,
                                GetSupportedOperations,
                                GetUnSupportedOperations);
                LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Returning newly created schema");
            }
            return _schema;
        }

        internal Schema GetSchema() {
            return _schema;
        }

        /// <summary>
        /// Gets the object class info for specified object class, used for schema building
        /// </summary>
        /// <param name="oc">ObjectClass to get info for</param>
        /// <returns>ObjectClass' ObjectClassInfo</returns>
        internal ObjectClassInfo GetObjectClassInfo(ObjectClass oc) {
            //return GetHandler(oc.GetObjectClassValue()).GetObjectClassInfo(this, oc);
            return GetDeclaredObjectClassInfos()[oc];
        }

        /// <summary>
        /// Defines the supported object classes by the connector, used for schema building
        /// </summary>
        /// <returns>List of supported object classes</returns>
        public ICollection<ObjectClass> GetSupportedObjectClasses() {
            return GetDeclaredObjectClassInfos().Keys;
        }

        /// <summary>
        /// Gets the object class info for specified object class, used for schema building
        /// </summary>
        /// <param name="oc">ObjectClass to get info for</param>
        /// <returns>ObjectClass' ObjectClassInfo</returns>
        public ObjectClassInfo GetObjectClassInfoGeneric(ObjectClass oc) {
            return GetDeclaredObjectClassInfos()[oc];
        }

        // copied from AD and modified
        private IDictionary<ObjectClass, ObjectClassInfo> GetDeclaredObjectClassInfos() {
            if (_objectClassInfos == null) {
                var infos = new List<IDictionary<ObjectClass, ObjectClassInfo>>();

                if (_configuration.ObjectClassesReplacementFile != null) {
                    infos.Add(CommonUtils.GetOCInfoFromFile(_configuration.ObjectClassesReplacementFile));
                } else {
                    infos.Add(CommonUtils.GetOCInfoFromExecutingAssembly("Org.IdentityConnectors.ActiveDirectory.ObjectClasses.xml"));
                    infos.Add(CommonUtils.GetOCInfoFromAssembly("Org.IdentityConnectors.Exchange.ObjectClasses.xml", Assembly.GetExecutingAssembly()));
                }
                if (_configuration.ObjectClassesExtensionFile != null) {
                    infos.Add(CommonUtils.GetOCInfoFromFile(_configuration.ObjectClassesExtensionFile));
                }
                _objectClassInfos = CommonUtils.MergeOCInfo(infos);
            }
            return _objectClassInfos;
        }

        /// <summary>
        /// Gets the list of supported operations by the object class, used for schema building
        /// </summary>
        /// <param name="oc"></param>
        /// <returns></returns>
        public IList<SafeType<SPIOperation>> GetSupportedOperations(ObjectClass oc) {
            return null;
        }

        /// <summary>
        /// Gets the list of UNsupported operations by the object class, used for schema building
        /// </summary>
        /// <param name="oc"></param>
        /// <returns></returns>
        public IList<SafeType<SPIOperation>> GetUnSupportedOperations(ObjectClass oc) {
            return _activeDirectoryConnector.GetUnSupportedOperations(oc);
        }

        #endregion

        #region Attribute normalization

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
            if (attribute.Is(ExchangeConnectorAttributes.AttExternalEmailAddress) || attribute.Is(ExchangeConnectorAttributes.AttForwardingSmtpAddress)) {
                return NormalizeSmtpAddressAttribute(attribute);
            } else {
                return attribute;
            }

            // TODO: what with EmailAddresses? (we should not remove SMTP/smpt prefix, because it carries information on primary/secondary address type)
            // TODO: and other attributes?
        }

        private ConnectorAttribute NormalizeSmtpAddressAttribute(ConnectorAttribute attribute) {
            if (attribute.Value == null) {
                return attribute;
            }

            IList<object> normValues = new List<object>();
            bool normalized = false;
            foreach (object val in attribute.Value) {
                string strVal = val as string;
                if (strVal != null) {
                    string[] split = strVal.Split(':');
                    if (split.Length == 2) {
                        // it contains delimiter, use the second part
                        normValues.Add(split[1]);
                        normalized = true;
                    } else {
                        // put the original value
                        normValues.Add(val);
                    }
                }
            }

            if (normalized) {
                // build the attribute again
                return ConnectorAttributeBuilder.Build(attribute.Name, normValues);
            } else {
                return attribute;
            }
        }
        #endregion

        #region Filter translation

        // TODO Exchange-specific attributes
        
        /// <summary>
        /// Implementation of SearchOp.CreateFilterTranslator
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="options">Operation options</param>
        /// <returns>Filter translator</returns>
        public FilterTranslator<string> CreateFilterTranslator(ObjectClass oclass, OperationOptions options) {
            return _handlers[oclass.GetObjectClassValue()].CreateFilterTranslator(this, oclass, options);
        }

        #endregion

        // ====================================== HERE ARE OBJECT-CLASS-INDEPENDENT CONNECTOR PARTS =====================================

        #region Connector Members
        /// <summary>
        /// Inits the connector with configuration injected
        /// </summary>
        /// <param name="configuration">Connector configuration</param>
        public void Init(Configuration configuration) {
            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "ExchangeConnector.Init: entry");

            _configuration = (ExchangeConfiguration)configuration;
            _activeDirectoryConnector.Init(configuration);
            _schema = null;
            _objectClassInfos = null;
            Schema();
            _powershell = new ExchangePowerShellSupport(_configuration.ExchangeVersion, _configuration.ExchangeUri, 
                 _configuration.ConnectorMessages);
            _scripting = new Scripting(_configuration.ScriptingConfigurationFile, _powershell);

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "ExchangeConnector.Init: exit");
        }
        #endregion

        #region IDisposable Members
        /// <summary>
        /// Dispose resources, <see cref="IDisposable"/>
        /// </summary>
        public void Dispose() {
            this.Dispose(true);
            GC.SuppressFinalize(this);
        }

        /// <summary>
        /// Dispose the resources we use
        /// </summary>
        /// <param name="disposing">true if called from <see cref="PSExchangeConnector.Dispose()"/> (?????)</param>
        protected virtual void Dispose(bool disposing)
        {
            if (disposing) {
                // free managed resources
                if (this._powershell != null) {
                    this._powershell.Dispose();
                    this._powershell = null;
                }
            }
        }
        #endregion

        #region TestOp Members

        public void Test() {
            const string operation = "Test";

            LOGGER_API.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Exchange.Test method");

            Context context = new Context() {
                Connector = this,
                ConnectorConfiguration = _configuration,
                OperationName = operation
            };

            _scripting.ExecutePowerShell(context, Scripting.Position.BeforeMain);
            if (!_scripting.ExecutePowerShell(context, Scripting.Position.InsteadOfMain)) {
                TestMain();
            }
            _scripting.ExecutePowerShell(context, Scripting.Position.AfterMain);
        }

        /// <summary>
        /// Tests if the connector is properly configured and ready
        /// </summary>
        public void TestMain()
        {
            // validate the configuration first, this will check AD configuration too
            _configuration.Validate();

            // AD validation (includes configuration validation too)
            _activeDirectoryConnector.Test();

            // runspace check (disabled as it does nothing)
            //_powershell.Test();
        }

        #endregion

        #region ScriptOnResourceOp Members

        public object RunScriptOnResource(ScriptContext request, OperationOptions options)
        {
            return _activeDirectoryConnector.RunScriptOnResource(request, options);
        }

        #endregion

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


    }
}
