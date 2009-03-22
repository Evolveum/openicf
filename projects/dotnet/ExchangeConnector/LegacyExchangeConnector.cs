// <copyright file="LegacyExchangeConnector.cs" company="Sun Microsystems, Inc.">
// ====================
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// 
// Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
    public class LegacyExchangeConnector : ActiveDirectoryConnector
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
        private static readonly string ClassName = typeof(LegacyExchangeConnector).ToString();

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
            const string METHOD = "Create";
            Debug.WriteLine(METHOD + ":entry", ClassName);

            // get recipient type
            string rcptType = ExchangeUtility.GetAttValue(AttRecipientType, attributes) as string;

            if (rcptType != RcptTypeMailBox && rcptType != RcptTypeMailUser)
            {
                // AD account only, we do nothing
                return base.Create(oclass, attributes, options);
            }

            // first create the object in AD
            Uid uid = base.Create(oclass, FilterOut(attributes), options);

            // prepare the command
            ExchangeConnector.CommandInfo cmdInfo = rcptType == RcptTypeMailBox
                                          ? ExchangeConnector.CommandInfo.EnableMailbox
                                          : ExchangeConnector.CommandInfo.EnableMailUser;
            Command cmd = ExchangeUtility.GetCommand(cmdInfo, attributes);

            try
            {
                this.InvokePipeline(cmd);
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

            // update in AD first
            Uid uid = base.Update(type, oclass, FilterOut(attributes), options);

            // get recipient type and database
            string rcptType = ExchangeUtility.GetAttValue(AttRecipientType, attributes) as string;
            string database = ExchangeUtility.GetAttValue(AttDatabase, attributes) as string;

            if (rcptType == RcptTypeMailUser)
            {
                if (type == UpdateType.REPLACE)
                {
                    // get name attribute
                    attributes = this.EnsureName(oclass, attributes, uid);

                    PSObject psuser = this.GetUser(ExchangeConnector.CommandInfo.GetUser, attributes);
                    string origRcptType = psuser.Members[AttRecipientType].Value.ToString();
                    Command cmd = origRcptType != rcptType ?
                        ExchangeUtility.GetCommand(ExchangeConnector.CommandInfo.EnableMailUser, attributes)
                        : ExchangeUtility.GetCommand(ExchangeConnector.CommandInfo.SetMailUser, attributes);
                    this.InvokePipeline(cmd);
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
                ConnectorObject aduser = this.ADSearchByUid(uid, oclass, ExchangeUtility.AddAttributeToOptions(options, AttDatabaseADName));
                attributes.Add(aduser.Name);
                ExchangeConnector.CommandInfo cmdInfo = ExchangeConnector.CommandInfo.GetUser;
                if (aduser.GetAttributeByName(AttDatabaseADName) != null)
                {
                    // we can be sure it is user mailbox type
                    cmdInfo = ExchangeConnector.CommandInfo.GetMailbox;
                }

                PSObject psuser = this.GetUser(cmdInfo, attributes);
                string origRcptType = psuser.Members[AttRecipientType].Value.ToString();
                string origDatabase = psuser.Members[AttDatabase] != null ? psuser.Members[AttDatabase].Value.ToString() : null;
                if (origRcptType != rcptType)
                {
                    Command cmd2 = ExchangeUtility.GetCommand(ExchangeConnector.CommandInfo.EnableMailbox, attributes);
                    this.InvokePipeline(cmd2);
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
            ArrayList attsToGet = null;
            if (options != null && options.AttributesToGet != null)
            {
                attsToGet = new ArrayList(options.AttributesToGet);
            }

            // delegate to get the exchange attributes if requested            
            SyncResultsHandler xchangeHandler = delegate(SyncDelta delta)
            {
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
            ArrayList attsToGet = null;
            if (options != null && options.AttributesToGet != null)
            {
                attsToGet = new ArrayList(options.AttributesToGet);
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
                    builder.AttributesToGet = (string[])newAttsToGet.ToArray(typeof(string));
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
        /// helper method to filter out all attributes used in LegacyExchangeConnector only
        /// </summary>
        /// <param name="attributes">Connector attributes</param>
        /// <returns>Filtered connector attributes</returns>
        private static ICollection<ConnectorAttribute> FilterOut(ICollection<ConnectorAttribute> attributes)
        {
            return ExchangeUtility.FilterOut(attributes, AttRecipientType, AttDatabase, AttExternalMail);
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
        private ConnectorObject AddExchangeAttributes(ObjectClass oc, ConnectorObject cobject, IList attToGet)
        {            
            ExchangeUtility.NullCheck(oc, "name", this.configuration);
            ExchangeUtility.NullCheck(oc, "cobject", this.configuration);

            // we support ACCOUNT only and if the recipient type and database is in att to get
            if (!oc.Is(ObjectClass.ACCOUNT_NAME) ||
                (!attToGet.Contains(AttRecipientType) && !attToGet.Contains(AttDatabase)))
            {
                return cobject;
            }

            bool getDatabase = false;
            ExchangeConnector.CommandInfo cmdInfo = ExchangeConnector.CommandInfo.GetUser;
            if (cobject.GetAttributeByName(AttDatabase) != null || cobject.GetAttributeByName(AttDatabaseADName) != null)
            {
                // we need to get database attribute, it is mailbox for sure
                getDatabase = true;
                cmdInfo = ExchangeConnector.CommandInfo.GetMailbox;
            }
            
            ConnectorObject retCObject = cobject;
            
            // prepare the connector attribute list to get the command
            ICollection<ConnectorAttribute> attributes = new Collection<ConnectorAttribute> { cobject.Name };

            // get the command
            Command cmd = ExchangeUtility.GetCommand(cmdInfo, attributes);

            ICollection<PSObject> foundObjects = this.InvokePipeline(cmd);

            // it has to be only one or zero objects in this case
            if (foundObjects != null && foundObjects.Count == 1)
            {
                string rcptName = RcptTypeUser;
                string database = null;
                foreach (PSObject obj in foundObjects)
                {
                    rcptName = obj.Members[AttRecipientType].Value.ToString();
                    database = obj.Members[AttDatabase] != null ? obj.Members[AttDatabase].Value.ToString() : null;
                    break;
                }

                ConnectorObjectBuilder cobjBuilder = new ConnectorObjectBuilder();
                if (getDatabase)
                {
                    foreach (ConnectorAttribute attribute in cobject.GetAttributes())
                    {
                        if ((attribute.Is(AttDatabase) || attribute.Is(AttDatabaseADName)) && database != null)
                        {
                            cobjBuilder.AddAttribute(ConnectorAttributeBuilder.Build(AttDatabase, database));
                        }
                        else
                        {
                            cobjBuilder.AddAttribute(attribute);
                        }
                    }
                }
                else
                {
                    cobjBuilder.AddAttributes(cobject.GetAttributes());
                }

                cobjBuilder.AddAttribute(ConnectorAttributeBuilder.Build(AttRecipientType, rcptName));
                retCObject = cobjBuilder.Build();
            }

            return retCObject;
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
        private PSObject GetUser(ExchangeConnector.CommandInfo cmdInfo, ICollection<ConnectorAttribute> attributes)
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
        /// Ensures we have Name attribute in attributes collection, if not present, it uses AD search to get it
        /// </summary>
        /// <param name="oclass">object class</param>
        /// <param name="attributes">Collection of attributes</param>
        /// <param name="uid">object Uid</param>
        /// <returns>Collection of attributes conta</returns>
        private ICollection<ConnectorAttribute> EnsureName(ObjectClass oclass, ICollection<ConnectorAttribute> attributes, Uid uid)
        {
            string name = ExchangeUtility.GetAttValue(Name.NAME, attributes) as string;
            if (name == null)
            {
                // we don't know name, but we need it - NOTE: searching for all the default attributes, we need only Name here, it can be improved
                ConnectorObject co = this.ADSearchByUid(uid, oclass, null);
                ExchangeUtility.NullCheck(co, "co", this.configuration);

                // add to attributes
                attributes.Add(co.Name);
            }
            return attributes;
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

                return base.GetLdapNamesForAttribute(attr);
            }
        }
    }
}
