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
    using System.Diagnostics;
    using System.Globalization;
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
        internal static readonly string[,] AttMapping = new[,] 
        {
        { AttDatabase, AttDatabaseADName },
        { AttExternalMail, AttExternalMailADName }
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
                        ConnectorAttributeInfo.Flags.REQUIRED | ConnectorAttributeInfo.Flags.NOT_UPDATEABLE | ConnectorAttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT);

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
        private const string RcptTypeMailBox = "mailbox";

        /// <summary>
        /// Recipient type attribute for MailUser
        /// </summary>
        private const string RcptTypeMailUser = "mailuser";

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
                // execute the command
                this.runspace.InvokePipeline(cmd);
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

            Assertions.NullCheck(type, "updatetype");
            Assertions.NullCheck(oclass, "oclass");
            Assertions.NullCheck(attributes, "attributes");

            // update in AD first
            Uid uid = base.Update(type, oclass, FilterOut(attributes), options);

            // get recipient type
            string rcptType = ExchangeUtility.GetAttValue(AttRecipientType, attributes) as string;

            // update is possible for mailuser's external email only
            if (rcptType == RcptTypeMailUser)
            {
                if (type == UpdateType.REPLACE)
                {
                    // get name attribute
                    string name = ExchangeUtility.GetAttValue(Name.NAME, attributes) as string;
                    if (name == null)
                    {
                        // we don't know name, but we need it - NOTE: searching for all the default attributes, we need only Name here, it can be improved
                        ConnectorObject co = this.ADSearchByUid(uid, oclass, null);
                        Assertions.NullCheck(co, "co");

                        // add to attributes
                        attributes.Add(co.Name);
                    }

                    Command cmd = ExchangeUtility.GetCommand(ExchangeConnector.CommandInfo.SetMailUser, attributes);
                    this.runspace.InvokePipeline(cmd);
                }
                else
                {
                    throw new ConnectorException(string.Format(CultureInfo.CurrentCulture, "Update type [{0}] not supported", type));
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
            // TODO: implement Sync
            base.Sync(objClass, token, handler, options);
        }

        /// <summary>
        /// Implementation of SynOp.GetLatestSyncToken
        /// </summary>
        /// <param name="objectClass">Object class</param>
        /// <returns>Last sync token</returns>
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
                                                cobject, attsToGet, AttMapping);
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
                    var newAttsToGet = ExchangeUtility.FilterReplace(attsToGet, AttMapping);

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
            if (oc == ObjectClass.ACCOUNT)
            {
                var classInfoBuilder = new ObjectClassInfoBuilder { IsContainer = oinfo.IsContainer, ObjectType = oinfo.ObjectType };
                classInfoBuilder.AddAllAttributeInfo(oinfo.ConnectorAttributeInfos);
                classInfoBuilder.AddAttributeInfo(AttInfoDatabase);
                classInfoBuilder.AddAttributeInfo(AttInfoRecipientType);
                classInfoBuilder.AddAttributeInfo(AttInfoExternalMail);
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
        /// helper method for searching object in AD by UID
        /// </summary>
        /// <param name="uid">Uid of the searched </param>
        /// <param name="oclass">Object class</param>
        /// <param name="options">Operation options</param>
        /// <returns>Connector object found by the Uid</returns>
        private ConnectorObject ADSearchByUid(Uid uid, ObjectClass oclass, OperationOptions options)
        {
            Assertions.NullCheck(uid, "uid");
            Assertions.NullCheck(oclass, "oclass");
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
